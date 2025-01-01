import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class PomodoroTimer {
    private JFrame frame;
    private JLabel timerLabel, cyclesLabel;
    private JButton startButton, pauseButton, cancelButton;
    private JTextField minutesInput, breakInput;
    private Timer timer;
    private int remainingTime;
    private boolean isRunning = false;
    private int completedCycles = 0;
    private final File cycleFile = new File("num.txt");
    private boolean isBreak = false;
    private JButton toggleEditableButton;

    public PomodoroTimer() {
        createUI();
    }

    private void createUI() {
        frame = new JFrame("pomotime");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLayout(new GridLayout(6, 2));
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage("dav.ico"));

        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("settings");
        JMenuItem settingsItem = new JMenuItem("open settings");
        JMenuItem exitItem = new JMenuItem("exit app");
        settingsMenu.add(settingsItem);
        settingsMenu.add(exitItem);
        menuBar.add(settingsMenu);
        frame.setJMenuBar(menuBar);

        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettingsMenu();
            }
        });

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                exitApplication();
            }
        });

        timerLabel = new JLabel("00:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 24));

        cyclesLabel = new JLabel(loadCompletedCycles(), SwingConstants.CENTER);
        cyclesLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        minutesInput = new JTextField("25");
        breakInput = new JTextField("5");
        breakInput.setEditable(false);

        minutesInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateBreakTime();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateBreakTime();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateBreakTime();
            }
        });

        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        cancelButton = new JButton("Cancel");

        startButton.addActionListener(new StartAction());
        pauseButton.addActionListener(new PauseAction());
        cancelButton.addActionListener(new CancelAction());

        frame.add(new JLabel("study mins:", SwingConstants.RIGHT));
        frame.add(minutesInput);
        frame.add(new JLabel("break mins:", SwingConstants.RIGHT));
        frame.add(breakInput);
        frame.add(timerLabel);
        frame.add(startButton);
        frame.add(pauseButton);
        frame.add(cancelButton);
        frame.add(cyclesLabel);

        frame.setVisible(true);
    }

    private void openSettingsMenu() {
        JFrame settingsFrame = new JFrame("settings menu");
        settingsFrame.setSize(300, 200);
        settingsFrame.setLayout(new GridLayout(1, 2));

        JLabel customBreaksLabel = new JLabel("custom breaks:");
        toggleEditableButton = new JButton("No");
        toggleEditableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isEditable = breakInput.isEditable();
                breakInput.setEditable(!isEditable);
                toggleEditableButton.setText(isEditable ? "No" : "Yes");
            }
        });

        settingsFrame.add(customBreaksLabel);
        settingsFrame.add(toggleEditableButton);
        settingsFrame.setVisible(true);
    }

    private void exitApplication() {
        int confirm = JOptionPane.showConfirmDialog(frame, 
            "do you want to exit", 
            "exit box", 
            JOptionPane.YES_NO_OPTION);
    
        if (confirm == JOptionPane.YES_OPTION) {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            saveCompletedCycles();
            frame.dispose();
            System.exit(0);
        }
    }

    private void updateBreakTime() {
        try {
            int studyMinutes = Integer.parseInt(minutesInput.getText());
            int breakMinutes = studyMinutes / 5;
            breakInput.setText(String.valueOf(breakMinutes));
        } catch (NumberFormatException e) {
            breakInput.setText("0");
        }
    }

    private String loadCompletedCycles() {
        if (!cycleFile.exists()) {
            completedCycles = 0;
            return "sessions: 0";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(cycleFile))) {
            String line = reader.readLine();
            if (line != null) {
                completedCycles = Integer.parseInt(line);
                return "sessions: " + completedCycles;
            }
        } catch (IOException | NumberFormatException e) {
            completedCycles = 0;
            return "sessions: 0";
        }
        return "sessions: 0";
    }

    private void saveCompletedCycles() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cycleFile, false))) {
            writer.write(String.valueOf(completedCycles));
        } catch (IOException e) {
            System.err.println("error saving cycles " + e.getMessage());
        }
    }

    private void updateCyclesLabel() {
        cyclesLabel.setText("sessions: " + completedCycles);
        frame.revalidate();
        frame.repaint();
    }

    private void startTimer() {
        try {
            int minutes = Integer.parseInt(minutesInput.getText());
            int totalMilliseconds = minutes * 60 * 1000;
            int studyMilliseconds = totalMilliseconds;
            int breakMilliseconds = (totalMilliseconds / 5);
            remainingTime = isBreak ? breakMilliseconds : studyMilliseconds;
        } catch (NumberFormatException e) {
            System.err.println("time input not valid: " + e.getMessage());
            return;
        }

        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (remainingTime > 0) {
                    remainingTime -= 1000;
                    updateTimerLabel();
                } else {
                    timer.stop();
                    isRunning = false;
                    if (isBreak) {
                        completedCycles++;
                        saveCompletedCycles();
                        updateCyclesLabel();
                        Toolkit.getDefaultToolkit().beep();
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                    }
                    isBreak = !isBreak;
                    updateWindowTitle();
                    startTimer();
                }
            }
        });

        isRunning = true;
        updateWindowTitle();
        updateTimerLabel();
        timer.start();
    }

    private void updateTimerLabel() {
        int minutes = remainingTime / 60000;
        int seconds = (remainingTime % 60000) / 1000;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void updateWindowTitle() {
        if (isRunning) {
            frame.setTitle(isBreak ? "on break" : "studying");
        } else {
            frame.setTitle("paused");
        }
    }

    private void stopTimer() {
        if (timer != null && isRunning) {
            timer.stop();
            isRunning = false;
            updateWindowTitle();
        }
    }

    private void pauseTimer() {
        if (timer != null && isRunning) {
            timer.stop();
            isRunning = false;
            updateWindowTitle();
            pauseButton.setText("Resume");
        } else if (timer != null && !isRunning) {
            timer.start();
            isRunning = true;
            updateWindowTitle();
        }
    }

    private void cancelTimer() {
        stopTimer();
        remainingTime = 0;
        isBreak = false;
        updateTimerLabel();
        updateWindowTitle();
    }

    private class StartAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isRunning) {
                startTimer();
            }
        }
    }

    private class PauseAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            pauseTimer();
        }
    }

    private class CancelAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            cancelTimer();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PomodoroTimer::new);
    }
}
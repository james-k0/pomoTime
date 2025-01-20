import javax.sound.sampled.*;
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
    private long remainingTime;
    private boolean isRunning = false;
    private int completedCycles = 0;
    private final File cycleFile = new File("num.txt");
    private boolean isBreak = false;
    private JButton toggleEditableButton;
    private boolean autoStartNextSessionEnabled = false;
    private boolean isBeepEnabled = true;
    private final Image icon = new ImageIcon(PomodoroTimer.class.getResource("/dav.png")).getImage();
    private File customSoundFile;

    public PomodoroTimer() {
        createUI();
    }

    private void createUI() {
        frame = new JFrame("get workin");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(5, 2));
        frame.setIconImage(icon);

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
            public void actionPerformed(ActionEvent e) {
                exitApplication();
            }
        });

        timerLabel = new JLabel("00:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 24));

        cyclesLabel = new JLabel(loadCompletedCycles(), SwingConstants.CENTER);
        cyclesLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        minutesInput = new JTextField("50");
        breakInput = new JTextField("10");
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
        pauseButton.setEnabled(false);
        cancelButton.setEnabled(false);

        startButton.addActionListener(new StartAction());
        pauseButton.addActionListener(new PauseAction());
        cancelButton.addActionListener(new CancelAction());

        frame.add(new JLabel("study mins:", SwingConstants.CENTER));
        frame.add(minutesInput);
        frame.add(new JLabel("break mins:", SwingConstants.CENTER));
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
        settingsFrame.setLayout(new GridLayout(5, 2));
        settingsFrame.setIconImage(icon);

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

        JLabel autoStartNextSessionLabel = new JLabel("auto-start next session:");
        JButton toggleAutoStartNextSessionButton = new JButton(autoStartNextSessionEnabled ? "Yes" : "No");
        toggleAutoStartNextSessionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoStartNextSessionEnabled = !autoStartNextSessionEnabled;
                toggleAutoStartNextSessionButton.setText(autoStartNextSessionEnabled ? "Yes" : "No");
            }
        });

        JLabel beepLabel = new JLabel("enable beep sound:");
        JButton toggleBeepButton = new JButton(isBeepEnabled ? "Yes" : "No");
        toggleBeepButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isBeepEnabled = !isBeepEnabled;
                toggleBeepButton.setText(isBeepEnabled ? "Yes" : "No");
            }
        });

        JButton incrementSessionButton = new JButton("+1 session");
        incrementSessionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                completedCycles++;
                saveCompletedCycles();
                updateCyclesLabel();
            }
        });

        JButton decrementSessionButton = new JButton("-1 session");
        decrementSessionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (completedCycles > 0) {
                    completedCycles--;
                    saveCompletedCycles();
                    updateCyclesLabel();
                }
            }
        });

        JLabel customSoundLabel = new JLabel("custom beep:");
        JButton chooseSoundButton = new JButton("Choose .Wav");
        chooseSoundButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    customSoundFile = fileChooser.getSelectedFile();
                    saveCompletedCycles();
                }
            }
        });

        settingsFrame.add(customBreaksLabel);
        settingsFrame.add(toggleEditableButton);
        settingsFrame.add(autoStartNextSessionLabel);
        settingsFrame.add(toggleAutoStartNextSessionButton);
        settingsFrame.add(beepLabel);
        settingsFrame.add(toggleBeepButton);
        settingsFrame.add(decrementSessionButton);
        settingsFrame.add(incrementSessionButton);
        settingsFrame.add(customSoundLabel);
        settingsFrame.add(chooseSoundButton);

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
                String soundFilePath = reader.readLine();
                if (soundFilePath != null && !soundFilePath.isEmpty()) {
                    customSoundFile = new File(soundFilePath);
                }
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
            writer.newLine();
            if (customSoundFile != null) {
                writer.write(customSoundFile.getAbsolutePath());
            }
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
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        cancelButton.setEnabled(true);
        if (remainingTime <= 0) {
            remainingTime = isBreak
                    ? Integer.parseInt(breakInput.getText()) * 60 * 1000L
                    : Integer.parseInt(minutesInput.getText()) * 60 * 1000L;
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + remainingTime;

        timer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long currentTime = System.currentTimeMillis();
                remainingTime = endTime - currentTime;

                if (remainingTime <= 0) {
                    timer.stop();
                    isRunning = false;

                    if (isBreak) {
                        completedCycles++;
                        saveCompletedCycles();
                        updateCyclesLabel();
                        isBreak = false;
                        if (isBeepEnabled) playSound();
                        updateWindowTitle();
                        remainingTime = 0;
                        if (autoStartNextSessionEnabled) {
                            startTimer();
                        } else {
                            startButton.setEnabled(true);
                            pauseButton.setEnabled(false);
                            cancelButton.setEnabled(false);
                        }
                    } else {
                        isBreak = true;
                        if (isBeepEnabled) playSound();
                        updateWindowTitle();
                        remainingTime = 0;
                        startTimer();
                    }
                } else {
                    updateTimerLabel();
                }
            }
        });

        isRunning = true;
        updateWindowTitle();
        updateTimerLabel();
        timer.start();
    }

    private void updateTimerLabel() {
        int minutes = (int) (remainingTime / 60000);
        int seconds = (int) ((remainingTime % 60000) / 1000);
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void updateWindowTitle() {
        if (isRunning) {
            frame.setTitle(isBreak ? "on break" : "studying");
        } else {
            frame.setTitle("not workin");
        }
    }

    private void pauseTimer() {
        if (timer != null && isRunning) {
            timer.stop();
            isRunning = false;
            updateWindowTitle();
            pauseButton.setText("Resume");
        } else if (timer != null && !isRunning) {
            startTimer();
            isRunning = true;
            pauseButton.setText("Pause");
        }
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
            isRunning = false;
            remainingTime = 0;
            updateWindowTitle();
            updateTimerLabel();
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            cancelButton.setEnabled(false);
        }
    }

    private void playSound() {
        if (customSoundFile != null && customSoundFile.exists()) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(customSoundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            } catch (Exception e) {
                e.printStackTrace();
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PomodoroTimer::new);
    }

    private class StartAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            startTimer();
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
}

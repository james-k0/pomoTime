@echo off
setlocal

set MAIN_CLASS=PomodoroTimer
set JAR_NAME=pomoTime.jar
set BIN_DIR=bin

if not exist "%BIN_DIR%" (
    mkdir "%BIN_DIR%"
)

javac -d "%BIN_DIR%" "%MAIN_CLASS%.java"

if errorlevel 1 (
    echo javac failed
    exit /b 1
)

cd "%BIN_DIR%"
jar cfe "%JAR_NAME%" %MAIN_CLASS% *.class

move "%JAR_NAME%" ..

cd ..
rmdir /s /q "%BIN_DIR%"

echo JAR out: %JAR_NAME%
endlocal

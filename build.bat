@echo off
setlocal

set MAIN_CLASS=PomodoroTimer
set JAR_NAME=pomoTime.jar
echo jar: %JAR_NAME%
set BIN_DIR=bin
set ICON_FILE=dav.ico
echo ico: %ICON_FILE%
set OUTPUT_EXE=pomoTime.exe
echo out: %OUTPUT_EXE%
set LAUNCH4J_PATH="C:\Program Files (x86)\Launch4j\launch4jc.exe"
set XML_FILE="launch4j_build.xml"

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

%LAUNCH4J_PATH% %XML_FILE%

echo EXE out: %OUTPUT_EXE%

endlocal

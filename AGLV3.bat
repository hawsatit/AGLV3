@echo off
:: Run from the folder this file is in, wherever the project is copied
cd /d "%~dp0"

:: Create output directory if it doesn't exist
if not exist target\classes mkdir target\classes

echo Compiling...
javac -d target\classes src\main\java\edu\grinnell\psychMAP26\AGLV3\*.java
if errorlevel 1 (
    echo.
    echo Compile failed. Is Java ^(JDK^) installed and on the PATH?
    pause
    exit /b 1
)

echo Running...
java -cp target\classes edu.grinnell.psychMAP26.AGLV3.AGLV3

pause

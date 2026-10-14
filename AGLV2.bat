@echo off
cd /d H:\SpringMAP26\AGLV2-main

:: Create output directory if it doesn't exist
if not exist target\classes mkdir target\classes

echo Compiling...
javac -d target\classes src\main\java\edu\grinnell\psychMAP26\AGLV2\*.java

echo Running...
java -cp target\classes edu.grinnell.psychMAP26.AGLV2.AGLV2

pause


fix it for this location

C:\Users\psyclab8\Desktop\SpringMAP2026Desktop\AGLV2
@ECHO off
find "RemoveDesire" decompiled\*.java > result.txt 
if %errorlevel%==0 echo file contains
pause.
@ECHO off
set FIRST=NODE_ARRIVED
set SECOND=state

findstr /M "%FIRST%" decompiled\*.txt > pre_result.txt 
echo "%FIRST% + %SECOND%" > result.txt
for /f "delims=" %%x in (pre_result.txt) do find "%SECOND%" %%x >> result.txt
if %errorlevel%==0 echo file contains both words
pause.
@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark

title BLUECOVE JSR82 EMULATION device 2

set CP=%BLUECOVE_JAR%
set CP=%CP%;%BLUECOVE_EMU_JAR%
set CP=%CP%;%BLUECOVE_TESTER_JAR%

java -Dbluecove.stack=emulator -Dbluecove.deviceID=2 -cp %CP% %BLUECOVE_MAIN%
if errorlevel 2 (
    echo Error calling java
    pause
)
pause java ends.
:endmark
ENDLOCAL
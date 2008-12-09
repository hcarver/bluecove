@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark

title BLUECOVE JSR82 EMULATION SERVER


set CP=%BLUECOVE_JAR%
set CP=%CP%;%BLUECOVE_EMU_JAR%

java -cp %CP% com.intel.bluetooth.emu.EmuServer
if errorlevel 2 (
    echo Error calling java
    pause
)
pause java ends.
:endmark
ENDLOCAL
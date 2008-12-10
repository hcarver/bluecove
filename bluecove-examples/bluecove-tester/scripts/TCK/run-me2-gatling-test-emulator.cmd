@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call "%~dp0tck-environment.cmd" %*
if errorlevel 1 (
    echo Error calling tck-environment.cmd
    endlocal
    pause
    exit /b 1
)

SET STACK=emulator
title %STACK%-BluetoothTCK

set CP=%MICROEMULATOR_HOME%\microemulator.jar
set CP=%CP%;%BLUECOVE_JAR%
set CP=%CP%;%BLUECOVE_EMU_JAR%


java -Dbluecove.stack=%STACK% -cp "%CP%" org.microemu.app.Main -Xautotest:http://%BLUECOVE_TCK_HOST%:%BLUECOVE_TCK_PORT%/getNextApp.jad >  run-%STACK%.cmd.log

if errorlevel 1 goto errormark
echo [Launched OK]
goto endmark
:errormark
	ENDLOCAL
	echo Error in run
:endmark
pause
ENDLOCAL

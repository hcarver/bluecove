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

SET STACK=widcomm
title %STACK%-BluetoothTCK

set CP=%MICROEMULATOR_JAR%;%BLUECOVE_JAR%

java -Dbluecove.stack=%STACK% -cp "%CP%" %MICROEMULATOR_MAIN% %MICROEMULATOR_ARGS% -Xautotest:http://%BLUECOVE_TCK_HOST%:%BLUECOVE_TCK_PORT%/getNextApp.jad >  run-%STACK%.cmd.log

if errorlevel 1 goto errormark
echo [Launched OK]
goto endmark
:errormark
	ENDLOCAL
	echo Error in start
	pause
:endmark
ENDLOCAL

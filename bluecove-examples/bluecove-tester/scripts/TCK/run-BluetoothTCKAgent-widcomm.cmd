@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call %~dp0tck-environment.cmd %*
if errorlevel 1 (
    echo Error calling tck-environment.cmd
    endlocal
    pause
    exit /b 1
)

SET STACK=widcomm
title %STACK%-BluetoothTCKAgent
java -Dbluecove.stack=%STACK% -cp %TCK_JSR82_HOME%\Bluetooth_1-1_005_TCK\BluetoothTCKAgent.zip;%BLUECOVE_JAR% BluetoothTCKAgent.BluetoothTCKAgentApp

if errorlevel 1 goto errormark
echo [Launched OK]
goto endmark
:errormark
	ENDLOCAL
	echo Error in start
	pause
:endmark
ENDLOCAL

@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\bluecove-tester\scripts\version.cmd
SET STACK=winsock
title %STACK%-obex

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1
set JVM_ARGS=%JVM_ARGS% -Dbluecove.obex.mtu=40000

java -Dbluecove.stack=%STACK% %JVM_ARGS% -jar target\obex-server-%BLUECOVE_VERSION%-app.jar
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL
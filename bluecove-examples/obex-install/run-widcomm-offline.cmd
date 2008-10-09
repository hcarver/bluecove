@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\bluecove-tester\scripts\version.cmd
SET STACK=widcomm
title %STACK%-obex
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1
java -Dbluecove.stack=%STACK% %JVM_ARGS% -jar target\obex-install-%BLUECOVE_VERSION%-mini.jar
if errorlevel 2 (
    echo Error calling java
)
ENDLOCAL
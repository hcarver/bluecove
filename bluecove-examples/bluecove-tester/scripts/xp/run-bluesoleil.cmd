@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark

SET STACK=bluesoleil
title %STACK%-tester
java -Dbluecove.stack=%STACK% -jar %BLUECOVE_TESTER_APP_JAR% >  run-%STACK%.cmd.log
if errorlevel 1 (
    echo Error calling java
    pause
)
:endmark
ENDLOCAL
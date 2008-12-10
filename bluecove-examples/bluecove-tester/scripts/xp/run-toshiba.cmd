@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark

SET STACK=toshiba
title %STACK%-tester
java -Dbluecove.stack=%STACK% -jar "%BLUECOVE_TESTER_APP_JAR%" >  run-%STACK%.cmd.log
if errorlevel 2 (
    echo Error calling java
)
pause java ends.

:endmark
ENDLOCAL
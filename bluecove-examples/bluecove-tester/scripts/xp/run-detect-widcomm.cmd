@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark

set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1
set JVM_ARGS=%JVM_ARGS% -Dbluecove.stack.first=widcomm

java %JVM_ARGS% -jar "%BLUECOVE_TESTER_APP_JAR%"
if errorlevel 1 (
    echo Error calling java
    pause
)
:endmark
ENDLOCAL
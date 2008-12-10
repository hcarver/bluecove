@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark

set JVM_ARGS=%JVM_ARGS% -Davetanabt.stack=microsoft

set CP=%BLUECOVE_3RDPARTY_HOME%\avetanaBluetooth\avetanaBluetooth.jar;%BLUECOVE_TESTER_JAR%;%MAVEN2_REPO%\junit\junit\3.8.1\junit-3.8.1.jar

java %JVM_ARGS% -cp "%CP%" %BLUECOVE_MAIN%
if errorlevel 1 (
    echo Error calling java
    pause
)
:endmark
ENDLOCAL
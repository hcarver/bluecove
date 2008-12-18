@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark

title BlueCove-tester-console

set JVM_ARGS=
set JVM_ARGS=%JVM_ARGS% -Dbluecove.connect.timeout=10000

set START_ARGS=-cp "%BLUECOVE_TESTER_CP%" -Dbluecove.native.path="%BLUECOVE_PROJECT_HOME%/src/main/resources" %BLUECOVE_MAIN%
rem set START_ARGS=-jar target\bluecove-tester-%VERSION%-app.jar

echo %START_ARGS%

java %JVM_ARGS% %START_ARGS% --console
if errorlevel 2 (
    echo Error calling java
)
pause java ends.
:endmark
ENDLOCAL
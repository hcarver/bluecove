@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark


@set CDCTK_HOME=%ProgramFiles%\CDCTK10

set JVM_ARGS=
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1
rem set JVM_ARGS=%JVM_ARGS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y

title CDCTK 1.0

set CP=%BLUECOVE_TESTER_HOME%\target\classes
rem set CP=%CP%;%BLUECOVE_PROJECT_HOME%\target\classes
set CP=%CP%;%BLUECOVE_TESTER_HOME%\target\cldcunit.jar
set CP=%CP%;%BLUECOVE_TESTER_HOME%\target\cldcunit-se.jar


set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.resource=false
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.path=%BLUECOVE_PROJECT_HOME%\src\main\resources
set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.path=%CDCTK_HOME%\bin

set BOOTCLASSPATH=%CDCTK_HOME%\lib\agui-1.0.jar;%CDCTK_HOME%\lib\rt.jar;%CDCTK_HOME%\lib\jaas.jar;%CDCTK_HOME%\lib\jce.jar;%CDCTK_HOME%\lib\jsse-cdc.jar
set BOOTCLASSPATH=%BOOTCLASSPATH%;%BLUECOVE_PROJECT_HOME%\target\classes

set JVM_ARGS=%JVM_ARGS% -Xbootclasspath:%BOOTCLASSPATH%

set START_ARGS=-cp %CP% %BLUECOVE_MAIN%
rem set START_ARGS=-jar %BLUECOVE_TESTER_APP_JAR%

%CDCTK_HOME%\bin\emulator %JVM_ARGS% %START_ARGS%

pause
:endmark
ENDLOCAL
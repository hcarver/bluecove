@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark

@set JAVA_HOME=%ProgramFiles%\IBM\WEME\runtimes\61\win-x86-ppro10

@if exist "%JAVA_HOME%\bin" goto 3p_found
@echo Error: JAVA not found in folder [%JAVA_HOME%]
pause
goto :endmark
:3p_found


set JVM_ARGS=
set JVM_ARGS=%JVM_ARGS% -jcl:ppro10

set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

title J9 PPRO1.0-v6.1.1

"%JAVA_HOME%\bin\j9" %JVM_ARGS% -classpath "%BLUECOVE_TESTER_APP_JAR%" %BLUECOVE_MAIN%

pause
:endmark
ENDLOCAL
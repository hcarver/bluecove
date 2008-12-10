@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark


@set JAVA_HOME=%ProgramFiles%\IBM\DeviceDeveloper\wsdd5.0\ive-2.2

@if exist "%JAVA_HOME%\bin" goto 3p_found
@echo Error: JAVA not found in folder [%JAVA_HOME%]
pause
goto :endmark
:3p_found


set JVM_ARGS=

set JVM_ARGS=%JVM_ARGS% -jcl:midp20
rem set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth

set JVM_ARGS=%JVM_ARGS% -Dcom.ibm.oti.vm.bootstrap.library.path=%~dp0..\..\bluecove\src\main\resources;%JAVA_HOME%\bin

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.path=%~dp0..\..\bluecove\src\main\resources

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

call "%~dp0..\set-stack.cmd"

copy %BLUECOVE_JAR% "%JAVA_HOME%\lib\jclMidp20\ext\bluecove.jar"

title J9 MIDP20-v5.7.2

"%JAVA_HOME%\bin\j9.exe" %JVM_ARGS% -cp %BLUECOVE_TESTER_HOME%\target\bctest.jar "-jxe:%JAVA_HOME%\lib\jclMidp20\jclMidp20.jxe" %BLUECOVE_TESTER_HOME%\target\bctest.jad

pause
:endmark
ENDLOCAL
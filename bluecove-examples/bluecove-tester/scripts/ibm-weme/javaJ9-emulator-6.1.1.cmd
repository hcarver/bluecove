@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark


@set JAVA_HOME=%ProgramFiles%\IBM\WEME\runtimes\61\win-x86-midp20

@if exist "%JAVA_HOME%\bin" goto 3p_found
@echo Error: JAVA not found in folder [%JAVA_HOME%]
pause
goto :endmark
:3p_found


set JVM_ARGS=

rem set JVM_ARGS=%JVM_ARGS% -jcl:midp20
rem set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth

rem set JVM_ARGS=%JVM_ARGS% -Dcom.ibm.oti.vm.bootstrap.library.path=%~dp0..\..\bluecove\src\main\resources;%JAVA_HOME%\bin

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.path=%~dp0..\..\bluecove\src\main\resources

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

call "%~dp0..\set-stack.cmd"

@if exist %JAVA_HOME%\lib\jclMidp20\ext goto ext_exists
@mkdir %JAVA_HOME%\lib\jclMidp20\ext
:ext_exists
copy %BLUECOVE_JAR% "%JAVA_HOME%\lib\jclMidp20\ext\bluecove.jar"

"%JAVA_HOME%\bin\emulator.exe" %JVM_ARGS%

pause
:endmark
ENDLOCAL
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

set JVM_ARGS=%JVM_ARGS% -jcl:midp20
set JVM_ARGS=%JVM_ARGS% -Xbootclasspath:%JAVA_HOME%\lib\jclMidp20\jclMidp20.jxe
rem set JVM_ARGS=%JVM_ARGS% -jxe %JAVA_HOME%\lib\jclMidp20\jclMidp20.jxe

rem set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

call "%~dp0..\set-stack.cmd"

@if exist "%JAVA_HOME%\lib\jclMidp20\ext" goto :ext_exists
@echo Create directory "%JAVA_HOME%\lib\jclMidp20\ext"
mkdir "%JAVA_HOME%\lib\jclMidp20\ext"
:ext_exists

copy "%BLUECOVE_JAR%" "%JAVA_HOME%\lib\jclMidp20\ext\bluecove.jar"
copy "%BLUECOVE_PROJECT_HOME%\src\main\resources\*.dll" "%JAVA_HOME%\bin"

title J9 MIDP20-v6.1.1

%JAVA_HOME%\bin\j9 %JVM_ARGS% -cp "%BLUECOVE_TESTER_HOME%\target\bctest.jar" javax.microedition.lcdui.AppManager "%BLUECOVE_TESTER_HOME%\target\bctest.jad"

pause
:endmark
ENDLOCAL
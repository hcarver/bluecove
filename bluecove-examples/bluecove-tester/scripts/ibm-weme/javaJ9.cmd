@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark

SET JAVA_HOME=%ProgramFiles%\IBM\DeviceDeveloper\wsdd5.0\ive-2.2\runtimes\win32\x86\ppro10

set JVM_ARGS=%JVM_ARGS% -jcl:ppro10
set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth

%JAVA_HOME%\bin\j9  %JVM_ARGS% -classpath %BLUECOVE_TESTER_APP_JAR% net.sf.bluecove.awt.Main
pause
:endmark
ENDLOCAL

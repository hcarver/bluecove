@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark

SET JAVA_HOME=D:\jdk1.1.8

set JVM_ARGS=
set JVM_ARGS=%JVM_ARGS% -Dbluecove.connect.timeout=10000

call %~dp0scripts\set-stack.cmd

%JAVA_HOME%\bin\java %JVM_ARGS% -Djava.compiler=NONE -classpath %JAVA_HOME%\lib\classes.zip;%BLUECOVE_TESTER_APP_JAR% net.sf.bluecove.awt.Main

pause

:endmark
ENDLOCAL

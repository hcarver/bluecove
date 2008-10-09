@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark

title bluecove-1.2.3-tester
java -classpath %BLUECOVE_TESTER_HOME%src\site\resources\bluecove-1.2.3-signed.jar;%BLUECOVE_TESTER_APP_JAR% net.sf.bluecove.awt.Main >  run-winsock-1.2.3.cmd.log
if errorlevel 1 (
    echo Error calling java
    pause
)
:endmark
ENDLOCAL
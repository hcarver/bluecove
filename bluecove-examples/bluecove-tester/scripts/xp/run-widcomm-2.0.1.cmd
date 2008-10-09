@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark

SET STACK=widcomm
title bluecove-2.0.1-tester
java  -Dbluecove.stack=%STACK% -classpath %BLUECOVE_TESTER_HOME%\src\site\resources\bluecove-2.0.1-signed.jar;%BLUECOVE_TESTER_APP_JAR% net.sf.bluecove.awt.Main >  run-%STACK%-2.0.1.cmd.log
if errorlevel 1 (
    echo Error calling java
    pause
)
:endmark
ENDLOCAL
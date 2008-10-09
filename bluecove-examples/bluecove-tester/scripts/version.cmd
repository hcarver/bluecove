@echo off
rem @version $Revision$ ($Author$)  $Date$

if exist %~dp0generated-version.cmd goto generated_version_found
echo 
echo %~dp0generated-version.cmd Not Found, run maven first
goto :errormark

:generated_version_found
call %~dp0generated-version.cmd
rem echo BLUECOVE_VERSION=%BLUECOVE_VERSION%
goto :endmark

:errormark
	exit /b 1
:endmark
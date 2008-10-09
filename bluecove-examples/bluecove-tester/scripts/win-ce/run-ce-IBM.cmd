@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call %~dp0environment.cmd %*
if errorlevel 1 (
    echo Error calling environment.cmd
    endlocal
    pause
    exit /b 1
)

set JVM_ARGS=%WIN_CE_JVM_ARGS%
set JVM_ARGS=%JVM_ARGS% -jcl:ppro10
set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth
set JVM_ARGS=%JVM_ARGS% -Dbluecove.stack=widcomm

%WMDPT%\RAPI_Start\rapistart "\Program Files\J9\PPRO10\bin\j9.exe" %JVM_ARGS% -cp "%BLUECOVE_INSTALL_DIR%\bluecove-tester.jar" net.sf.bluecove.awt.Main

rem %WMDPT%\RAPI_Start\rapistart "\bluecove\BlueCove-IBM"

if errorlevel 1 goto errormark
echo [Launched OK]
goto endmark
:errormark
	ENDLOCAL
	echo Error in start
	pause
:endmark
ENDLOCAL

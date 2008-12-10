@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call "%~dp0environment.cmd" %*
if errorlevel 1 (
    echo Error calling environment.cmd
    endlocal
    pause
    exit /b 1
)

%WMDPT%\CECopy\cecopy "%DEFAULT_BUILD_HOME%\target\bluecove-tester-%VERSION%-%ASSEMBLY_ID%.jar" "dev:%BLUECOVE_INSTALL_DIR%\bluecove-tester.jar"

if errorlevel 1 goto errormark
echo [Copy OK]
goto endmark
:errormark
	ENDLOCAL
	echo Error in build
	pause
:endmark
ENDLOCAL

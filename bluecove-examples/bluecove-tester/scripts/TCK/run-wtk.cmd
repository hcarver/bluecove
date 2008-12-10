@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call "%~dp0tck-environment.cmd" %*
if errorlevel 1 (
    echo Error calling tck-environment.cmd
    endlocal
    pause
    exit /b 1
)

echo WTK_HOME=[%WTK_HOME%]

rem set PATH=%JAVA_HOME%\bin;%PATH%

rem set ARGS=-Xdomain:trusted
rem set ARGS=-Xdomain:untrusted
set ARGS=-Xdomain:manufacturer
rem Since WTK 2.5
rem set ARGS=-Xdomain:maximum

rem If On start or in DEBUG: Uncaught exception java/lang/SecurityException: Application not authorized to access the restricted API.
rem need to sign midlets in GATLING for WTK!  See wtk-keys\keys.txt


rem set ARGS=%ARGS% -Xverbose:class
rem set ARGS=%ARGS% -Xverbose:exceptions

title TCK tests on Sun WTK

"%WTK_HOME%\bin\emulator.exe" %ARGS% -Xautotest:http://%BLUECOVE_TCK_HOST%:%BLUECOVE_TCK_PORT%/getNextApp.jad

if errorlevel 1 goto errormark
echo [Launched OK]
pause
goto endmark
:errormark
	ENDLOCAL
	echo Error in start
	pause
:endmark
ENDLOCAL

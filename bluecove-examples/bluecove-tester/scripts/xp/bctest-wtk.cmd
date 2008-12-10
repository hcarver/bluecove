@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\environment.cmd"
if errorlevel 1 goto endmark

rem set WTK_HOME=%ProgramFiles%\WTK252

@if exist "%WTK_HOME%\bin" goto 3p_found
@echo Error: WTK not found in folder [%WTK_HOME%]
pause
goto :endmark
:3p_found

set EMULATOR_ARGS=
rem set EMULATOR_ARGS=%EMULATOR_ARGS% -Xverbose:class

title WTK

"%WTK_HOME%\bin\emulator" %EMULATOR_ARGS% "-Xdescriptor:%BLUECOVE_TESTER_HOME%\target\bctest.jad"

pause
:endmark
ENDLOCAL
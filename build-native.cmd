@rem $Id$

@rem @echo off
@SETLOCAL

@echo Starting build at %TIME%, %DATE%

@rem %~dp0 is expanded pathname of the current script under NT
@set DEFAULT_BUILD_HOME=%~dp0
@rem get Parent Directory
@for /f %%i in ("%DEFAULT_BUILD_HOME%..") do @set DEFAULT_BUILD_HOME=%%~fi

@if exist %JAVA_HOME%/include/win32 goto java_found
@echo JAVA_HOME Not Found
:java_found

%JAVA_HOME%\bin\javah -d intelbth -classpath build com.intel.bluetooth.BluetoothPeer
@if errorlevel 1 goto errormark

@set p=%ProgramFiles%\Microsoft Visual Studio 8\VC\bin
@if exist "%p%\VCVARS32.BAT" goto vs_found

@echo Visual Studio Not Found
@goto :errormark

:vs_found
@echo Found Visual Studio %p%
@set PATH=%p%;%PATH%

@rem PATH=%DEFAULT_BUILD_HOME%\bin;%PATH%

@echo [%p%\VCVARS32.BAT]
call "%p%\VCVARS32.BAT"

@set sdk=%ProgramFiles%\Microsoft SDKs\Windows\v6.0
@if exist "%sdk%\Include" goto sdk_found
@echo Microsoft SDKs Not Found
@goto :errormark
:sdk_found
@echo Microsoft SDKs Found [%sdk%]
@set INCLUDE=%sdk%\Include;%INCLUDE%
@set LIB=%sdk%\Lib;%LIB%

gmake.exe -fmakefile %* default
@if errorlevel 1 goto errormark
@echo [Build OK]
@goto endmark
:errormark
	@ENDLOCAL
	echo Error in build
	pause
:endmark
@ENDLOCAL
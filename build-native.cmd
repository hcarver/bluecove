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

@rem %JAVA_HOME%\bin\javah -d src\main\c\intelbth -classpath target\classes com.intel.bluetooth.BluetoothPeer
@rem @if errorlevel 1 goto errormark

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

@rem gmake.exe -fmakefile %* default

@set CONFIGURATION=Winsock

@rem configuration:  Winsock, WIDCOMM, BlueSoleil or Release

@set sdk_widcomm=%ProgramFiles%\Widcomm\BTW DK\SDK
@if NOT exist "%sdk_widcomm%" goto sdk_other_not_found
@echo Widcomm SDKs Found [%sdk_widcomm%]
@set CONFIGURATION=WIDCOMM

@set sdk_BlueSoleil=%ProgramFiles%\IVT Corporation\BlueSoleil\api
@if NOT exist "%sdk_BlueSoleil%" goto sdk_other_not_found
@echo BlueSoleil SDKs Found [%sdk_BlueSoleil%]
@set CONFIGURATION=Release
@echo All Supported SDK not found. Will use Release configuration
@goto DO_BUILD

:sdk_other_not_found
@echo WARNING: Some Supported SDK not found!

:DO_BUILD
vcbuild /rebuild src\main\c\intelbth\intelbth.sln "%CONFIGURATION%|Win32"
@if errorlevel 1 goto errormark
@echo [Build OK]
@goto endmark
:errormark
	@ENDLOCAL
	echo Error in build
	pause
:endmark
@ENDLOCAL
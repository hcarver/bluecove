@rem $Id$

@rem @echo off
@SETLOCAL

@echo Starting build at %TIME%, %DATE%

@rem %~dp0 is expanded pathname of the current script under NT
@set DEFAULT_BUILD_HOME=%~dp0
@rem get Parent Directory
@rem for /f %%i in ("%DEFAULT_BUILD_HOME%..") do @set DEFAULT_BUILD_HOME=%%~fi

@set CALLED_FROM_MAVEN=0
@if /I '%1' EQU '-maven' (
    set CALLED_FROM_MAVEN=1
)
@echo CALLED_FROM_MAVEN=%CALLED_FROM_MAVEN%
    
@if exist %JAVA_HOME%/include/win32 goto java_found
@echo WARN: JAVA_HOME Not Found
:java_found

@rem %JAVA_HOME%\bin\javah -d src\main\c\intelbth -classpath target\classes com.intel.bluetooth.BluetoothPeer
@rem @if errorlevel 1 goto errormark

@set p=%ProgramFiles%\Microsoft Visual Studio\VC98\bin
@if exist "%p%\VCVARS32.BAT" goto vs_found

@echo Visual Studio 6 Not Found
@goto :errormark

:vs_found
@echo Found Visual Studio %p%
@set PATH=%p%;%PATH%

@rem PATH=%DEFAULT_BUILD_HOME%\bin;%PATH%

@echo [%p%\VCVARS32.BAT]
call "%p%\VCVARS32.BAT"

@rem WIDCOMM build by VC6
@set sdk_widcomm=%ProgramFiles%\Widcomm\BTW DK\SDK
@if NOT exist "%sdk_widcomm%" goto sdk_other_not_found
@echo Widcomm SDKs Found [%sdk_widcomm%]
@set INCLUDE=%INCLUDE%;%sdk_widcomm%\Inc
@set LIB=%LIB%;%sdk_widcomm%\Release

@echo Supported SDK found. Will use Release configuration
@goto DO_BUILD

:sdk_other_not_found
@echo WARNING: Some Supported SDK not found!
@goto errormark

:DO_BUILD
cd "%DEFAULT_BUILD_HOME%\src\main\c\intelbth"
nmake /A /F bluecove.mak
@if errorlevel 1 goto errormark
@echo [Build OK]
@cd "%DEFAULT_BUILD_HOME%"
copy src\main\resources\bluecove.dll target\classes\
@if errorlevel 1 goto errormark
@goto endmark
:errormark
    @cd "%DEFAULT_BUILD_HOME%"
	@echo Error in build
    @if "%CALLED_FROM_MAVEN%" == "1" (
        @ENDLOCAL
        exit 1
    )
    @ENDLOCAL
    pause
:endmark
@ENDLOCAL
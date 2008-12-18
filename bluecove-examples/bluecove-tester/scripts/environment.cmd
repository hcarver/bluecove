@echo off
rem @version $Revision$ ($Author$)  $Date$

call "%~dp0version.cmd"
if errorlevel 1 goto errormark

set DEFAULT_BUILD_HOME=%~dp0
for /f "delims=" %%i in ("%DEFAULT_BUILD_HOME%..") do @set DEFAULT_BUILD_HOME=%%~fi

set BLUECOVE_TESTER_HOME=%DEFAULT_BUILD_HOME%
rem echo BLUECOVE_TESTER_HOME=[%BLUECOVE_TESTER_HOME%]

rem set BLUECOVE_HOME=%BLUECOVE_TESTER_HOME%\..\..
for /f "delims=" %%i in ("%BLUECOVE_TESTER_HOME%\..") do @set BLUECOVE_HOME=%%~fi
for /f "delims=" %%i in ("%BLUECOVE_HOME%\..") do @set BLUECOVE_HOME=%%~fi

set BLUECOVE_PROJECT_HOME=%BLUECOVE_HOME%\bluecove
rem echo BLUECOVE_PROJECT_HOME=[%BLUECOVE_PROJECT_HOME%]

set BLUECOVE_JAR=%BLUECOVE_PROJECT_HOME%\target\bluecove-%BLUECOVE_VERSION%.jar
set BLUECOVE_EMU_JAR=%BLUECOVE_HOME%\bluecove-emu\target\bluecove-emu-%BLUECOVE_VERSION%.jar
set BLUECOVE_TESTER_JAR=%BLUECOVE_TESTER_HOME%\target\bluecove-tester-%BLUECOVE_VERSION%.jar
set BLUECOVE_TESTER_APP_JAR=%BLUECOVE_TESTER_HOME%\target\bluecove-tester-%BLUECOVE_VERSION%-app.jar
set BLUECOVE_TESTER_BASE_PROJECT_HOME=%BLUECOVE_HOME%\bluecove-examples\bluecove-tester-base

set BLUECOVE_TESTER_CP=%BLUECOVE_TESTER_HOME%\target\classes
set BLUECOVE_TESTER_CP=%BLUECOVE_TESTER_CP%;%BLUECOVE_PROJECT_HOME%\target\classes
set BLUECOVE_TESTER_CP=%BLUECOVE_TESTER_CP%;%BLUECOVE_TESTER_BASE_PROJECT_HOME%\target\classes
set BLUECOVE_TESTER_CP=%BLUECOVE_TESTER_CP%;%BLUECOVE_TESTER_HOME%\target\cldcunit.jar
rem set BLUECOVE_TESTER_CP=%BLUECOVE_TESTER_CP%;%BLUECOVE_TESTER_HOME%\target\cldcunit-se.jar

@if exist "%BLUECOVE_JAR%" goto bluecove_jar_found
@echo Error: BLUECOVE_JAR [%BLUECOVE_JAR%] Not found
goto :errormark
:bluecove_jar_found

rem set BLUECOVE_MAIN=net.sf.bluecove.awt.Main
set BLUECOVE_MAIN=net.sf.bluecove.se.Main

rem set BLUECOVE_3RDPARTY_HOME=%BLUECOVE_HOME%\..\3p
for /f "delims=" %%i in ("%BLUECOVE_HOME%\..") do @set BLUECOVE_3RDPARTY_HOME=%%~fi\3p

set JVM_ARGS=

set ALL_JAVA_BASE=D:

rem set JAVA_HOME=D:\jdk1.5.0
rem set JAVA_HOME=D:\harmony-jdk-629320
rem set PATH=%JAVA_HOME%\bin;%PATH%

set MAVEN2_REPO=%HOMEDRIVE%\%HOMEPATH%\.m2\repository
set PATH=%JAVA_HOME%\bin;%PATH%

set WMDPT="%ProgramFiles%\Windows Mobile Developer Power Toys"
if exist %WMDPT%\CECopy\cecopy.exe goto pt_found
echo WARN: Windows Mobile Developer Power Toys Not Found
rem goto :errormark

:pt_found

set _JVM_MYSAIFU=true
set WIN_CE_PHONE=true

set ASSEMBLY_ID=app

set WIN_CE_JVM_ARGS=
rem set WIN_CE_JVM_ARGS=%WIN_CE_JVM_ARGS% -Dbluecove.debug=true

if NOT '%WIN_CE_PHONE%' EQU 'true' (
    set BLUECOVE_INSTALL_DIR=\bluecove
)

if '%WIN_CE_PHONE%' EQU 'true' (
    set BLUECOVE_INSTALL_DIR=\Storage\bluecove
    set ASSEMBLY_ID=phone
)

rem set BLUECOVE_INSTALL_DIR=\Storage Card\bluecove

goto endmark
:errormark
    pause
	exit /b 1
:endmark


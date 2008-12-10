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

@if exist "%GATLING_CORE_ROOT%" goto gatling_found
@echo Error: GATLING CORE [%GATLING_CORE_ROOT%] Not found
goto :errormark
:gatling_found

@if exist "%WTK_HOME%" goto wtk_found
@echo Error: WTK_HOME [%WTK_HOME%] Not found
goto :errormark
:wtk_found

@if exist "%WTK_HOME%\lib\cldcapi10.jar" goto wtk_lib_found
@echo Error: WTK [%WTK_HOME%\lib\cldcapi10.jar] Not found
goto :errormark
:wtk_lib_found

call "%GATLING_CORE_ROOT%\bin\SignatureTest.bat" "%GATLING_CORE_ROOT%\lib" "%TCK_JSR82_HOME%\Bluetooth_%TCK_VERSION_ID%_TCK\SignatureTests\CLDC" "%BLUECOVE_JAR%" -report Bluetooth_%TCK_VERSION_ID%_TCK_CLDC_Signature-Report.txt -jars "%WTK_HOME%\lib\cldcapi10.jar"

call "%GATLING_CORE_ROOT%\bin\SignatureTest.bat" "%GATLING_CORE_ROOT%\lib" "%TCK_JSR82_HOME%\OBEX_%TCK_VERSION_ID%_TCK\SignatureTests\CLDC" "%BLUECOVE_JAR%" -report OBEX_%TCK_VERSION_ID%_TCK_CLDC_Signature-Report.txt -jars "%WTK_HOME%\lib\cldcapi10.jar"

if errorlevel 1 goto errormark
echo [Launched OK]
goto endmark
:errormark
	ENDLOCAL
	echo Error in start
	pause
:endmark
ENDLOCAL

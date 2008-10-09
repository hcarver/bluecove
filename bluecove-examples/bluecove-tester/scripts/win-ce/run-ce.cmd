@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call %~dp0scripts\environment.cmd %*
if errorlevel 1 (
    echo Error calling environment.cmd
    endlocal
    pause
    exit /b 1
)

rem cd ..\..\bluecove
call m2
rem cd %~dp0
rem call m2
call scripts\install-ce.cmd

if NOT '%WIN_CE_PHONE%' EQU 'true' (
    if '%JVM_MYSAIFU%' EQU 'true' (
        call scripts\run-ce-Mysaifu.cmd
    ) else (
        call scripts\run-ce-IBM.cmd
    )
)

if '%WIN_CE_PHONE%' EQU 'true' (
    call scripts\run-ce-Mysaifu-phone.cmd
)
ENDLOCAL
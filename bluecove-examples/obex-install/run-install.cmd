@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call "%~dp0..\bluecove-tester\scripts\version.cmd"

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

java %JVM_ARGS% -cp target\obex-install-%BLUECOVE_VERSION%-mini.jar net.sf.bluecove.obex.Deploy btgoep://0019639c4007:6  src\main\resources\icon.png
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL
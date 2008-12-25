@echo off
rem @version $Revision$ ($Author$)  $Date$
rem helper to run maven2
rem
if not defined MAVEN_OPTS  set MAVEN_OPTS=-Xmx256M

rem call mvn clean
rem @if errorlevel 1 goto errormark

call mvn clean deploy -P build -P release -DperformRelease=true
@if errorlevel 1 goto errormark

call mvn site-deploy -P build -P release -DperformRelease=true
@if errorlevel 1 goto errormark
@goto endmark
:errormark
	@echo Error in build
    pause
:endmark

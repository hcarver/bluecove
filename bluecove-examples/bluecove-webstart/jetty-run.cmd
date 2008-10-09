@echo off
rem @version $Revision$ ($Author$)  $Date$
title *Jetty:microemu-webstart

call mvn -o -P debug webstart:jnlp
echo Go to http://localhost:8080/bluecove-webstart/
call mvn %* jetty:run

title Jetty:microemu-webstart - ended

pause

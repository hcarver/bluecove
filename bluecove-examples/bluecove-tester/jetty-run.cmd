@echo off
rem @version $Revision$ ($Author$)  $Date$
title *Jetty:bluecove-tester

call mvn -o package site
echo Go to http://localhost:8080/bluecove-tester/awt-bctest-localhost.jnlp
call mvn %* jetty:run

title Jetty:bluecove-tester - ended

pause

#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
. ./tck-environment.sh

BLUECOVE_TCK_CP="${MICROEMULATOR_HOME}/microemulator.jar"
BLUECOVE_TCK_CP="${BLUECOVE_TCK_CP}:${BLUECOVE_JAR}:${BLUECOVE_GPL_JAR}"

java -cp ${BLUECOVE_TCK_CP} org.microemu.app.Main -Xautotest:http://${BLUECOVE_TCK_HOST}:${BLUECOVE_TCK_PORT}/getNextApp.jad
rc=$?
if [ ! "${rc}" = "0" ]; then
    echo Error calling java
    echo Prsess enter to exit
    read
fi

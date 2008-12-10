#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname "${0}"`/..
. "${SCRIPTS_DIR}/environment.sh"
if [[ ! "$?" = "0" ]]; then
    echo Error calling environment.sh
    exit 1
fi

JAVA_HOME=/usr/lib/jvm/java-gcj

#echo BLUECOVE_TESTER_APP_JAR=${BLUECOVE_TESTER_APP_JAR}

${JAVA_HOME}/bin/java -classpath "${BLUECOVE_TESTER_APP_JAR}" ${BLUECOVE_MAIN} $*

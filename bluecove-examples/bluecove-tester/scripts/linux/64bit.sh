#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname "${0}"`/..
. "${SCRIPTS_DIR}/environment.sh"
if [ ! "$?" = "0" ]; then
    echo Error calling environment.sh
    exit 1
fi

java -d64 -cp "${BLUECOVE_TESTER_CP}" ${BLUECOVE_MAIN} $*
rc=$?
if [ ! "${rc}" = "0" ]; then
    echo Error calling java
    echo Prsess enter to exit
    read
fi

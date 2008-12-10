#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname "${0}"`/..
. "${SCRIPTS_DIR}/environment.sh"
if [[ ! "$?" = "0" ]]; then
    echo Error calling environment.sh
    exit 1
fi

JVM_ARGS=
ERRORS=0
SUCCESS=0
echo Starting > run-continuous.log

break_tests=0

while [ "${break_tests}" = "0" ]
do

echo "-----------------------------"
echo "--- SUCCESS=${SUCCESS} ERRORS=${ERRORS} ---"
echo "-----------------------------"

java ${JVM_ARGS} -cp "${BLUECOVE_TESTER_APP_JAR}" ${BLUECOVE_MAIN} --runonce  >>  run-continuous.log
rc=$?
#echo "rc=[${rc}]"
if [ "${rc}" = "2" ]; then
    echo Error calling java
    let "ERRORS += 1"
elif [ "${rc}" = "1" ]; then
    let "SUCCESS += 1"
elif [ "${rc}" = "3" ]; then
    echo "No tests executed"
else
    echo "End of tests rc=[${rc}]"
    break_tests=1
fi

done

echo "--------- Done --------------"
echo "--- SUCCESS=${SUCCESS} ERRORS=${ERRORS} ---"
echo "-----------------------------"

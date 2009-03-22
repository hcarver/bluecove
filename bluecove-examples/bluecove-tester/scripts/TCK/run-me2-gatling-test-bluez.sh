#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
. ./tck-environment.sh
if [ ! "$?" = "0" ]; then
    echo Error calling tck-environment.sh
    exit 1
fi

if [ ! -d "${MICROEMULATOR_HOME}" ] ; then
  echo "Invalid Microemulator directory ${MICROEMULATOR_HOME}"
  exit 1
fi

if [ ! -f "${MICROEMULATOR_HOME}/microemulator.jar" ] ; then
  echo "Microemulator jar not found in directory ${MICROEMULATOR_HOME}"
  exit 1
fi

if [ ! -f "${BLUECOVE_BLUEZ_JAR}" ] ; then
  echo "BlueCove-BLUEZ not found ${BLUECOVE_BLUEZ_JAR}"
  exit 1
fi

BLUECOVE_TCK_CP="${MICROEMULATOR_HOME}/microemulator.jar"
BLUECOVE_TCK_CP="${BLUECOVE_TCK_CP}:${BLUECOVE_JAR}:${BLUECOVE_BLUEZ_JAR}"
BLUECOVE_TCK_CP="${BLUECOVE_TCK_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/dbus.jar"
BLUECOVE_TCK_CP="${BLUECOVE_TCK_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/debug-disable.jar"
BLUECOVE_TCK_CP="${BLUECOVE_TCK_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/unix.jar"

JVM_ARGS=
JVM_ARGS="${JVM_ARGS} -Dbluecove.debug=1"

java -cp "${BLUECOVE_TCK_CP}" ${JVM_ARGS} ${MICROEMULATOR_MAIN} ${MICROEMULATOR_ARGS} -Xautotest:http://${BLUECOVE_TCK_HOST}:${BLUECOVE_TCK_PORT}/getNextApp.jad  | tee tck_test-dbus.log
rc=$?
if [ ! "${rc}" = "0" ]; then
    echo Error calling java
    echo Prsess enter to exit
    read
fi

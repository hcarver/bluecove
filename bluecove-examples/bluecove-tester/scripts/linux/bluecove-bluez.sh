#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname "${0}"`/..
. "${SCRIPTS_DIR}/environment.sh"
if [ ! "$?" = "0" ]; then
    echo Error calling environment.sh
    exit 1
fi

BLUECOVE_BLUEZ_CP="${BLUECOVE_JAR}"
#BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_JAR}"
BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/classes"

DBUS_JAVA_JAR=/usr/share/java/dbus-java/dbus.jar

if [ -f "${DBUS_JAVA_JAR}" ] ; then
    echo "dbus-java installation found ${DBUS_JAVA_JAR}"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${DBUS_JAVA_JAR}"
    if [ "$HOSTTYPE" = "x86_64" ]; then
        LIBMATTHEW_JAVA_DIR=/usr/lib64/libmatthew-java
    else
        LIBMATTHEW_JAVA_DIR=/usr/lib/libmatthew-java
    fi
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${LIBMATTHEW_JAVA_DIR}/unix.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${LIBMATTHEW_JAVA_DIR}/debug-disable.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${LIBMATTHEW_JAVA_DIR}/hexdump.jar"
else
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/dbus.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/debug-disable.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/unixsockets.jar"
fi


BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_TESTER_JAR}"

java -cp "${BLUECOVE_BLUEZ_CP}" ${BLUECOVE_MAIN} $*
rc=$?
if [ ! "${rc}" = "0" ]; then
    echo Error calling java
    echo Prsess enter to exit
    read
fi

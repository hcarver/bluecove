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
JAVA_ARGS=

case "$MACHTYPE" in
    *-suse-*)
        DBUS_JAVA_JAR=/usr/share/java/dbus.jar
        ;;
    *-redhat-*)
        DBUS_JAVA_JAR=/usr/share/java/dbus-java/dbus.jar
        ;;
esac

#BLUECOVE_USE_RPM=true; export BLUECOVE_USE_RPMS

if [ -f "${DBUS_JAVA_JAR}" -a "${BLUECOVE_USE_RPM}" == "true" ] ; then

    echo "dbus-java installation found ${DBUS_JAVA_JAR}"
    echo "Use: BLUECOVE_USE_RPM=false; export BLUECOVE_USE_RPM; to use library from maven repository"
    echo ""

    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${DBUS_JAVA_JAR}"

    case "$MACHTYPE" in
    *-suse-*)
        LIBMATTHEW_JAVA_DIR=/usr/share/java
        if [ "$HOSTTYPE" = "x86_64" ]; then
            JAVA_ARGS=-Djava.library.path=/usr/lib64
        else
            JAVA_ARGS=-Djava.library.path=/usr/lib
        fi
        ;;
    *-redhat-*)
        if [ "$HOSTTYPE" = "x86_64" ]; then
            LIBMATTHEW_JAVA_DIR=/usr/lib64/libmatthew-java
        else
            LIBMATTHEW_JAVA_DIR=/usr/lib/libmatthew-java
        fi
        ;;
    esac

    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${LIBMATTHEW_JAVA_DIR}/unix.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${LIBMATTHEW_JAVA_DIR}/debug-disable.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${LIBMATTHEW_JAVA_DIR}/hexdump.jar"
    #JAVA_ARGS=-Djava.library.path=${LIBMATTHEW_JAVA_DIR}
else
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/dbus.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/debug-disable.jar"
    BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_BLUEZ_PROJECT_HOME}/target/unix.jar"
fi


BLUECOVE_BLUEZ_CP="${BLUECOVE_BLUEZ_CP}:${BLUECOVE_TESTER_JAR}"

java ${JAVA_ARGS} -cp "${BLUECOVE_BLUEZ_CP}" ${BLUECOVE_MAIN} $*
rc=$?
if [ ! "${rc}" = "0" ]; then
    echo Error calling java
    echo Prsess enter to exit
    read
fi

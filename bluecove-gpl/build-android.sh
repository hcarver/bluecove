#!/bin/sh
# @version $Id$
#
# Created by Francois Kooman, modified for Android by Dennis Munsie
#
# Use this file in case you don't have ant or maven installed on the system
#
# requirements: agcc, javac, javah, bluez, jar

BUILD_DIR=`pwd`

BLUECOVE_VERSION=2.1.1-SNAPSHOT

SRC_JAVA_DIR=${BUILD_DIR}/src/main/java
SRC_C_DIR=${BUILD_DIR}/src/main/c
TARGET_DIR=${BUILD_DIR}/target
CLASSES_DIR=${TARGET_DIR}/classes
OBJ_DIR=${TARGET_DIR}/native
JAVAC_OPTIONS="-g -Xlint:unchecked -source 1.3 -target 1.1"

BLUECOVE_JAR="${BUILD_DIR}/../bluecove/target/bluecove-${BLUECOVE_VERSION}.jar"
if [[ ! -f ${BLUECOVE_JAR} ]] ; then
  echo "BlueCove jar not found ${BLUECOVE_JAR}"
  exit 1
fi

mkdir -p ${CLASSES_DIR}

echo "=== Compile the bluez stack java files ==="
javac -d ${CLASSES_DIR} ${JAVAC_OPTIONS} -cp ${BLUECOVE_JAR} ${SRC_JAVA_DIR}/com/intel/bluetooth/BluetoothStackBlueZ*.java
if [[ ! "$?" = "0" ]]; then
    echo Error in Java compilation
    exit 1
fi

echo "=== Generate the JNI C header files from these java files ==="
javah -d ${SRC_C_DIR} -classpath ${CLASSES_DIR} com.intel.bluetooth.BluetoothStackBlueZ com.intel.bluetooth.BluetoothStackBlueZConsts com.intel.bluetooth.BluetoothStackBlueZNativeTests
if [[ ! "$?" = "0" ]]; then
    echo Error in JNI haders creation
    exit 1
fi

echo "=== Compile the C files ==="
mkdir -p ${OBJ_DIR}
cd ${OBJ_DIR}
agcc -fPIC -c ${SRC_C_DIR}/*.c -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
if [[ ! "$?" = "0" ]]; then
    echo Error in C compilation
    cd ${BUILD_DIR}
    exit 1
fi
cd ${BUILD_DIR}

echo "=== Link object file into the library ==="
agcc -shared -lbluetooth -ldl -o ${BUILD_DIR}/target/libbluecove.so ${OBJ_DIR}/*.o
if [[ ! "$?" = "0" ]]; then
    echo Error in linking
    exit 1
fi

# copy the shared library to classes directory
cp ${BUILD_DIR}/target/libbluecove.so ${CLASSES_DIR}

echo "Native library ${BUILD_DIR}/target/libbluecove.so created"

# build the jar file
jar cvf ${TARGET_DIR}/bluecove-gpl-${BLUECOVE_VERSION}.jar -C ${CLASSES_DIR} .

echo "Jar file ${TARGET_DIR}/bluecove-gpl-${BLUECOVE_VERSION}.jar created"


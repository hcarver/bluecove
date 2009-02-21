#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
# Created by Francois Kooman
#
# Use this file in case you don't have ant or maven installed on the system
#
# requirements: gcc, javac, javah, bluez headers

BUILD_DIR=`pwd`

BLUECOVE_VERSION=2.1.1-SNAPSHOT

SRC_JAVA_DIR=${BUILD_DIR}/src/main/java
SRC_C_DIR=${BUILD_DIR}/src/main/c
CLASSES_DIR=${BUILD_DIR}/target/classes
OBJ_DIR=${BUILD_DIR}/target/native

BLUECOVE_JAR="${BUILD_DIR}/../bluecove/target/bluecove-${BLUECOVE_VERSION}.jar"
if [[ ! -f ${BLUECOVE_JAR} ]] ; then
  echo "BlueCove jar not found ${BLUECOVE_JAR}"
  exit 1
fi

mkdir -p ${CLASSES_DIR}

echo "=== Compile the bluez stack java files ==="
javac -d ${CLASSES_DIR} -cp ${BLUECOVE_JAR} ${SRC_JAVA_DIR}/com/intel/bluetooth/BluetoothStackBlueZ*.java
if [[ ! "$?" = "0" ]]; then
    echo Error in Java compilation
    exit 1
fi

echo "=== Generate the JNI C header files from these java files ==="
javah -d ${SRC_C_DIR} -classpath ${CLASSES_DIR} com.intel.bluetooth.BluetoothStackBlueZDBus com.intel.bluetooth.BluetoothStackBlueZDBusConsts com.intel.bluetooth.BluetoothStackBlueZDBusNativeTests
if [[ ! "$?" = "0" ]]; then
    echo Error in JNI haders creation
    exit 1
fi

echo "=== Compile the C files ==="
mkdir -p ${OBJ_DIR}
cd ${OBJ_DIR}
gcc -fPIC -c ${SRC_C_DIR}/*.c -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
if [[ ! "$?" = "0" ]]; then
    echo Error in C compilation
    cd ${BUILD_DIR}
    exit 1
fi
cd ${BUILD_DIR}

echo "=== Link object file into the library ==="
gcc -shared -Wl,-soname,libbluecovez-${BLUECOVE_VERSION} -o ${BUILD_DIR}/target/libbluecovez.so ${OBJ_DIR}/*.o
if [[ ! "$?" = "0" ]]; then
    echo Error in linking
    exit 1
fi

# copy the shared library to classes directory
cp ${BUILD_DIR}/target/libbluecovez.so ${CLASSES_DIR}

echo "Native library ${BUILD_DIR}/target/libbluecovez.so created"



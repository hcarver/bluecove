#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
# Created by Francois Kooman
#
# Use this file in case you don't have ant or maven installed on the system

# requirements: gcc, javac, javah, bluez

BLUECOVE_VERSION=2.1.1-SNAPSHOT

mkdir -p target/classes

# compile the bluez stack java files
javac -d target/classes -cp ../bluecove/target/bluecove-${BLUECOVE_VERSION}.jar src/main/java/com/intel/bluetooth/BluetoothStackBlueZ*.java

# generate the C header files from these java files
javah -d src/main/c -classpath src/main/java -d src/main/c/ com.intel.bluetooth.BluetoothStackBlueZ com.intel.bluetooth.BluetoothStackBlueZConsts com.intel.bluetooth.BluetoothStackBlueZNativeTests

# compile the C files
mkdir -p target/native
cd target/native
gcc -fPIC -c ../../src/main/c/*.c ../../src/main/c/*.h
cd ../../

# link them into the library
gcc -shared -lbluetooth -Wl,-soname,libbluecove-${BLUECOVE_VERSION} -o target/libbluecove.so target/native/*.o

# copy the shared library to root
cp target/libbluecove.so target/classes



# @version $Revision$ ($Author$) $Date$
#
# Created by Francois Kooman
#
# Use this file in case you don't have ant or maven installed on the system

BLUECOVE_VERSION=2.1.1-SNAPSHOT
BLUECOVE_JAR=../bluecove/target/bluecove-${BLUECOVE_VERSION}.jar
#JAVA_HOME=/usr
JAVAH=$(JAVA_HOME)/bin/javah
JAVAC=$(JAVA_HOME)/bin/javac
CC=gcc
CFLAGS=-Wall -fPIC # -Werror
# you really need bluez-libs-devel installed (for -lbluetooth)!
CLIBFLAGS=$(CFLAGS) -shared -lbluetooth -Wl,-soname,libbluecove-2.1.0
CSRCDIR=src/main/c
JSRCDIR=src/main/java
JAVACLASSES=com.intel.bluetooth.BluetoothStackBlueZ com.intel.bluetooth.BluetoothStackBlueZConsts com.intel.bluetooth.BluetoothStackBlueZNativeTests
LIBPOSTFIX=`uname -p | grep 64 | sed 's/.*64.*/_x64/g'`

CLASSPATH=$(BLUECOVE_JAR):${JSRCDIR}

all: classes jni-headers native-lib

classes:
	$(JAVAC) -classpath $(CLASSPATH) \
	$(JSRCDIR)/com/intel/bluetooth/*.java

jni-headers:
	$(JAVAH) -d $(CSRCDIR) \
	-classpath $(CLASSPATH) \
	$(JAVACLASSES)

native-lib:
	mkdir -p target/native
	cd target/native
	$(CC) $(CFLAGS) -c $(CSRCDIR)/*.c ${CSRCDIR}/*.h
	$(CC) $(CLIBFLAGS) -o target/libbluecove$(LIBPOSTFIX).so *.o
	strip target/libbluecove$(LIBPOSTFIX).so

clean:
	rm -rf target/
	rm *.o


# @version $Revision$ ($Author$) $Date$
#
# Created by Francois Kooman
#
# Use this file in case you don't have ant or maven installed on the system
# Usage: make all
#

BLUECOVE_VERSION=2.1.1-SNAPSHOT

BLUECOVE_JAR=../bluecove/target/bluecove-${BLUECOVE_VERSION}.jar
JAVAH=$(JAVA_HOME)/bin/javah
JAVAC=$(JAVA_HOME)/bin/javac
JAVAC_OPTIONS=-g -Xlint:unchecked -source 1.3 -target 1.1
CC=gcc
CFLAGS=-Wall -fPIC # -Werror
# you really need bluez-libs-devel installed (for -lbluetooth)!
# -nodefaultlibs ->  statically linked
CLIBFLAGS=$(CFLAGS) -nodefaultlibs -shared -lbluetooth -Wl,-soname,libbluecove-$(BLUECOVE_VERSION)
SRC_C_DIR=src/main/c
SRC_JAVA_DIR=src/main/java
CLASSES_DIR=target/classes
OBJ_DIR=target/native
JAVACLASSES=com.intel.bluetooth.BluetoothStackBlueZ com.intel.bluetooth.BluetoothStackBlueZConsts com.intel.bluetooth.BluetoothStackBlueZNativeTests
LIBPOSTFIX=`uname -p | grep 64 | sed 's/.*64.*/_x64/g'`

TARGET_LIB=target/libbluecove$(LIBPOSTFIX).so

CLASSPATH=$(BLUECOVE_JAR)

all: classes jni-headers native-lib

classes:
	-@mkdir -p $(CLASSES_DIR)
	-@$(JAVAC) -d $(CLASSES_DIR) $(JAVAC_OPTIONS) -classpath $(CLASSPATH) \
	$(SRC_JAVA_DIR)/com/intel/bluetooth/*.java

jni-headers:
	-@$(JAVAH) -d $(SRC_C_DIR) \
	-classpath $(CLASSPATH):$(CLASSES_DIR) \
	$(JAVACLASSES)

native-lib:
	-@mkdir -p $(OBJ_DIR)
	-@cd $(OBJ_DIR) && \
	$(CC) $(CFLAGS) -c ../../$(SRC_C_DIR)/*.c -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
	-@$(CC) $(CLIBFLAGS) -o $(TARGET_LIB) $(OBJ_DIR)/*.o
	-@strip $(TARGET_LIB)
	-@cp $(TARGET_LIB) $(CLASSES_DIR)/libbluecove$(LIBPOSTFIX).so
	-@echo "Native library $(TARGET_LIB) created"
	-@echo "Shared library dependencies:"
	-@ldd -v $(TARGET_LIB)

clean:
	rm -rf target


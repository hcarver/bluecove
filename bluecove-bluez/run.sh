#!/bin/sh --

JAVA=$JAVA_HOME/bin/java
JAVAUNIXLIBPATH=/home/vlads/dbus/dbus-java-2.3.1
JAVAUNIXJARPATH=/home/vlads/dbus/dbus-java-2.3.1
JARPATH=/home/vlads/dbus/dbus-java-2.3.1
DEBUG=disable

TESTS_PATH=/home/vlads/dbus/bluecove-bluez/target/classes

$JAVA -Djava.library.path=$JAVAUNIXLIBPATH -cp $JAVAUNIXJARPATH/unix.jar:$JAVAUNIXJARPATH/debug-$DEBUG.jar:$JAVAUNIXJARPATH/hexdump.jar:$JARPATH/dbus.jar:$TESTS_PATH ManagerTest

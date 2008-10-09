#! /bin/sh
#  @version $Revision$ ($Author$) $Date$
#
unset JVM_ARGS
LD_LIBRARY_PATH=/usr/lib/:${LD_LIBRARY_PATH}
export LD_LIBRARY_PATH
# JVM_ARGS=%JVM_ARGS%

java -cp /usr/share/java/idev_bluez.jar:junit-3.8.1.jar:bluecove-tester-2.0.1-SNAPSHOT.jar net.sf.bluecove.awt.Main

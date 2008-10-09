
set CP=D:/di/me2/bluecove/3p/TCK/gatling-1.0.05/plugins/com.motorola.test.tckui_0.1.0/gatling_core/lib/j2meunit.jar
set CP=%CP%;D:/di/me2/bluecove/3p/TCK/gatling-1.0.05/plugins/com.motorola.test.tckui_0.1.0/gatling_core/lib/client-core.jar
set CP=%CP%;D:\di\me2\bluecove\bluecove\target\bluecove-2.0.3-SNAPSHOT.jar

%WTK_HOME%\bin\preverify -target CLDC1.1 -classpath target\classes;%CP%;%WTK_HOME%\lib\cldcapi11.jar;%WTK_HOME%\lib\midpapi20.jar;%WTK_HOME%\wtklib\kvem.jar -d target\preverify target\classes

pause
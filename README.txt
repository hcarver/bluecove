#summary BlueCove documentation.
#labels Featured

= BlueCove =

== About ==

BlueCove is a LGPL licensed JSR-82 implementation on Java Standard Edition (J2SE) that currently interfaces with
the Microsoft Bluetooth stack. Originally developed by Intel Research and
currently maintained by volunteers.

== Requirements ==

  * Microsoft Bluetooth stack (currently this means Windows XP SP2 or newer and Windows Mobile 2003 or newer)
  * A Bluetooth device supported by the Microsoft bluetooth stack
  * Java 1.1 or newer for the binary execution, Java 1.4 or newer to compile.
  * Another Bluetooth device to communicate with

== Limitations ==

Due to the Microsoft Bluetooth stack only supporting RFCOMM connections,
BlueCove also only supports RFCOMM connections. The operating system support is
currently limited to Windows XP SP2 and newer, because the Microsoft Bluetooth
stack is not available on other operating systems. If someone writes code to
support another stack and/or operating system, it will be considered for
inclusion.  BlueCove does also not support OBEX, but there are other projects
that can (possibly) be used to achieve OBEX functionality with BlueCove.

== Not Implemented functionality ===

  * `DiscoveryAgent.cancelServiceSearch(..)`  Not implemented
  * `DiscoveryAgent.selectService(..)`  Not implemented
  * `RemoteDevice` authenticate, authorize and encrypt Not implemented

== Installation ==

Installation of the binary (already compiled) version of BlueCove is as follows:

  # [http://code.google.com/p/bluecove/downloads/list Download BlueCove] binary release
  # Unzip the archive
  # Add `bluecove.jar` to your classpath

For maven2 users see [maven2 Using maven2 to build application or MIDlet]

== Runtime configuration ==

Native Library location

  # By default Native Library is extracted from from jar to temporary directory `${java.io.tmpdir}/bluecove_${user.name}_N` and loaded from this location.
  # If you wish to load library (.dll) from another location add this system property `-Dbluecove.native.path=/your/path`.
  # If you wish to load library from default location in path e.g. `%SystemRoot%\system32` or any other location in %PATH% use `-Dbluecove.native.resource=false`

IBM J9

    To run BlueCove with [http://www.ibm.com/software/wireless/weme/ IBMs J9] Java VM on Win32 or PocketPC add this system property `-Dmicroedition.connection.pkgs=com.intel.bluetooth`.

Debug

    If something goes wrong system property `-Dbluecove.debug=true` will enable debug prints in BlueCove code

== Compilation ==

You need a C++ compiler and JDK. Tested on Visual C++ 2005 Express Edition SP1 and SDK for Windows Vista or Windows Server 2003 R2 Platform SDK.
 VC++ and Windows SDK are available for free download from microsoft.com.
 We are using for Windows Vista SDK for binary distribution:
    Make sure you have
     Tools ->  Options -> VC++ Directories ->
        "Include files" %ProgramFiles%\Microsoft SDKs\Windows\v6.0\Include
        "Library files" %ProgramFiles%\Microsoft SDKs\Windows\v6.0\lib

 Ant or maven2 are used as the build tool for java.


  # [http://code.google.com/p/bluecove/downloads/list Download BlueCove]source release
  # Unzip the source
  # Run `ant` or `mvn`
  # Go into `src\main\c\intelbth`
  # Open `intelbth.sln`
  # Compile the project for your platform (e.g. 'Winsock' for 'Win32')
  # Run `ant jar` or `mvn`

== Source ==

Available as downloadable packages or at the Subversion repository. Organized in:

  * *`src\main\c\intelbth`* - The native windows JNI dll
  * *`src\main\java`* - The implementation of JSR-82 with calls to intelbth
  * *`src\test\java`* - Some test programs

# @version $Revision$ ($Author$) $Date$

This is the custom build native libraries for released and SNAPSHOT versions.

    Currently the configurations that are supported but not frequently used (and tested) can be found here:
        win32-cdc         - For SUN CDC when running on Windows 32 XP/Vista          (native functions exported using __cdecl calling convention)
        vista-broadcom-6  - For L2CAP support using WIDCOMM stack v6 on M$ Vista     (compiled using BTW-6_1_0_1501-SDK)
    Since 2.1    
        broadcom-5        - For dual stack support on Windows XP, WIDCOMM stack v5.1 (compiled using BTW-5_1_0_3101-SDK)

If required the standard native libraries for released versions should extracted from distribution bluecove-$VERSION$.jar

Ideally the libraries in this directory should be produced by bluecove build process running on Windows XP/Mac OS X Leo/Linux Fedora in VMware Server on the main pyx4j.com Linux build server.

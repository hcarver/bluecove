========================================================================
    DYNAMIC LINK LIBRARY : intelbth.dll and bluecove.dll
========================================================================


intelbth.vcproj
    This is the project file for VC 2005 project.

    There are four Configurations:  Winsock, WIDCOMM, BlueSoleil, All and Release

    Release Configuration on Win32 will incorporated Winsock and BlueSoleil Configurations and should be used for distribution!

    N.B. We can't use the same DLL on windows for all implemenations. Since WIDCOMM need to be compile /MD using VC6 and winsock and BlueSoleil /MT using VC2005
        intelbth.dll build by VC2005 Configuration "Win32 winsock"
        bluecove.dll build by VC6 Configuration "Win32 Release"

    Release on Windows Mobile will incorporated Winsock and WIDCOMM Configurations

    "Pocket PC 2003 (ARMV4)" is Platform we use to build DLL for WindowsCE

    There are three working Configurations:  Winsock, WIDCOMM and Release  TODO

    A precompiled header (StdAfx.h, StdAfx.cpp) can't be used because BlueSoleil redefine some Microsoft definitions from BluetoothAPIs.h

    Runtime Library Multi-threaded static (/MT) used for intelbth.dll to avoid dependancy on MSVCR80.DLL

    Runtime Library Multi-threaded DLL (/MD) used for bluecove.dll and build by VC6.


WIDCOMM:  bluecove.dll
    Get Broadcom development kits from:  http://www.broadcom.com/products/bluetooth_sdk.php
    You have to register at the Broadcom site to gain access to the downloads.
    We are using BTW-5_0_1_902-SDK!
    Install it to default directory. e.g. "$(ProgramFiles)\Widcomm\BTW DK" for Win32
    Since wbtapi.dll does not come with BTW-5_0_1_902-SDK you need to copy this DLL from "%systemroot%\system32" to "$(ProgramFiles)\Widcomm\BTW DK\SDK\Release"
    Make all the files read only if you will ever uninstall WIDCOMM drivers the SDK files should remain.

    We tested BTW DK - BTW-5_1_0_3101 and we can't make it run properly with Broadcom drivers BTW 4.0.x and 5.0.x.
    Also BTW-5_1_0_3101 needs btwapi.dll that is not installed with all version of WIDCOMM drivers. So we use BTW-5_0_1_902-SDK

    You don't need to have bluetooth WIDCOMM drivers installed to build the dll.

BlueSoleil:  intelbth.dll
    Get BlueSoleil™ PC Platform Software Development Kit (SDK), 0.83 free version from this location:
    http://www.bluesoleil.com/download/index.asp?topic=bluesoleil_sdk

    The BlueSoleil API should be installed in directory: $(ProgramFiles)\IVT Corporation\BlueSoleil\api
    Make all the files read only if you will ever uninstall BlueSoleil the SDK files should remain.

    Four files should be there: a DLL file btfunc.dll, a library file btfunc.lib and two header file bt_ui.h, bt_def.h.

    You don't need to have bluetooth BlueSoleil drivers installed to build the dll.

common.h
    Used instead of StdAfx.h

common.cpp
    This is the main DLL source file.

intelbth.cpp
    This is the source file for Winsock Stack.

WIDCOMMStack.cpp and WIDCOMMStack.h
    This is the source file for WIDCOMM Stack.

BlueSoleilStack.cpp and BlueSoleilStack.h
    This is the source file for BlueSoleil Stack.

/////////////////////////////////////////////////////////////////////////////
Other notes:

/////////////////////////////////////////////////////////////////////////////

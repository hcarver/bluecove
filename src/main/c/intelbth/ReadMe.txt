========================================================================
    DYNAMIC LINK LIBRARY : intelbth Project Overview
========================================================================


intelbth.vcproj
    This is the main project file for VC++ project.

    There are four Configurations:  Winsock, WIDCOMM, BlueSoleil and Release

    Release on Win32 will incorporated  Winsock, WIDCOMM and BlueSoleil Configurations

    Release on Windows Mobile will incorporated  Winsock and WIDCOMM Configurations

    A precompiled header (StdAfx.h, StdAfx.cpp) can't be used because BlueSoleil redefine some Microsoft definitions from BluetoothAPIs.h

WIDCOMM:
    Get Broadcom development kits from:  http://www.broadcom.com/products/bluetooth_sdk.php
    You have to register at the Broadcom site to gain access to the downloads.
    We are using BTW-5_0_1_902-SDK!
    Install it to default directory. e.g. "$(ProgramFiles)\Widcomm\BTW DK" for Win32
    Since wbtapi.dll does not come with BTW-5_0_1_902-SDK you need to copy this DLL from "%systemroot%\system32" to "$(ProgramFiles)\Widcomm\BTW DK\SDK\Release"

    We tested BTW DK - BTW-5_1_0_3101 and we can't make it run properly with Broadcom drivers BTW 4.0.x and 5.0.x

BlueSoleil:
    Get BlueSoleil™ PC Platform Software Development Kit (SDK), 0.83 free version from this location:
    http://www.bluesoleil.com/download/index.asp?topic=bluesoleil_sdk

    The BlueSoleil API should be installed in directory: $(ProgramFiles)\IVT Corporation\BlueSoleil\api

    Four files should be there: a DLL file btfunc.dll, a library file btfunc.lib and two header file bt_ui.h, bt_def.h.

common.cpp
    This is the main DLL source file.

intelbth.cpp
    This is the source file for Winsock Stack.

WIDCOMMStack.cpp
    This is the source file for WIDCOMM Stack.

BlueSoleilStack.cpp
    This is the source file for BlueSoleil Stack.

/////////////////////////////////////////////////////////////////////////////
Other notes:

AppWizard uses "TODO:" comments to indicate parts of the source code you
should add to or customize.

/////////////////////////////////////////////////////////////////////////////

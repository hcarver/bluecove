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
    Install it to default directory. e.g. "$(ProgramFiles)\Widcomm\BTW DK" for Win32

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

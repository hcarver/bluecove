# Microsoft Developer Studio Project File - Name="bluecove" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=BLUECOVE - WIN32 RELEASE
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "bluecove.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "bluecove.mak" CFG="BLUECOVE - WIN32 RELEASE"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "bluecove - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe
# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Win32\bluecove"
# PROP Intermediate_Dir "Win32\bluecove"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "BLUECOVE_EXPORTS" /YX /FD /c
# ADD CPP /nologo /MD /W3 /GX /O2 /I "$(JAVA_HOME)\include" /I "$(JAVA_HOME)\include\win32" /I "$(ProgramFiles)\Widcomm\BTW DK\SDK\Inc" /I "$(ProgramFiles)\IVT Corporation\BlueSoleil\api" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "UNICODE" /D "BLUECOVE_EXPORTS" /D "_BTWLIB" /FR /FD /c
# SUBTRACT CPP /YX
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x1009 /d "NDEBUG"
# ADD RSC /l 0x1009 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 ws2_32.lib version.lib advapi32.lib user32.lib winspool.lib ole32.lib /nologo /dll /machine:I386 /out:"..\..\resources\bluecove.dll" /libpath:"$(ProgramFiles)\Widcomm\BTW DK\SDK\Release" /libpath:"$(ProgramFiles)\IVT Corporation\BlueSoleil\api"
# SUBTRACT LINK32 /pdb:none /incremental:yes
# Begin Target

# Name "bluecove - Win32 Release"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\BlueSoleilStack.cpp
# End Source File
# Begin Source File

SOURCE=..\..\..\..\target\classes\com\intel\bluetooth\BluetoothStackWIDCOMM.class
# Begin Custom Build
InputPath=..\..\..\..\target\classes\com\intel\bluetooth\BluetoothStackWIDCOMM.class

".\com_intel_bluetooth_BluetoothStackWIDCOMM.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	javah -jni -classpath ..\..\..\..\target\classes com.intel.bluetooth.BluetoothStackWIDCOMM

# End Custom Build
# End Source File
# Begin Source File

SOURCE=.\common.cpp
# End Source File
# Begin Source File

SOURCE=.\commonTest.cpp
# End Source File
# Begin Source File

SOURCE=.\WIDCOMMStack.cpp
# End Source File
# Begin Source File

SOURCE=.\WIDCOMMStackL2CAP.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\BlueSoleilStack.h
# End Source File
# Begin Source File

SOURCE=.\com_intel_bluetooth_BluetoothStackBlueSoleil.h
# End Source File
# Begin Source File

SOURCE=.\com_intel_bluetooth_BluetoothStackWIDCOMM.h
# End Source File
# Begin Source File

SOURCE=.\com_intel_bluetooth_NativeTestInterfaces.h
# End Source File
# Begin Source File

SOURCE=.\common.h
# End Source File
# Begin Source File

SOURCE=.\WIDCOMMStack.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# Begin Source File

SOURCE=.\bluecove.rc
# End Source File
# End Group
# End Target
# End Project

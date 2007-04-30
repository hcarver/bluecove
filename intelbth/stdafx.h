// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
#pragma once

#ifdef _WIN32_WCE
#pragma comment(linker, "/nodefaultlib:libc.lib")
#pragma comment(linker, "/nodefaultlib:libcd.lib")

// NOTE - this value is not strongly correlated to the Windows CE OS version being targeted
#define WINVER _WIN32_WCE

#include <ceconfig.h>
#if defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)
#define SHELL_AYGSHELL
#endif // (WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)

#ifdef _CE_DCOM
#define _ATL_APARTMENT_THREADED
#endif // _CE_DCOM

#ifdef SHELL_AYGSHELL
#include <aygshell.h>
#pragma comment(lib, "aygshell.lib") 
#endif // SHELL_AYGSHELL


// Windows Header Files:
#include <windows.h>


#if defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)
#ifndef _DEVICE_RESOLUTION_AWARE
#define _DEVICE_RESOLUTION_AWARE
#endif // _DEVICE_RESOLUTION_AWARE
#endif //(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP)

#ifdef _DEVICE_RESOLUTION_AWARE
#include "DeviceResolutionAware.h"
#endif // _DEVICE_RESOLUTION_AWARE

#if _WIN32_WCE < 0x500 && ( defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP) )
	#pragma comment(lib, "ccrtrtti.lib")
	#ifdef _X86_	
		#if defined(_DEBUG)
			#pragma comment(lib, "libcmtx86d.lib")
		#else
			#pragma comment(lib, "libcmtx86.lib")
		#endif
	#endif
#endif // _WIN32_WCE < 0x500 && ( defined(WIN32_PLATFORM_PSPC) || defined(WIN32_PLATFORM_WFSP) )

#include <altcecrt.h>

#include <winsock2.h>
#include <ws2bth.h>
#include <bthapi.h>
#include <bt_api.h>
#include <bthutil.h>
#include <bt_sdp.h>

#else // _WIN32_WCE

#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers
// Windows Header Files:
#include <windows.h>
#include <winsock2.h>
#include <ws2bth.h>
#include <BluetoothAPIs.h>
#include <tchar.h>
#include <strsafe.h>

#endif // #else // _WIN32_WCE

// TODO: reference additional headers your program requires here
#include <stdlib.h>

#include "com_intel_bluetooth_BluetoothPeer.h"


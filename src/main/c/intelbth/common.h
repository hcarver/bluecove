/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */

#define INQUIRY_COMPLETED 0
#define INQUIRY_TERMINATED 5
#define INQUIRY_ERROR 7

#define SERVICE_SEARCH_COMPLETED 1
#define SERVICE_SEARCH_TERMINATED 2
#define SERVICE_SEARCH_ERROR 3
#define SERVICE_SEARCH_NO_RECORDS 4
#define SERVICE_SEARCH_DEVICE_NOT_REACHABLE 6

void callDebugListener(JNIEnv *env, const char* fileName, int lineN, const char *fmt, ...);
#define debug(fmt) callDebugListener(env, __FILE__, __LINE__, fmt);
#define debugs(fmt, message) callDebugListener(env, __FILE__, __LINE__, fmt, message);
#define debugss(fmt, message1, message2) callDebugListener(env, __FILE__, __LINE__, fmt, message1, message2);

void throwException(JNIEnv *env, const char *name, const char *msg);

void throwIOException(JNIEnv *env, const char *msg);

WCHAR *GetWSAErrorMessage(DWORD last_error);

void throwExceptionWSAErrorMessage(JNIEnv *env, const char *name, const char *msg, DWORD last_error);

void throwIOExceptionWSAErrorMessage(JNIEnv *env, const char *msg, DWORD last_error);

void throwIOExceptionWSAGetLastError(JNIEnv *env, const char *msg);

BOOL ExceptionCheckCompatible(JNIEnv *env);

void convertBytesToUUID(jbyte *bytes, GUID *uuid);

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
package com.intel.bluetooth.obex;

/**
 * See <a
 * href="http://bluetooth.com/Bluetooth/Learn/Technology/Specifications/">Bluetooth
 * Specification Documents</A> for details.
 * 
 * 
 * @author vlads
 * 
 */
public interface OBEXOperationCodes {

	public static final byte OBEX_VERSION = 0x10;  /* OBEX Protocol Version 1.1 */
	
	public static final short OBEX_DEFAULT_MTU = 0x400;
	
	public static final char FINAL_BIT = 0x80;
	
	public static final char CONNECT = 0x00;

	public static final char DISCONNECT = 0x01;

	public static final char PUT = 0x02;

	public static final char GET = 0x03;

	public static final char SETPATH = 0x05;

	public static final char SESSION = 0x07;

	public static final char ABORT = 0x7F;

}

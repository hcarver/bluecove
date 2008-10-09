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
package net.sf.bluecove;

/**
 * @author vlads
 * 
 */
public class CommunicationData implements Consts {

	public static final String stringData = "TestString2007";

	public static final String stringUTFData = "\u0413\u043E\u043B\u0443\u0431\u043E\u0439\u0417\u0443\u0431";

	public static final int byteCount = 12; // 1024;

	public static final byte[] byteAray = new byte[] { 1, 7, -40, 80, 90, -1, 126, 100, 87, -10, 127, 31, -127, 0, -77 };

	public static final byte streamAvailableByteCount = 126;

	public static final int byteAray8KPlusSize = 0x2010; // More then 8K

	public static final int byteAray64KPlusSize = 0x10100; // More then 64K

	public static final int byteAray128KSize = 0x20000; // 64K

	public static final byte aKnowndPositiveByte = 21;

	public static final byte aKnowndNegativeByte = -33;

}

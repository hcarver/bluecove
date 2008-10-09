/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex;

import java.util.Hashtable;

public class ObexTypes {

	private static Hashtable types = new Hashtable();

	static {
		types.put("jpg", "image/jpeg");
		types.put("jpeg", "image/jpeg");
		types.put("gif", "image/gif");
		types.put("mp3", "audio/mpeg");
		types.put("txt", "text/plain");
		types.put("jar", "application/java-archive");
	}

	static String getFileExtension(String fileName) {
		int extEnd = fileName.lastIndexOf('.');
		if (extEnd == -1) {
			return "";
		} else {
			return fileName.substring(extEnd + 1).toLowerCase();
		}
	}

	static String getObexFileType(String fileName) {
		return (String) types.get(getFileExtension(fileName));
	}
}

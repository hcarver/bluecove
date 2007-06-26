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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.obex.HeaderSet;

public class OBEXHeaderSetImpl implements HeaderSet {

	private int responseCode;
	
	private Hashtable headerValues;

	OBEXHeaderSetImpl() {
		this(Integer.MIN_VALUE);
	}

	private OBEXHeaderSetImpl(int responseCode) {
		headerValues = new Hashtable();
		this.responseCode = responseCode;
	}
	
	public void setHeader(int headerID, Object headerValue) {
		if (headerValue == null) {
			headerValues.remove(new Integer(headerID));
		} else {
			headerValues.put(new Integer(headerID), headerValue);
		}
	}

	public Object getHeader(int headerID) throws IOException {
		return headerValues.get(new Integer(headerID));
	}

	public int[] getHeaderList() throws IOException {
		int[] headerIDArray = new int[headerValues.size()];
		int i = 0;
		for (Enumeration e = headerValues.keys(); e.hasMoreElements();) {
			headerIDArray[i++] = ((Integer) e.nextElement()).intValue();
		}
		return headerIDArray;
	}

	public int getResponseCode() throws IOException {
		if (this.responseCode == Integer.MIN_VALUE) {
			throw new IOException();
		}
		return this.responseCode;
	}

	public void createAuthenticationChallenge(String realm, boolean userID, boolean access) {
		// TODO Auto-generated method stub

	}
	
	static byte[] toByteArray(HeaderSet headers) throws IOException {
		if (headers == null) {
			return new byte[0];
		}
		
		return new byte[0];
	}
	
	static HeaderSet read(byte responseCode, byte[] buf, int off) throws IOException {
		HeaderSet hs = new OBEXHeaderSetImpl((int)(responseCode));
		
		return hs;
	}

}

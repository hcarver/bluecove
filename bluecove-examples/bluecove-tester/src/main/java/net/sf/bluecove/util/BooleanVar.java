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

package net.sf.bluecove.util;

/**
 * Calss use instead of boolean since there are no reflection in J2ME. 
 * @author vlads
 */
public class BooleanVar {
	
	private boolean value;
	
	public BooleanVar() {
		this.value = false;
	}
	
	public BooleanVar(boolean value) {
		this.value = value;
	}
	
	public void setValue(boolean value) {
		this.value = value;
	}
	
	public boolean booleanValue() {
		return this.value;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof BooleanVar) {
			return ((BooleanVar)obj).value = this.value;
		} else {
			return false;
		}
	}
	
	public String toString() {
		return (new Boolean(value)).toString();
	}
}

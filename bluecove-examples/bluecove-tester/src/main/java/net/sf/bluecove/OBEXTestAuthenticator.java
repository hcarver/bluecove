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

import javax.obex.Authenticator;
import javax.obex.PasswordAuthentication;

/**
 * @author vlads
 *
 */
public class OBEXTestAuthenticator implements Authenticator {

	String userName;
	
	public OBEXTestAuthenticator(String userName) {
		this.userName = userName;
	}
	
	public PasswordAuthentication onAuthenticationChallenge(String description, boolean isUserIdRequired, boolean isFullAccess) {
		Logger.debug("challenge " + (isUserIdRequired?"U":"") + (isFullAccess?"A":"") + " " + description);
		return new PasswordAuthentication(userName.getBytes(), (new String("password")).getBytes());
	}

	public byte[] onAuthenticationResponse(byte[] userName) {
		Logger.debug("authenticate " + new String(userName));
		return (new String("password")).getBytes();
	}

}

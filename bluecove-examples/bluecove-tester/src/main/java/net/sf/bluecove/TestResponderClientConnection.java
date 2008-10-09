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

import java.io.IOException;

import javax.microedition.io.Connection;

/**
 * @author vlads
 * 
 */
public abstract class TestResponderClientConnection {

	public abstract String protocolID();

	public abstract ConnectionHolder connected(Connection conn) throws IOException;

	public abstract void executeTest(int testType, TestStatus testStatus) throws IOException;

	public abstract void replySuccess(String logPrefix, int testType, TestStatus testStatus) throws IOException;

	public abstract void sendStopServerCmd(String serverURL);
}

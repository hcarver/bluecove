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
package com.ibm.oti.connection.tcpobex;


/**
 * This class is Proxy for tcpobex (OBEX over TCP) Connection implementations for IBM J9 support.
 * <p>
 * No need to configure -Dmicroedition.connection.pkgs=com.intel.bluetooth when bluecove.jar installed to "%J9_HOME%\lib\jclMidp20\ext\
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 * @author vlads
 *
 */
public class Connection extends com.intel.bluetooth.tcpobex.Connection {

}

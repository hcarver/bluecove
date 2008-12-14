/*
 *  $HeadURL$
 *
 *
 *  Copyright (c) 2001-2008 Motorola, Inc.  All rights reserved. 
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *        
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  
 *  Revision History:
 *
 *  Date             Author                   Comment
 *  ---------------------------------------------------------------------------------
 *  Oct 15,2006      Motorola, Inc.           Initial creation        
 *
 */


package OBEXTCKAgent;


/**
 * This class is used to intialize the transport protocol to begin
 * testing.
 */
public class HelperUtil {
	public static final String TIMEOUT = "timeout";
	public static final String TRANSPORT = "transport";
	
	// supported transport protocol
	public static final String BT = "btgoep";
	public static final String TCP = "tcpobex";
	public static final String IRDA = "irdaobex";

    /**
     * Initializes the bluetooth transport protocol to begin the testing of the
     * OBEX API.
     */
    public static void initialize() {

        javax.bluetooth.LocalDevice local = null;
        try {
            local = javax.bluetooth.LocalDevice.getLocalDevice();

            local.setDiscoverable(javax.bluetooth.DiscoveryAgent.GIAC);
        } catch (Exception ex) {
            System.out.println("Exception on initialization : " +
                ex.getClass().getName() + " " + ex.getMessage());
        }
    }

}

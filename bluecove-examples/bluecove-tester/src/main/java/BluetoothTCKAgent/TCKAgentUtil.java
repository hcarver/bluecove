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


package BluetoothTCKAgent;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class TCKAgentUtil {

    public static int SHORT = 2000;
    public static int MEDIUM = 5000;
    public static int LONG = 10000;

	private static RemoteDevice rDev = null;
	private static final int NO_SECURITY	
        = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
    public static void pause(int delay) {
        try {
            Thread.sleep(delay);
        } catch (Exception e) {
        }
    }

    public static int getServiceClass(String btAddress) {
        LocalDevice device;
        DiscoveryAgent da;
        DiscoveryListenerImpl listen;
        DeviceClass deviceClass;

        try {
             Boolean synch = new Boolean(true);

            listen = new DiscoveryListenerImpl(synch, btAddress);

            device = LocalDevice.getLocalDevice();
            da = device.getDiscoveryAgent();

            if (!da.startInquiry(DiscoveryAgent.GIAC, listen)) {
                return -1;
            }

              synchronized (synch) {
                try {
                    synch.wait(60000);
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            }
        } catch (Throwable th) {
            System.out.println("Error : "  + th);
            return -1;
        }

        if(listen.getType() != 
                DiscoveryListener.INQUIRY_COMPLETED) {            
            return -1;
        }

        deviceClass = listen.getDeviceClass();

        if(deviceClass != null) {
            return deviceClass.getServiceClasses();
        }

        return -1;
    }

    /*
     * Starts an inquiry to find the remoteDevice with the address
     * passed in as a parameter.
     */
    public static RemoteDevice getRemoteDevice(String btAddress) {
        LocalDevice device;
        boolean result = false;
        DiscoveryListenerImpl listen = null;
        DiscoveryAgent da;
        String addr;
        int count = 0;
        Boolean synch;

        if (rDev != null) {
            btAddress = (btAddress.trim()).toUpperCase();
            addr = rDev.getBluetoothAddress();
            addr = (addr.trim()).toUpperCase();

            if (btAddress.equals(addr) ) {
                return rDev;
            }
        }

        do {
            try {
                synch = new Boolean(true);
                listen = new DiscoveryListenerImpl(synch, btAddress);

                device = LocalDevice.getLocalDevice();
                da = device.getDiscoveryAgent();

                synchronized (synch) {
                    result = da.startInquiry(DiscoveryAgent.GIAC, listen);

                    try {
                        synch.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            } catch (Throwable th) {
                System.out.println("Error : "  + th);
            }

            rDev = listen.getRemoteDevice();

        } while ((rDev == null) && (count++ < 5));

        return rDev;
    }

    /*
     * This method starts an serviceSearch and returns the array of
     * serviceRecords it finds. It takes a bluetooth address and a UUID
     * array as the input parameters.
     */
    public static ServiceRecord [] getServiceRecords(String btAddress,
                                                      UUID [] uArr) {
        DiscoveryListenerImpl listen;
        RemoteDevice dev = null;
        DiscoveryAgent da;
        LocalDevice device;
        ServiceRecord [] records = null;
        int transID = 0;
        int count = 0;

        try {
            Boolean synch = new Boolean(true);
            listen = new DiscoveryListenerImpl(synch);
            device = LocalDevice.getLocalDevice();

            dev = getRemoteDevice(btAddress);

            if (dev == null) {
                System.out.println("TCKAgentUtil : ERROR. Unable to" +
                                   " retrieve Remote BT Device.");
                return null;
            }

            da = device.getDiscoveryAgent();
            synchronized (synch) {
                while ((records == null) && (count < 4)) {
                    transID = da.searchServices(null, uArr, dev, listen);
                    for (int k = 0; k < uArr.length; k++) {
                        System.out.println(k + " UUID is " + uArr[k]);
                    }

                    count++;

                    try {
                        System.out.println("Before wait: " + synch);
                        synch.wait();
                        System.out.println("After wait: " + synch);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    records = listen.getRecordArray();
                }
            }

        } catch (Throwable th) {
            System.out.println("TCKAgentUtil : ERROR. Unable to " +
                               "retrieve Remote Service Records.");
            return null;
        }

        System.out.println("TCKAgentUtil.getServiceRecords(): Returning " + records);
        return records;
    }

    /*
     * Utility method that returns one of the service records registered
     * by the BluetoothTCKAgent on the device with Bluetooth address
     * btAddress. The service record returned depends on the uuid argument,
     * which is the 16-bit UUID defined in the Bluetooth assigned numbers
     * for one of the protocols L2CAP, RFCOM or OBEX.  The service record
     * returned describes how to connect to the agent using this protocol.
     * Returns null if the service record is not found or if uuid is not
     * one of 0x0003, 0x0100, or 0x0008.
     */

    public static ServiceRecord getServiceRecord(String btAddress, int uuid) {

        UUID [] uArr = new UUID[1];
        ServiceRecord [] records = null;
        final int RFCOMM_UUID=0x0003, L2CAP_UUID=0x0100, OBEX_UUID=0x0008;
        final String L2CAP_AGENT_SERVICE_SRVCLASS_ID
            = "3B9FA89520078C303355AAA694238F07";
        final String RFCOMM_AGENT_SERVICE_SRVCLASS_ID
            = "2000000031b811d88698000874b33fc0";
        final String BTGOEP_AGENT_SERVICE_SRVCLASS_ID
            = "3000000031b811d88698000874b33fc0";

        switch (uuid) {
            case L2CAP_UUID:
                uArr[0]
                    = new UUID(L2CAP_AGENT_SERVICE_SRVCLASS_ID, false);
                break;

            case RFCOMM_UUID:
                uArr[0]
                    = new UUID(RFCOMM_AGENT_SERVICE_SRVCLASS_ID, false);
                break;

            case OBEX_UUID:
                uArr[0]
                    = new UUID(BTGOEP_AGENT_SERVICE_SRVCLASS_ID, false);
                break;

            default:
                return null;
        }

        records = getServiceRecords(btAddress, uArr);
        pause(SHORT);

        if (records == null) {
            return null;
        }

        return records[0];
    }

    /*
     * Utility method that returns the connection URL for one of the
     * services offered by the BluetoothTCKAgent on the device btAddress.
     * The uuid is one of the three 16-bit UUIDs recognized by the
     * getServiceRecord(btAddress, uuid) method.  Returns null if the
     * the service record can't be found or if getConnectionURL()
     * cannot create a connection string for the service record.
     */

    public static String getURL(String btAddress, int uuid) {

        ServiceRecord record = null;
        String url = null;

        record = getServiceRecord(btAddress, uuid);

        if (record == null) {
            System.out.println("Record is null");
            return null;
        }

        url = record.getConnectionURL(NO_SECURITY, false);
        return url;
    }

} /* end of class TCKAgentUtil*/

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

/*
 * This is the DiscoveryListener implementation for the bluetooth TCK
 * tests. It utilizes public variables that are set when the
 * listener methods are called.  This enables testing to ensure the
 * methods are reached.  It makes parameters such as the
 * ServiceRecord[] and DeviceClass available for testing. It also
 * allows the test to synchronize with it in the
 * serviceSearchComplete() method.  This avoids the unnecessary
 * waiting in the test.
 */
import java.lang.*;
import javax.bluetooth.*;
import java.util.*;

public class DiscoveryListenerImpl implements DiscoveryListener {

    /*
     * Public variable declarations
     */
    Boolean finished;
    private boolean done;
    private int type = -1;
    private int code;
    private ServiceRecord[] sRecord;
    public boolean calledDeviceDiscovered = false;
    public boolean calledServiceSearchCompleted = false;
    public boolean reached = false;
    public Hashtable idList = new Hashtable();
    public Hashtable discList = new Hashtable();

    RemoteDevice remoteDev = null;
    String btAddress = null;
    String server;
    DeviceClass deviceClass = null;

    /*
     * Constructor
     */
    public DiscoveryListenerImpl() {
        done = false;
    }

    public DiscoveryListenerImpl(Boolean syn) {
        finished = syn;
        done = false;
    }

    public DiscoveryListenerImpl(Boolean syn, String btAdr) {
        finished = syn;
        btAddress = btAdr.toUpperCase();
        System.out.println("DiscoveryListenerImpl(): Searching for " + btAddress);
    }

    /*
     * Methods
     */

    public void deviceDiscovered(RemoteDevice btDevice,
                                 DeviceClass sdClass) {
        calledDeviceDiscovered = true;

        if (btAddress != null) {
            if (btAddress.equals(btDevice.getBluetoothAddress())) {
                remoteDev = btDevice;
                deviceClass = sdClass;
            }
            
        }
    }

    public void servicesDiscovered(int transID,
                                   ServiceRecord[] servRecord) {

        ServiceRecord tempRecords[];
        int i = 0;
        int j = 0;

        if (sRecord == null) {
            sRecord = servRecord;
        } else {
            tempRecords = new ServiceRecord[sRecord.length + servRecord.length];

            for (i = 0; i < sRecord.length; i++) {
                tempRecords[j++] = sRecord[i];
            }

            for (i = 0; i < servRecord.length; i++) {
                tempRecords[j++] = servRecord[i];
            }

            sRecord = tempRecords;
        }
        reached = true;
        discList.put(new Integer(transID), new Integer(transID));

    }

    public void inquiryCompleted(int discType) {

        done = true;
        type = discType;

        synchronized(finished) {
            try {
                finished.notifyAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void serviceSearchCompleted(int transID, int respCode){
        calledServiceSearchCompleted = true;
        code = respCode;

        if (!inRespList(transID)) {
            idList.put(new Integer(transID), new Integer(code));
        }

        synchronized(finished) {
            try {
                finished.notifyAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Additional methods
     */

    public ServiceRecord[] getRecordArray() {
        return sRecord;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public int getType() {
        return type;
    }

    public int getRespCode(int id) {
        if (inRespList(id)) {
            return ((Integer) idList.get(new Integer(id))).intValue();
        }

        return -1;
    }

    public int getDiscCode(int id) {
        if (inDiscList(id)) {
            return ((Integer) discList.get(new Integer(id))).intValue();
        }

        return -1;
    }

    public boolean isDone() {
        return done;
    }

    public RemoteDevice getRemoteDevice() {
        return remoteDev;
    }

    public boolean inRespList(int id) {

        return idList.containsKey(new Integer(id));
    }

    public boolean inDiscList(int id) {

        return discList.containsKey(new Integer(id));
    }

}

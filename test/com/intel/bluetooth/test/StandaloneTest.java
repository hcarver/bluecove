package com.intel.bluetooth.test;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

/**
 * This class provides a stand-alone test for Blue Cove
 * 
 */
public class StandaloneTest {
    
	public static void main(String[] args) {
	    LocalDevice l;
	    try {
	        l = LocalDevice.getLocalDevice();
	    } catch(BluetoothStateException e) {
	        System.err.println("Cannot get local device: " + e);
	        return;
	    }
	    
	    System.out.println("Local btaddr is " + l.getBluetoothAddress());
	    System.out.println("Local name is " + l.getFriendlyName());
	    
	    BluetoothInquirer bi = new BluetoothInquirer();
	    while(true) {
	        System.out.println("Starting inquiry");
            if(!bi.startInquiry()) break;

	        while(bi.inquiring) {
		        try {
		            Thread.sleep(1000);
		        } catch(Exception e) {
		        }
	        }
	    }
	}
	
	public static class BluetoothInquirer implements DiscoveryListener {
	    boolean inquiring;

	    public boolean startInquiry() {
	        try {
	            LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC,this);
	        } catch(BluetoothStateException e) {
		        System.err.println("Cannot start inquiry: " + e);
		        return false;
		    }
	        inquiring=true;
	        return true;
	    }
	    
        public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        	System.out.println("deviceDiscovered");
            StringBuffer name;
            try {
            	System.out.println("call getFriendlyName");
                name=new StringBuffer(btDevice.getFriendlyName(true));
            } catch(IOException ioe) {
            	ioe.printStackTrace();
                name=new StringBuffer();
            }
          	while(name.length() < 20) name.append(' ');
            System.out.println("Found " + btDevice.getBluetoothAddress() + " : " + name + " : " + cod);
            
        }

        public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        }

        public void serviceSearchCompleted(int transID, int respCode) {
        }

        public void inquiryCompleted(int discType) {
            inquiring=false;
        }
	    
	}
}

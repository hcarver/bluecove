/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
package javax.bluetooth;

public interface DiscoveryListener {
	/*
	 * Indicates the normal completion of device discovery. Used with the
	 * inquiryCompleted() method. The value of INQUIRY_COMPLETED is 0x00 (0).
	 * 
	 * See Also: inquiryCompleted(int), DiscoveryAgent.startInquiry(int,
	 * javax.bluetooth.DiscoveryListener)
	 */

	public static final int INQUIRY_COMPLETED = 0;

	/*
	 * Indicates device discovery has been canceled by the application and did
	 * not complete. Used with the inquiryCompleted() method. The value of
	 * INQUIRY_TERMINATED is 0x05 (5).
	 * 
	 * See Also: inquiryCompleted(int), DiscoveryAgent.startInquiry(int,
	 * javax.bluetooth.DiscoveryListener),
	 * DiscoveryAgent.cancelInquiry(javax.bluetooth.DiscoveryListener)
	 */

	public static final int INQUIRY_TERMINATED = 5;

	/*
	 * Indicates that the inquiry request failed to complete normally, but was
	 * not cancelled. The value of INQUIRY_ERROR is 0x07 (7).
	 * 
	 * See Also: inquiryCompleted(int), DiscoveryAgent.startInquiry(int,
	 * javax.bluetooth.DiscoveryListener)
	 */

	public static final int INQUIRY_ERROR = 7;

	/*
	 * Indicates the normal completion of service discovery. Used with the
	 * serviceSearchCompleted() method. The value of SERVICE_SEARCH_COMPLETED is
	 * 0x01 (1).
	 * 
	 * See Also: serviceSearchCompleted(int, int),
	 * DiscoveryAgent.searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */

	public static final int SERVICE_SEARCH_COMPLETED = 1;

	/*
	 * Indicates the service search has been canceled by the application and did
	 * not complete. Used with the serviceSearchCompleted() method. The value of
	 * SERVICE_SEARCH_TERMINATED is 0x02 (2).
	 * 
	 * See Also: serviceSearchCompleted(int, int),
	 * DiscoveryAgent.searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener),
	 * DiscoveryAgent.cancelServiceSearch(int)
	 */

	public static final int SERVICE_SEARCH_TERMINATED = 2;

	/*
	 * Indicates the service search terminated with an error. Used with the
	 * serviceSearchCompleted() method. The value of SERVICE_SEARCH_ERROR is
	 * 0x03 (3).
	 * 
	 * See Also: serviceSearchCompleted(int, int),
	 * DiscoveryAgent.searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */

	public static final int SERVICE_SEARCH_ERROR = 3;

	/*
	 * Indicates the service search has completed with no service records found
	 * on the device. Used with the serviceSearchCompleted() method. The value
	 * of SERVICE_SEARCH_NO_RECORDS is 0x04 (4).
	 * 
	 * See Also: serviceSearchCompleted(int, int),
	 * DiscoveryAgent.searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */

	public static final int SERVICE_SEARCH_NO_RECORDS = 4;

	/*
	 * Indicates the service search could not be completed because the remote
	 * device provided to DiscoveryAgent.searchServices() could not be reached.
	 * Used with the serviceSearchCompleted() method. The value of
	 * SERVICE_SEARCH_DEVICE_NOT_REACHABLE is 0x06 (6).
	 * 
	 * See Also: serviceSearchCompleted(int, int),
	 * DiscoveryAgent.searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */

	public static final int SERVICE_SEARCH_DEVICE_NOT_REACHABLE = 6;

	/*
	 * Called when a device is found during an inquiry. An inquiry searches for
	 * devices that are discoverable. The same device may be returned multiple
	 * times. Parameters: btDevice - the device that was found during the
	 * inquiry cod - the service classes, major device class, and minor device
	 * class of the remote device See Also: DiscoveryAgent.startInquiry(int,
	 * javax.bluetooth.DiscoveryListener)
	 */

	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod);

	/*
	 * Called when service(s) are found during a service search. Parameters:
	 * transID - the transaction ID of the service search that is posting the
	 * result service - a list of services found during the search request See
	 * Also: DiscoveryAgent.searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord);

	/*
	 * Called when a service search is completed or was terminated because of an
	 * error. Legal status values in the respCode argument include
	 * SERVICE_SEARCH_COMPLETED, SERVICE_SEARCH_TERMINATED,
	 * SERVICE_SEARCH_ERROR, SERVICE_SEARCH_NO_RECORDS and
	 * SERVICE_SEARCH_DEVICE_NOT_REACHABLE. The following table describes when
	 * each respCode will be used: respCode Reason SERVICE_SEARCH_COMPLETED if
	 * the service search completed normally SERVICE_SEARCH_TERMINATED if the
	 * service search request was cancelled by a call to
	 * DiscoveryAgent.cancelServiceSearch() SERVICE_SEARCH_ERROR if an error
	 * occurred while processing the request SERVICE_SEARCH_NO_RECORDS if no
	 * records were found during the service search
	 * SERVICE_SEARCH_DEVICE_NOT_REACHABLE if the device specified in the search
	 * request could not be reached or the local device could not establish a
	 * connection to the remote device
	 * 
	 * Parameters: transID - the transaction ID identifying the request which
	 * initiated the service search respCode - the response code that indicates
	 * the status of the transaction
	 */

	public void serviceSearchCompleted(int transID, int respCode);

	/*
	 * Called when an inquiry is completed. The discType will be
	 * INQUIRY_COMPLETED if the inquiry ended normally or INQUIRY_TERMINATED if
	 * the inquiry was canceled by a call to DiscoveryAgent.cancelInquiry(). The
	 * discType will be INQUIRY_ERROR if an error occurred while processing the
	 * inquiry causing the inquiry to end abnormally. Parameters: discType - the
	 * type of request that was completed; either INQUIRY_COMPLETED,
	 * INQUIRY_TERMINATED, or INQUIRY_ERROR See Also: INQUIRY_COMPLETED,
	 * INQUIRY_TERMINATED, INQUIRY_ERROR
	 */

	public void inquiryCompleted(int discType);
}
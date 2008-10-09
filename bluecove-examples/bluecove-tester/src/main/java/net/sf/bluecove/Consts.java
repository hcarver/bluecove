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

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

public interface Consts {

	public static final String RESPONDER_LONG_UUID = "B10C0BE1111111111111111111110001";

	public static final String RESPONDER_LONG_UUID_L2CAP = "B10C0BE1111111111111111111110002";

	public static final String RESPONDER_LONG_UUID_OBEX = "B10C0BE1111111111111111111110003";

	public static final String RESPONDER_SHORT_UUID = "BC01";

	public static final String RESPONDER_SHORT_UUID_L2CAP = "BC02";

	public static final String RESPONDER_SERVICECLASS_UUID = "B10C0BE1111111111111111111110000";

	public static final UUID uuidLong = new UUID(Consts.RESPONDER_LONG_UUID, false);

	public static final UUID uuidShort = new UUID(Consts.RESPONDER_SHORT_UUID, true);

	public static final UUID uuidL2CAPLong = new UUID(Consts.RESPONDER_LONG_UUID_L2CAP, false);

	public static final UUID uuidL2CAPShort = new UUID(Consts.RESPONDER_SHORT_UUID_L2CAP, true);

	public static final UUID uuidOBEX = new UUID(Consts.RESPONDER_LONG_UUID_OBEX, false);

	public static final UUID uuidSrvClassExt = new UUID(Consts.RESPONDER_SERVICECLASS_UUID, false);

	public static final String RESPONDER_SERVERNAME = "bluecovesrv";

	public static final int TEST_SERVICE_ATTRIBUTE_INT_ID = 0x0A0;

	public static final int TEST_SERVICE_ATTRIBUTE_INT_TYPE = DataElement.INT_1;

	public static final int TEST_SERVICE_ATTRIBUTE_INT_VALUE = 77;

	public static final int TEST_SERVICE_ATTRIBUTE_INT_VALUE_TEST_ALL = 88;

	public static final int TEST_SERVICE_ATTRIBUTE_STR_ID = 0x0A1;

	public static final String TEST_SERVICE_ATTRIBUTE_STR_VALUE = "SomeData";

	public static final int TEST_SERVICE_ATTRIBUTE_URL_ID = 0x0A2;

	public static final String TEST_SERVICE_ATTRIBUTE_URL_VALUE = "http:/www.bluecove.org:80/someUrl?q=10&bluecove=123&ServiceDiscovery=Test";

	public static final int TEST_SERVICE_ATTRIBUTE_LONG_ID = 0x0A3;

	public static final int TEST_SERVICE_ATTRIBUTE_LONG_TYPE = DataElement.INT_4;

	public static final long TEST_SERVICE_ATTRIBUTE_LONG_VALUE = 128; // 0xF1234567l;

	// //4045620583

	public static final int TEST_SERVICE_ATTRIBUTE_BYTES_ID = 0x0A4;

	public static final int TEST_SERVICE_ATTRIBUTE_BYTES_TYPE = DataElement.INT_16;

	public static final byte[] TEST_SERVICE_ATTRIBUTE_BYTES_VALUE = new byte[] { 1, -2, 3, 4, -5, 6, 7, 8, 9, -10, 11,
			12, -13, 14, 15, 16 };

	public static final int VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID = 0x0A5;

	public static final int SERVICE_ATTRIBUTE_BYTES_SERVER_INFO = 0x0A6;

	public static final int SERVICE_ATTRIBUTE_ALL_START = 0x0B0;

	public static final int serverUpTimeOutSec = 2 * 60;

	public static final int DEVICE_COMPUTER = 0x0100;

	public static final int DEVICE_PHONE = 0x0200;

	public static final byte SEND_TEST_START = 7;

	public static final int SEND_TEST_REPLY_OK = 77;

	public static final int SEND_TEST_REPLY_OK_MESSAGE = 78;

	public static final int TEST_SERVER_TERMINATE = 99;

	public static final int TRAFFIC_GENERATOR_WRITE = 100;

	public static final int TRAFFIC_GENERATOR_READ = 101;

	public static final int TRAFFIC_GENERATOR_READ_WRITE = 102;

	// RFCOMM test numbers

	public static final int TEST_STRING = 1;

	public static final int TEST_STRING_BACK = 2;

	public static final int TEST_BYTE = 3;

	public static final int TEST_BYTE_BACK = 4;

	public static final int TEST_STRING_UTF = 5;

	public static final int TEST_STRING_UTF_BACK = 6;

	public static final int TEST_BYTE_ARRAY = 7;

	public static final int TEST_BYTE_ARRAY_BACK = 8;

	public static final int TEST_DataStream = 9;

	public static final int TEST_DataStream_BACK = 10;

	public static final int TEST_StreamAvailable = 11;

	public static final int TEST_StreamAvailable_BACK = 12;

	public static final int TEST_EOF_READ = 13;

	public static final int TEST_EOF_READ_BACK = 14;

	public static final int TEST_EOF_READ_ARRAY = 15;

	public static final int TEST_EOF_READ_ARRAY_BACK = 16;

	public static final int TEST_CONNECTION_INFO = 17;

	public static final int TEST_CLOSED_CONNECTION = 18;

	public static final int TEST_CLOSED_CONNECTION_BACK = 19;

	public static final int TEST_BYTES_256 = 20;

	public static final int TEST_BYTES_256_BACK = 21;

	public static final int TEST_CAN_CLOSE_READ_ON_CLIENT = 22;

	public static final int TEST_CAN_CLOSE_READ_ON_SERVER = 23;

	public static final int TEST_CAN_CLOSE_READ_ARRAY_ON_CLIENT = 24;

	public static final int TEST_CAN_CLOSE_READ_ARRAY_ON_SERVER = 25;

	public static final int TEST_TWO_THREADS_SYNC_BYTES = 26;

	public static final int TEST_TWO_THREADS_SYNC_ARRAYS = 27;

	public static final int TEST_TWO_THREADS_BYTES = 28;

	public static final int TEST_TWO_THREADS_ARRAYS = 29;

	public static final int TEST_LAST_WORKING = TEST_TWO_THREADS_ARRAYS;

	// Next tests may fail on Some phones e.g. SE-K790 But should not fail on
	// BlueCove!

	public static final int TEST_8K_PLUS_BYTE_ARRAY = 30;

	public static final int TEST_8K_PLUS_BYTE_ARRAY_BACK = 31;

	public static final int TEST_64K_PLUS_BYTE_ARRAY = 32;

	public static final int TEST_64K_PLUS_BYTE_ARRAY_BACK = 33;

	public static final int TEST_128K_BYTE_ARRAY_X_10 = 34;

	public static final int TEST_128K_BYTE_ARRAY_X_10_BACK = 35;

	public static final int TEST_LAST_BLUECOVE_WORKING = TEST_128K_BYTE_ARRAY_X_10_BACK;

	// L2CAP test numbers

	public static final int TEST_L2CAP_LAST_WORKING = 3;
}

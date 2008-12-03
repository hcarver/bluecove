/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 * 
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id$
 */
package net.sf.bluecove.obex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import com.intel.bluetooth.obex.BlueCoveOBEX;

/**
 * 
 */
public class ObexBluetoothClient {

	private UserInteraction interaction;

	private String fileName;

	private byte[] data;

	private class ProgressMonitor {

	    int total;

        long startedTime;

        int complete;

        long printedTime;

        static final long STEP_COUNT = 15;

        static final long STEP_INTERVAL = 2 * 1000;

        DecimalFormat formater = new DecimalFormat("#,000");
        
        ProgressMonitor(int total) {
            this.startedTime = System.currentTimeMillis();
            this.complete = 0;
            this.total = total;
            this.printedTime = 0;
            formater.setMaximumFractionDigits(0);
        }

        void transferProgress(int sent) {
            this.complete += sent;
            interaction.setProgressValue(complete);
            long now = System.currentTimeMillis();
            if ((printedTime == 0) || ((now - printedTime) > STEP_INTERVAL)) {
                StringBuffer b = new StringBuffer();
                b.append("Transferring: ");
                b.append(formater.format(complete / 1024)).append("/").append(formater.format(total / 1024)).append("K ");
                b.append((long) (100 * complete / total)).append("% ");
                b.append(bps(total, now - this.startedTime));
                interaction.showStatus(b.toString());
                printedTime = now;
            }
        }

        void transferComplete(String message) {
            if (printedTime != 0) {
                long msec = System.currentTimeMillis() - this.startedTime;
                String txt = message + " " + formater.format(total / 1024) + "K completed in " + (msec/1000) + " sec " + bps(total, msec);
                Logger.debug(txt);
                interaction.showStatus(txt);
            }
        }
        
        String bps(int size, long durationMsec) {
            if (durationMsec == 0) {
                return "";
            }
            return formater.format((1000L * 8 * size) / durationMsec) + " bit/s";
        }
    }

	
	public ObexBluetoothClient(UserInteraction interaction, String fileName, byte[] data) {
		super();
		this.interaction = interaction;
		this.fileName = fileName;
		this.data = data;
	}

	public boolean obexPut(String serverURL) {
		ClientSession clientSession = null;
		ProgressMonitor progress = null;
		try {
			// System.setProperty("bluecove.debug", "true");
			Logger.debug("Connecting", serverURL);
			interaction.showStatus("Connecting ...");
			clientSession = (ClientSession) Connector.open(serverURL);
			HeaderSet hsConnectReply = clientSession.connect(clientSession.createHeaderSet());
			if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
				interaction.showStatus("Connect Error " + hsConnectReply.getResponseCode());
			}
			
			Logger.debug("MTU selected " + BlueCoveOBEX.getPacketSize(clientSession));
			
			progress = new ProgressMonitor(data.length);
			
			HeaderSet hsOperation = clientSession.createHeaderSet();
			hsOperation.setHeader(HeaderSet.NAME, fileName);
			String type = ObexTypes.getObexFileType(fileName);
			if (type != null) {
				hsOperation.setHeader(HeaderSet.TYPE, type);
			}
			hsOperation.setHeader(HeaderSet.LENGTH, new Long(data.length));

			interaction.setProgressMaximum(data.length);
			interaction.setProgressValue(0);

			interaction.showStatus("Sending " + fileName + " ...");
			Operation po = clientSession.put(hsOperation);

			OutputStream os = po.openOutputStream();

			ByteArrayInputStream is = new ByteArrayInputStream(data);
			byte[] buffer = new byte[0x400];
			int i = is.read(buffer);
			while (i != -1) {
				os.write(buffer, 0, i);
                // Show progress
                progress.transferProgress(i);
                
				i = is.read(buffer);
			}
			os.flush();
			os.close();

			// log.debug("put responseCode " + po.getResponseCode());

			po.close();

			interaction.setProgressDone();

			HeaderSet hsDisconnect = clientSession.disconnect(null);
			// log.debug("disconnect responseCode " + hs.getResponseCode());

			if (hsDisconnect.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
			    progress.transferComplete("Success");
				return true;
			} else {
			    progress.transferComplete("Code " + hsDisconnect.getResponseCode());
				return false;
			}

		} catch (IOException e) {
			Logger.error(e);
			interaction.showStatus("Communication error " + e.getMessage());
			return false;
		} catch (Throwable e) {
			Logger.error(e);
			interaction.showStatus("Error " + e.getMessage());
			return false;
		} finally {
			if (clientSession != null) {
				try {
					clientSession.close();
				} catch (IOException ignore) {
				}
			}
			clientSession = null;
			interaction.setProgressValue(0);
		}
	}

}

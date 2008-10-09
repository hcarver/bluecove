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
package net.sf.bluecove.obex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

/**
 * @author vlads
 * 
 */
public class ObexBluetoothClient {

	private UserInteraction interaction;

	private String fileName;

	private byte[] data;

	public ObexBluetoothClient(UserInteraction interaction, String fileName, byte[] data) {
		super();
		this.interaction = interaction;
		this.fileName = fileName;
		this.data = data;
	}

	public boolean obexPut(String serverURL) {
		ClientSession clientSession = null;
		try {
			// System.setProperty("bluecove.debug", "true");
			Logger.debug("Connecting", serverURL);
			interaction.showStatus("Connecting ...");
			clientSession = (ClientSession) Connector.open(serverURL);
			HeaderSet hsConnectReply = clientSession.connect(clientSession.createHeaderSet());
			if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
				interaction.showStatus("Connect Error " + hsConnectReply.getResponseCode());
			}
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
			byte[] buffer = new byte[0xFF];
			int i = is.read(buffer);
			int done = 0;
			while (i != -1) {
				os.write(buffer, 0, i);
				done += i;
				interaction.setProgressValue(done);
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
				interaction.showStatus("Finished successfully");
				return true;
			} else {
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

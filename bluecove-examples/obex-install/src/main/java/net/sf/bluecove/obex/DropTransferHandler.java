/**
 *  MicroEmulator and BlueCove
 *  Copyright (C) 2006-2009 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * 
 */
public class DropTransferHandler extends TransferHandler {

	private static final long serialVersionUID = 1L;

	private static DataFlavor uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String", null);

	private static boolean debugImport = false;

	private Main mainInstance;

	public DropTransferHandler(Main main) {
		mainInstance = main;
	}

	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY;
	}

	public boolean canImport(JComponent comp, DataFlavor transferFlavors[]) {
		for (int i = 0; i < transferFlavors.length; i++) {
			Class representationclass = transferFlavors[i].getRepresentationClass();
			// URL from Explorer or Firefox, KDE
			if ((representationclass != null) && URL.class.isAssignableFrom(representationclass)) {
				if (debugImport) {
					Logger.debug("acepted ", transferFlavors[i]);
				}
				return true;
			}
			// Drop from Windows Explorer
			if (DataFlavor.javaFileListFlavor.equals(transferFlavors[i])) {
				if (debugImport) {
					Logger.debug("acepted ", transferFlavors[i]);
				}
				return true;
			}
			// Drop from GNOME
			if (DataFlavor.stringFlavor.equals(transferFlavors[i])) {
				if (debugImport) {
					Logger.debug("acepted ", transferFlavors[i]);
				}
				return true;
			}
			if (uriListFlavor.equals(transferFlavors[i])) {
				if (debugImport) {
					Logger.debug("acepted ", transferFlavors[i]);
				}
				return true;
			}
			// String mimePrimaryType = transferFlavors[i].getPrimaryType();
			// String mimeSubType = transferFlavors[i].getSubType();
			// if ((mimePrimaryType != null) && (mimeSubType != null)) {
			// if (mimePrimaryType.equals("text") &&
			// mimeSubType.equals("uri-list")) {
			// Logger.debug("acepted ", transferFlavors[i]);
			// return true;
			// }
			// }
			if (debugImport) {
				Logger.debug(i + " unknown import ", transferFlavors[i]);
			}
		}
		if (debugImport) {
			Logger.debug("import rejected");
		}
		return false;
	}

	public boolean importData(JComponent comp, Transferable t) {
		DataFlavor[] transferFlavors = t.getTransferDataFlavors();
		for (int i = 0; i < transferFlavors.length; i++) {
			// Drop from Windows Explorer
			if (DataFlavor.javaFileListFlavor.equals(transferFlavors[i])) {
				Logger.debug("importing", transferFlavors[i]);
				try {
					List fileList = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
					if (fileList.get(0) instanceof File) {
						File f = (File) fileList.get(0);
						mainInstance.downloadFile(getCanonicalFileURL(f));
						for (int fi = 1; fi < fileList.size(); fi++) {
							f = (File) fileList.get(fi);
							mainInstance.queueFile(getCanonicalFileURL(f));
						}
					} else {
						Logger.debug("Unknown object in list ", fileList.get(0));
					}
				} catch (UnsupportedFlavorException e) {
					Logger.debug(e);
				} catch (IOException e) {
					Logger.debug(e);
				}
				return true;
			}

			// Drop from GNOME Firefox
			if (DataFlavor.stringFlavor.equals(transferFlavors[i])) {
				Object data;
				try {
					data = t.getTransferData(DataFlavor.stringFlavor);
				} catch (UnsupportedFlavorException e) {
					continue;
				} catch (IOException e) {
					continue;
				}
				if (data instanceof String) {
					Logger.debug("importing", transferFlavors[i]);
					String path = getPathString((String) data);
					mainInstance.downloadFile(path);
					return true;
				}
			}
			// Drop from GNOME Nautilus
			if (uriListFlavor.equals(transferFlavors[i])) {
				Object data;
				try {
					data = t.getTransferData(uriListFlavor);
				} catch (UnsupportedFlavorException e) {
					continue;
				} catch (IOException e) {
					continue;
				}
				if (data instanceof String) {
					Logger.debug("importing", transferFlavors[i]);
					String path = getPathString((String) data);
					mainInstance.downloadFile(path);
					return true;
				}
			}
			if (debugImport) {
				Logger.debug(i + " unknown importData ", transferFlavors[i]);
			}
		}
		// This is the second best option since it works incorrectly on Max OS X
		// making url like this [file://localhost/users/work/app.jad]
		for (int i = 0; i < transferFlavors.length; i++) {
			Class representationclass = transferFlavors[i].getRepresentationClass();
			// URL from Explorer or Firefox, KDE
			if ((representationclass != null) && URL.class.isAssignableFrom(representationclass)) {
				Logger.debug("importing", transferFlavors[i]);
				try {
					URL jadUrl = (URL) t.getTransferData(transferFlavors[i]);
					String urlString = jadUrl.toExternalForm();
					mainInstance.downloadFile(urlString);
				} catch (UnsupportedFlavorException e) {
					Logger.debug(e);
				} catch (IOException e) {
					Logger.debug(e);
				}
				return true;
			}
		}
		return false;
	}

	private String getPathString(String path) {
		if (path == null) {
			return null;
		}
		StringTokenizer st = new StringTokenizer(path.trim(), "\n\r");
		if (st.hasMoreTokens()) {
			return st.nextToken();
		}
		return path;
	}

	public static String getCanonicalFileURL(File file) {
		String path = file.getAbsoluteFile().getPath();
		if (File.separatorChar != '/') {
			path = path.replace(File.separatorChar, '/');
		}
		// Not network path
		if (!path.startsWith("//")) {
			if (path.startsWith("/")) {
				path = "//" + path;
			} else {
				path = "///" + path;
			}
		}
		return "file:" + path;
	}
}

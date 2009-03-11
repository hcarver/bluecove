/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
 *  =======================================================================================
 *
 *  BlueZ Java docs licensed under GNU Free Documentation License, Version 1.1 http://www.fsf.org
 *  Copyright (C) 2004-2008  Marcel Holtmann <marcel@holtmann.org>
 *  Copyright (C) 2005-2006  Johan Hedberg <johan.hedberg@nokia.com>
 *  Copyright (C) 2005-2006  Claudio Takahasi <claudio.takahasi@indt.org.br>
 *  Copyright (C) 2006-2007  Luiz von Dentz <luiz.dentz@indt.org.br> 
 *
 *  @author vlads
 *  @version $Id$
 */
package org.bluez.v3;

import org.bluez.Error;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * This interface provides methods to list the available adapters, retrieve the default local adapter path and
 * list/activate services. There is just one Manager object instance provided in the path "/org/bluez".
 * 
 * Service org.bluez;
 * <p>
 * Interface org.bluez.Manager;
 * <p>
 * Object path /org/bluez
 * 
 * Created base on D-Bus API description for BlueZ bluez-utils-3.17/hcid/dbus-api.txt
 * 
 */
@DBusInterfaceName("org.bluez.Manager")
public interface Manager extends org.bluez.Manager {

	/**
	 * Deprecated in BlueZ 4
	 * 
	 * @return the current interface version. At the moment only version 0 is supported.
	 */
	public UInt32 InterfaceVersion() throws Error.InvalidArguments;

	/**
	 * Returns object path for the default adapter.
	 * 
	 * @return returns Object in BlueZ 4
	 */
	String DefaultAdapter() throws Error.InvalidArguments, Error.NoSuchAdapter;

	/**
	 * Returns object path for the specified adapter.
	 * 
	 * @param pattern
	 *            "hci0" or "00:11:22:33:44:55"
	 * @return returns Object in BlueZ 4
	 */
	String FindAdapter(String pattern) throws Error.InvalidArguments, Error.NoSuchAdapter;

	/**
	 * Returns list of adapter object paths under /org/bluez
	 * 
	 * @return returns path list
	 */
	String[] ListAdapters() throws Error.InvalidArguments, Error.Failed, Error.OutOfMemory;

	/**
	 * 
	 * Returns object path for the specified service. Valid patterns are the unqiue identifier or a bus name.
	 * 
	 * @since BlueZ 3.10
	 * @param pattern
	 * @return
	 */
	String FindService(String pattern) throws Error.InvalidArguments, Error.NoSuchService;

	/**
	 * Returns list of object paths of current services.
	 * 
	 * @since BlueZ 3.10
	 * @return
	 * @throws Error.InvalidArguments
	 */
	String[] ListServices() throws Error.InvalidArguments;

	/**
	 * Returns the unqiue bus id of the specified service. Valid patterns are the same as for FindService(). If the
	 * service is not running it will be started.
	 * 
	 * @since BlueZ 3.10
	 * @param pattern
	 * @return
	 */
	String ActivateService(String pattern);

	/**
	 * Parameter is object path of added adapter.
	 */
	public class AdapterAdded extends DBusSignal {

		private final String adapterPath;

		public AdapterAdded(String path, String adapterPath) throws DBusException {
			super(path, adapterPath);
			this.adapterPath = adapterPath;
		}

		/**
		 * @return the adapterPath
		 */
		public String getAdapterPath() {
			return adapterPath;
		}
	}

	/**
	 * Parameter is object path of removed adapter.
	 */
	public class AdapterRemoved extends DBusSignal {

		private final String adapterPath;

		public AdapterRemoved(String path, String adapterPath) throws DBusException {
			super(path, adapterPath);
			this.adapterPath = adapterPath;
		}

		/**
		 * @return the adapterPath
		 */
		public String getAdapterPath() {
			return adapterPath;
		}
	}

	/**
	 * Parameter is object path of the new default adapter.
	 * 
	 * @since BlueZ 3.10
	 */
	public class DefaultAdapterChanged extends DBusSignal {

		private final String adapterPath;

		public DefaultAdapterChanged(String path, String adapterPath) throws DBusException {
			super(path, adapterPath);
			this.adapterPath = adapterPath;
		}

		/**
		 * @return the adapterPath
		 */
		public String getAdapterPath() {
			return adapterPath;
		}
	}

	/**
	 * Parameter is object path of registered service agent.
	 * 
	 * @since BlueZ 3.10
	 */
	public class ServiceAdded extends DBusSignal {

		private final String serviceAgentPath;

		public ServiceAdded(String path, String serviceAgentPath) throws DBusException {
			super(path, serviceAgentPath);
			this.serviceAgentPath = serviceAgentPath;
		}

		/**
		 * @return the serviceAgentPath
		 */
		public String getServiceAgentPath() {
			return serviceAgentPath;
		}
	}

	/**
	 * Parameter is object path of unregistered service agent.
	 * 
	 * @since BlueZ 3.10
	 */
	public class ServiceRemoved extends DBusSignal {

		private final String serviceAgentPath;

		public ServiceRemoved(String path, String serviceAgentPath) throws DBusException {
			super(path, serviceAgentPath);
			this.serviceAgentPath = serviceAgentPath;
		}

		/**
		 * @return the serviceAgentPath
		 */
		public String getServiceAgentPath() {
			return serviceAgentPath;
		}
	}
}

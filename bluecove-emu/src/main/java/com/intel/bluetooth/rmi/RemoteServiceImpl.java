/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

class RemoteServiceImpl implements RemoteService {

	private static final long serialVersionUID = 1L;

	private Map<String, Class<?>> cash = new HashMap<String, Class<?>>();

	public RemoteServiceImpl() throws RemoteException {
	}

	private Class<?> getClassByInterfaceName(String interfaceName) throws ClassNotFoundException {
		Class<?> c;
		synchronized (cash) {
			c = cash.get(interfaceName);
			if (c == null) {
				c = Class.forName(interfaceName + "Impl");
				cash.put(interfaceName, c);
			}
		}
		return c;
	}

	public boolean verify(String interfaceName) throws RemoteException {
		try {
			getClassByInterfaceName(interfaceName);
		} catch (Throwable e) {
			throw new RemoteException("Service for " + interfaceName + " not ready", e);
		}
		return true;
	}

	public ServiceResponse execute(ServiceRequest request) {
		try {
			Class<?> c = getClassByInterfaceName(request.getClassName());
			Method m = c.getDeclaredMethod(request.getMethodName(), request.getParameterTypes());
			ServiceResponse response = new ServiceResponse();
			try {
				response.setReturnValue(m.invoke(c.newInstance(), request.getParameters()));
			} catch (InvocationTargetException e) {
				response.setException(e.getTargetException());
			}
			return response;
		} catch (Throwable e) {
			return new ServiceResponse(e);
		}
	}

}

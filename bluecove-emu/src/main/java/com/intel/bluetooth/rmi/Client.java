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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Vector;

public class Client {

	private static final String rmiRegistryHostDefault = "localhost";

	private static final int rmiRegistryPortDefault = Server.rmiRegistryPortDefault;

	private static RemoteService remoteService;

	private static class ServiceProxy implements InvocationHandler {

		private AccessControlContext accessControlContext;

		private ServiceProxy() {
			accessControlContext = AccessController.getContext();
		}

		public Object invoke(Object proxy, final Method m, Object[] args) throws Throwable {
			final ServiceRequest request = new ServiceRequest(m.getDeclaringClass().getCanonicalName(), m.getName(), m
					.getParameterTypes(), args);
			ServiceResponse response;
			try {
				response = AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceResponse>() {
					public ServiceResponse run() throws RuntimeException {
						return execute(request, m);
					}
				}, accessControlContext);
			} catch (PrivilegedActionException e) {
				Throwable cause = e.getCause();
				if (cause != null) {
					throw new RuntimeException(cause.getMessage(), cause);
				} else {
					throw e;
				}
			}
			if (response.getException() == null) {
				return response.getReturnValue();
			} else {
				Throwable t = response.getException();
				// Build combined StackTrace
				StackTraceElement[] remote = t.getStackTrace();
				StackTraceElement[] curreent = Thread.currentThread().getStackTrace();
				List<StackTraceElement> combined = new Vector<StackTraceElement>();
				for (int i = 0; i < remote.length; i++) {
					combined.add(remote[i]);
					if ((remote[i].getMethodName().equals(m.getName()))
							&& (remote[i].getClassName().startsWith(m.getDeclaringClass().getCanonicalName()))) {
						break;
					}
				}
				int startClient = curreent.length;
				for (int i = 0; i < curreent.length; i++) {
					if (curreent[i].getClassName().equals(this.getClass().getName())) {
						startClient = i + 1;
						break;
					}
				}
				for (int i = startClient; i < curreent.length; i++) {
					combined.add(curreent[i]);
				}
				t.setStackTrace(combined.toArray(new StackTraceElement[combined.size()]));
				throw t;
			}
		}
	}

	private static String getRemoteExceptionMessage(RemoteException e) {
		String message = e.getMessage();
		int idx = message.indexOf("; nested exception is:");
		if (idx != -1) {
			return message.substring(0, idx);
		}
		return message;
	}

	public synchronized static Object getService(Class<?> interfaceClass, boolean isMaster, String host, String port)
			throws RuntimeException {
		if (remoteService == null) {
			try {
				if (isMaster) {
					if ((host != null) && (!"localhost".equals(host))) {
						throw new IllegalArgumentException("Can't start RMI registry while connecting to remote host "
								+ host);
					}
					Server.start(port);
				}
				remoteService = getRemoteService(host, port);
				remoteService.verify(interfaceClass.getCanonicalName());
			} catch (RemoteException e) {
				Throwable t = (e.getCause() != null) ? e.getCause() : e;
				throw new RuntimeException(getRemoteExceptionMessage(e), t);
			} catch (NotBoundException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		Class<?>[] allInterfaces = new Class[interfaceClass.getInterfaces().length + 1];
		allInterfaces[0] = interfaceClass;
		System.arraycopy(interfaceClass.getInterfaces(), 0, allInterfaces, 1, interfaceClass.getInterfaces().length);
		return Proxy.newProxyInstance(interfaceClass.getClassLoader(), allInterfaces, new ServiceProxy());
	}

	private static ServiceResponse execute(ServiceRequest request, Method method) throws RuntimeException {
		try {
			return remoteService.execute(request);
		} catch (RemoteException e) {
			Throwable t = (e.getCause() != null) ? e.getCause() : e;
			throw new RuntimeException(getRemoteExceptionMessage(e), t);
		}
	}

	private static RemoteService getRemoteService(String host, String port) throws RemoteException, NotBoundException {
		String rmiHost = rmiRegistryHostDefault;
		if ((host != null) && (host.length() > 0)) {
			rmiHost = host;
		}
		int rmiPort = rmiRegistryPortDefault;
		if ((port != null) && (port.length() > 0)) {
			rmiPort = Integer.parseInt(port);
		}
		if (rmiPort == 0) {
			// in process server
			return new RemoteServiceImpl();
		} else {
			Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
			return (RemoteService) registry.lookup(RemoteService.SERVICE_NAME);
		}
	}
}

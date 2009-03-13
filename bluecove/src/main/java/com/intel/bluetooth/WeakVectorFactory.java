/**
 *  BlueCove - Java library for Bluetooth
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
package com.intel.bluetooth;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.WeakHashMap;

/**
 * An entry in a WeakVector will automatically be removed when its key is no
 * longer in ordinary use. This class is wrapper above WeakHashMap when
 * available. e.g. on J2SE 1.2 and above For IBM J9 MIDP we will use Vector to
 * make application work. But connection can't be discarded by the garbage
 * collector.
 *
 */
class WeakVectorFactory {

	public static interface WeakVector {

		public void addElement(Object obj);

		public int size();

		public boolean removeElement(Object obj);

		public boolean contains(Object elem);

		public Object firstElement();

		public Enumeration elements();

		public void removeAllElements();

	}

	public static WeakVector createWeakVector() {
		try {
			return new WeakVectorOnWeakHashMapImpl();
		} catch (Throwable e) {
			return new WeakVectorOnVectorImpl();
		}
	}

	private static class WeakVectorOnVectorImpl implements WeakVector {

		private Vector vectorImpl;

		private WeakVectorOnVectorImpl() {
			vectorImpl = new Vector();
		}

		public void addElement(Object obj) {
			vectorImpl.addElement(obj);
		}

		public boolean contains(Object elem) {
			return vectorImpl.contains(elem);
		}

		public Object firstElement() {
			return vectorImpl.firstElement();
		}

		public Enumeration elements() {
			return vectorImpl.elements();
		}

		public boolean removeElement(Object obj) {
			return vectorImpl.removeElement(obj);
		}

		public int size() {
			return vectorImpl.size();
		}

		public void removeAllElements() {
			vectorImpl.removeAllElements();
		}

	}

	private static class WeakVectorOnWeakHashMapImpl implements WeakVector {

		private WeakHashMap mapImpl;

		private static class EnumerationAdapter implements Enumeration {

			Iterator iterator;

			public EnumerationAdapter(Iterator iterator) {
				this.iterator = iterator;
			}

			public boolean hasMoreElements() {
				return this.iterator.hasNext();
			}

			public Object nextElement() {
				return this.iterator.next();
			}

		}

		private WeakVectorOnWeakHashMapImpl() {
			mapImpl = new WeakHashMap();

		}

		public void addElement(Object obj) {
			mapImpl.put(obj, new Object());

		}

		public boolean contains(Object elem) {
			return mapImpl.containsKey(elem);
		}

		public Object firstElement() {
			return mapImpl.keySet().iterator().next();
		}

		public Enumeration elements() {
			return new EnumerationAdapter(mapImpl.keySet().iterator());
		}

		public boolean removeElement(Object obj) {
			return (mapImpl.remove(obj) != null);
		}

		public int size() {
			return mapImpl.size();
		}

		public void removeAllElements() {
			mapImpl.clear();
		}

	}
}

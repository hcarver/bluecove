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
 * @author vlads
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

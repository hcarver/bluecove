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

package net.sf.bluecove.util;

import java.util.Enumeration;
import java.util.Vector;

public class CollectionUtils {

	public static interface IsComparable {

		public int compareTo(Object o);

	}

	public static void sort(Vector v) {
		quickSort(v, 0, v.size() - 1);
	}

	public static Vector copy(Vector v) {
		Vector copy = new Vector();
		synchronized (v) {
			for (Enumeration en = v.elements(); en.hasMoreElements();) {
				copy.addElement(en.nextElement());
			}
		}
		return copy;
	}

	public static int compareLong(long l1, long l2) {
		long cmp = l1 - l2;
		if (cmp > 0) {
			return 1;
		}
		if (cmp < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	private static int compare(Object o1, Object o2) {
		if (o1 instanceof Long) {
			return compareLong(((Long) o1).longValue(), ((Long) o2).longValue());
		} else if (o1 instanceof Integer) {
			return compareLong(((Integer) o1).longValue(), ((Integer) o2).longValue());
		} else if (o1 instanceof String) {
			return ((String) o1).compareTo((String) o2);
		}
		return ((IsComparable) o1).compareTo(o2);
	}

	private static void quickSort(Vector v, int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		if (hi0 > lo0) {
			Object mid = v.elementAt((lo0 + hi0) / 2);
			while (lo <= hi) {
				while (lo < hi0 && compare(v.elementAt(lo), mid) < 0) {
					lo++;
				}
				for (; hi > lo0 && compare(v.elementAt(hi), mid) > 0; hi--) {
					;
				}
				if (lo <= hi) {
					swap(v, lo, hi);
					lo++;
					hi--;
				}
			}
			if (lo0 < hi) {
				quickSort(v, lo0, hi);
			}
			if (lo < hi0) {
				quickSort(v, lo, hi0);
			}
		}
	}

	public static void swap(Vector v, int i, int j) {
		Object o = v.elementAt(i);
		v.setElementAt(v.elementAt(j), i);
		v.setElementAt(o, j);
	}
}

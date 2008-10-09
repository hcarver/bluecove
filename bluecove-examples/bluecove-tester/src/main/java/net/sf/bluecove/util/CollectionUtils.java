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
		for (Enumeration en = v.elements(); en.hasMoreElements();) {
			copy.addElement(en.nextElement());
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

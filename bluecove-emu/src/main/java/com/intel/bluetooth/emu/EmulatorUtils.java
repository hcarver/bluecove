/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Michael Lifshits
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
package com.intel.bluetooth.emu;

import java.util.Arrays;
import java.util.Set;
import java.util.Vector;

/**
 * @author vlads
 * 
 */
public class EmulatorUtils {

	public static long getNextAvailable(Vector<Long> handles, long firstAvalable, int step) {
		return getNextAvailable(handles.toArray(new Long[handles.size()]), firstAvalable, step);
	}

	public static long getNextAvailable(Set<Long> handles, long firstAvalable, int step) {
		return getNextAvailable(handles.toArray(new Long[handles.size()]), firstAvalable, step);
	}

	public static long getNextAvailable(Long[] handles, long firstAvalable, int step) {
		if (handles.length == 0) {
			return firstAvalable;
		}
		Arrays.sort(handles);
		for (int i = 0; i < handles.length; i++) {
			long expect = firstAvalable + i * step;
			if (((Long) handles[i]).longValue() != expect) {
				return expect;
			}
		}
		return ((Long) handles[handles.length - 1]).longValue() + step;
	}
}

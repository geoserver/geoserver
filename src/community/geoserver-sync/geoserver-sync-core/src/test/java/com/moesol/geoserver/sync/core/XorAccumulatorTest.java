/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.core;




import com.moesol.geoserver.sync.core.XorAccumulator;

import junit.framework.TestCase;

public class XorAccumulatorTest extends TestCase {

	public void testUpdate() {
		XorAccumulator a = new XorAccumulator();
		byte[] v = new byte[20];
		v[0] = 1;
		a.update(v);
		assertEquals("0100000000000000000000000000000000000000", a.toString());
		
		v[19] = 1;
		a.update(v);
		assertEquals("0000000000000000000000000000000000000001", a.toString());
	}

	public void testToString() {
		XorAccumulator a = new XorAccumulator();
		assertEquals(40, a.toString().length());
		assertEquals("0000000000000000000000000000000000000000", a.toString());
	}

}

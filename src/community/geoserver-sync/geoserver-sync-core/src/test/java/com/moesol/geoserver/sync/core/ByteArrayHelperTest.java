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




import java.util.Arrays;

import com.moesol.geoserver.sync.core.ByteArrayHelper;

import junit.framework.TestCase;

public class ByteArrayHelperTest extends TestCase {

	public void testFromHex() {
		byte[] value;
		String hex;
		
		value = new byte[] { 0, 1, 2, 3 };
		hex = ByteArrayHelper.toHex(value);
		assertTrue(hex, Arrays.equals(value, ByteArrayHelper.fromHex(hex)));
		
		value = new byte[] { (byte) 0xFF, (byte) 0xF1, (byte) 0xF2 };
		hex = ByteArrayHelper.toHex(value);
		assertTrue(hex, Arrays.equals(value, ByteArrayHelper.fromHex(hex)));
	}
	
	public void testBadHex() {
		try {
			ByteArrayHelper.fromHex("a");
			fail("ex");
		} catch (IllegalArgumentException e) {
			assertEquals("String length must be even", e.getMessage());
		}
	}

}

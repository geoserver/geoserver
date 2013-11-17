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




import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.json.Sha1SyncJson;

import junit.framework.TestCase;

public class VersionFeaturesTest extends TestCase {

	public void testFromSha1SyncJson() {
		Sha1SyncJson json = new Sha1SyncJson();
		json.v = null;
		assertEquals(VersionFeatures.VERSION1, VersionFeatures.fromSha1SyncJson(json));
		
		json.v = "1";
		assertEquals(VersionFeatures.VERSION1, VersionFeatures.fromSha1SyncJson(json));

		json.v = "2";
		assertEquals(VersionFeatures.VERSION2, VersionFeatures.fromSha1SyncJson(json));
		
		try {
			json.v = "3";
			VersionFeatures.fromSha1SyncJson(json);
			fail("expected exception");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

}

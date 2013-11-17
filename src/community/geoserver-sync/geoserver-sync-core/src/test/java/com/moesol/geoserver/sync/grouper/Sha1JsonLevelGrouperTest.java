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

package com.moesol.geoserver.sync.grouper;




import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.grouper.Sha1JsonLevelGrouper;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

import junit.framework.TestCase;

public class Sha1JsonLevelGrouperTest extends TestCase {
	
	private MessageDigest m_sha1;
	private List<IdAndValueSha1s> m_data;
	private Sha1JsonLevelGrouper m_grouper;

	public Sha1JsonLevelGrouperTest() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		setupWithTestData(257);
	}
	private void setupWithTestData(int n) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		m_sha1 = MessageDigest.getInstance("SHA-1");
		m_data = new ArrayList<IdAndValueSha1s>();
		for (int i = 0; i < n; i++) {
			String v = "data" + i;
			Sha1Value aSha1 = new Sha1Value(m_sha1.digest(v.getBytes("UTF-8")));
			m_data.add(new IdAndValueSha1s(aSha1, aSha1));
		}
		m_grouper = new Sha1JsonLevelGrouper(VersionFeatures.VERSION1, m_data);
//		System.out.println(m_data);
	}

	public void testGroupForLevelZero() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		m_grouper.groupForLevel(0);
		Sha1SyncJson json = m_grouper.getJson();
		
//		System.out.println(new Gson().toJson(json));
		assertEquals(0, json.l);
		assertEquals(1, json.h.size());
		assertEquals(257, json.m);
		Sha1SyncPositionHash positionHash = json.h.get(0);
		assertEquals("", positionHash.p);
	}

	public void testGroupForLevelOne() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		m_grouper.groupForLevel(1);
		Sha1SyncJson json = m_grouper.getJson();
		
//		System.out.println(new Gson().toJson(json));
		assertEquals(1, json.l);
		assertEquals(164, json.h.size());
		assertEquals(5, json.m);
		
//		new DebugPrintLevelGrouper(m_data).groupForLevel(1);
	}

	/**
	 * One change should lead to only one top level hash changing out of the 256 hashes.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public void testGroupForLevelOneWithOneChange() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		m_grouper.groupForLevel(1);
		Sha1SyncJson json = m_grouper.getJson();
		
		Sha1Value aSha1 = new Sha1Value(m_sha1.digest("extra".getBytes("UTF-8")));
		m_data.add(new IdAndValueSha1s(aSha1, aSha1));
		m_grouper = new Sha1JsonLevelGrouper(VersionFeatures.VERSION1, m_data);
		m_grouper.groupForLevel(1);
		Sha1SyncJson json2 = m_grouper.getJson();

		for (int i = 0; i < json.h.size(); i++) {
			if (i == 118) {
				continue;
			}
			assertEquals("i: " + i, json.h.get(i).s, json2.h.get(i).s);
		}
		
//		System.out.println(new Gson().toJson(json));
		assertEquals(1, json.l);
		assertEquals(164, json.h.size());
		assertEquals(5, json.m);
		
//		new DebugPrintLevelGrouper(m_data).groupForLevel(1);
	}
	
	public void testGroupForLevelTwo() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		m_grouper.groupForLevel(2);
		Sha1SyncJson json = m_grouper.getJson();
		
//		System.out.println(new Gson().toJson(json));
		assertEquals(2, json.l);
		assertEquals(257, json.h.size());
		assertEquals(1, json.m);

//		new DebugPrintLevelGrouper(m_data).groupForLevel(2);
	}

	public void testGroupForLevelThree() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		m_grouper.groupForLevel(3);
		Sha1SyncJson json = m_grouper.getJson();
		
//		System.out.println(new Gson().toJson(json));
		assertEquals(3, json.l);
		assertEquals(257, json.h.size());
		assertEquals(1, json.m);
	}
	
	public void testGroupSixWithCollision() {
		Sha1Value fake = new Sha1Value(""); 
		m_grouper = new Sha1JsonLevelGrouper(VersionFeatures.VERSION2, Arrays.asList(
				new IdAndValueSha1s(new Sha1Value("1091a76e395ba65b6a35f7ae5110eb02e0661137"), fake), 
				new IdAndValueSha1s(new Sha1Value("4ef9968cef6257d719b5a8ee58037c7570270c0f"), fake), 
				new IdAndValueSha1s(new Sha1Value("ba58d6e118850afcb9833c646cd5930c9edb33c3"), fake),
				new IdAndValueSha1s(new Sha1Value("c313c8e098040f0b8e7b233f589b809e80f14fc5"), fake),
				new IdAndValueSha1s(new Sha1Value("cf300c55b1d127f07005fe22d6ec12fa2501f089"), fake),
				new IdAndValueSha1s(new Sha1Value("cf64472f30a25d72b259b220cc484cab72b8fa4b"), fake)));
		m_grouper.groupForLevel(1);
		Sha1SyncJson json = m_grouper.getJson();
		
		assertEquals(1, json.level());
		assertEquals(5, json.hashes().size());
		assertEquals(2, json.max());
	}
	
}

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




import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.moesol.geoserver.sync.core.XorAccumulator;

import junit.framework.TestCase;

/**
 * @author hastings
 * @deprecated The collision space for XOR of SHA-1 seems much larger than we thought
 * so we are not going to use XOR to overcome the sorting issue.
 */
public class XorSha1Test extends TestCase {
	
	private MessageDigest m_sha1;
	private boolean DEBUG = false;
	List<String> largeSet = new ArrayList<String>();
	List<String> veryLargeSet = new ArrayList<String>();
	
	public XorSha1Test() throws NoSuchAlgorithmException {
		m_sha1 = MessageDigest.getInstance("SHA-1");
	}

	public void testSetReorder() throws NoSuchAlgorithmException {
		List<String> smallSet = new ArrayList<String>();
		smallSet.add("A");
		smallSet.add("B");
		smallSet.add("C");
		smallSet.add("D");
		
		XorAccumulator s1 = computeSha1OfSet(smallSet);		
		Collections.shuffle(smallSet);
		XorAccumulator s2 = computeSha1OfSet(smallSet);
		assertEquals(s1, s2);
	}
	
	public void testReorderLarge() {
		fillWithRandom(largeSet, 10000);
		XorAccumulator s1 = computeSha1OfSet(largeSet);		
		Collections.shuffle(largeSet);
		XorAccumulator s2 = computeSha1OfSet(largeSet);
		assertEquals(s1, s2);
	}

	public void testReorderVeryLarge() {
		fillWithRandom(veryLargeSet, 100000);
		XorAccumulator s1 = computeSha1OfSet(veryLargeSet);		
		Collections.shuffle(veryLargeSet);
		XorAccumulator s2 = computeSha1OfSet(veryLargeSet);
		assertEquals(s1, s2);
	}
	
	public void testReorderVeryLargeWithOneChange() {
		fillWithRandom(veryLargeSet, 100000);
		XorAccumulator s1 = computeSha1OfSet(veryLargeSet);		
		Collections.shuffle(veryLargeSet);
		perturbZeroElement(veryLargeSet);
		XorAccumulator s2 = computeSha1OfSet(veryLargeSet);
		assertFalse(s1.equals(s2));
	}
	
	private void perturbZeroElement(List<String> veryLargeSet) {
		String v = veryLargeSet.get(0);
		UUID uid = UUID.fromString(v);
		long mostSigBits = uid.getMostSignificantBits();
		long leastSigBits = uid.getLeastSignificantBits();
		leastSigBits++; // one bit change...
		UUID uid2 = new UUID(mostSigBits, leastSigBits);
		veryLargeSet.set(0, uid2.toString());
	}

	private void fillWithRandom(List<String> set, int n) {
		set.clear();
		for (int i = 0; i < n; i++) {
			UUID id = UUID.randomUUID();
			set.add(id.toString());
		}
	}

	private XorAccumulator computeSha1OfSet(List<String> smallSet) {
		XorAccumulator result = new XorAccumulator();
		long st = System.currentTimeMillis();
		for (String s : smallSet) {
			byte[] elSha1 = m_sha1.digest(s.getBytes());
			m_sha1.reset();
			result.update(elSha1);
		}
		if (DEBUG) {
			System.out.println(result);
		}
		long e = System.currentTimeMillis();
		
		System.out.println("took " + (e-st) + " ms");
		
		return result;
	}
	
	/**
	 * Run many 100K permutations...
	 * @param args
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException {
		long howLong = 600*1000; // ms
		long startTime = System.currentTimeMillis();
		XorSha1Test t = new XorSha1Test();
		long numRuns = 0L;
		while (System.currentTimeMillis() - startTime < howLong) {
			t.testReorderVeryLarge();
			t.testReorderVeryLargeWithOneChange();
			numRuns++;
			System.out.println("run: " + numRuns);
		}
		System.out.println("numRuns: " + numRuns);
	}
}

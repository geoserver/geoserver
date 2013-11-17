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

package com.moesol.geoserver.sync.filter;




import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.google.gson.Gson;
import com.moesol.geoserver.sync.core.IdAndValueSha1Comparator;
import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.filter.Sha1SyncFilterFunction;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;
import com.moesol.geoserver.sync.samples.Samples;
import com.vividsolutions.jts.io.ParseException;

public class Sha1SyncFilterFunctionTest extends TestCase {
    private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    private final Samples m_samples = new Samples();
    
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty("sha1.keep.name", "true");
	}
	
    @Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Sha1SyncFilterFunction.clearThreadLocals();
		System.clearProperty("sha1.keep.name");
	}


	public void testTwoFewArgs() throws ParseException {
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(0);
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal(new Gson().toJson(sync)));

		try {
			new Sha1SyncFilterFunction("sha1Sync", args);
			fail("ex");
		} catch (IllegalArgumentException e) {
			assertEquals("sha1Sync requires two arguments {attributes}, and {sha1SyncJson}", e.getMessage());
		}
    }
	
	public void testEvaluateObject() throws ParseException {
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(0);
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("-all"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction("sha1Sync", args);
		Object result;
		
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		
		assertEquals(2, func.getFeatureSha1s().size());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(1)).toString());

		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(1)).toString());
	}

	public void testSha1Match() throws ParseException, NoSuchAlgorithmException {
		Sha1Value featureSha1 = new Sha1Value("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a");
		MessageDigest SHA1 = MessageDigest.getInstance("SHA-1");
		Sha1Value sha1OfSha1 = new Sha1Value(SHA1.digest(featureSha1.get()));
		List<Sha1SyncPositionHash> hashes = new ArrayList<Sha1SyncPositionHash>();
		hashes.add(new Sha1SyncPositionHash().position("").summary(sha1OfSha1.toString()));
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(1).hashes(hashes);
		
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("-all"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction("sha1Sync", args);
		Object result;
		
		result = func.evaluate(feature());
		//assertEquals("false", result.toString());
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());
		
		result = func.evaluate(feature());
		//assertEquals("false", result.toString());
		assertEquals("true", result.toString());
		assertEquals(2, func.getFeatureSha1s().size());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());

		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(1)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(1)).toString());
	
	}

// TODO Code for this check was removed...	
//	public void testIncomingDuplicate() {
//		List<Sha1SyncPositionHash> hashes = new ArrayList<Sha1SyncPositionHash>();
//		hashes.add(new Sha1SyncPositionHash().position("").summary("aa"));
//		hashes.add(new Sha1SyncPositionHash().position("").summary("aa"));
//		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(1).hashes(hashes);
//		
//		List<Expression> args = new ArrayList<Expression>();
//		args.add(ff.literal("-all"));
//		args.add(ff.literal(new Gson().toJson(sync)));
//		try {
//			new Sha1SyncFilterFunction("sha1Sync", args);
//			fail("ex");
//		} catch (IllegalArgumentException e) {
//			assertEquals("Duplicate position found old({\"p\":\"\",\"s\":\"aa\"}), new({\"p\":\"\",\"s\":\"aa\"})", e.getMessage());
//		}
//	}
	
	public void testSha1Match_InSameGroupButNotSame() throws ParseException, NoSuchAlgorithmException {
		Sha1Value featureSha1 = new Sha1Value("F6e82e2d2452830bbdb9bc1ce353a2e159996308");
		MessageDigest SHA1 = MessageDigest.getInstance("SHA-1");
		Sha1Value sha1OfSha1 = new Sha1Value(SHA1.digest(featureSha1.get()));
		List<Sha1SyncPositionHash> hashes = new ArrayList<Sha1SyncPositionHash>();
		hashes.add(new Sha1SyncPositionHash().position("").summary(sha1OfSha1.toString()));
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(1).hashes(hashes);
		
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("-all"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction("sha1Sync", args);
		Object result;
		
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());
		
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(2, func.getFeatureSha1s().size());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());
		
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(1)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(1)).toString());
	
	}
	
	private Sha1Value v1(IdAndValueSha1s pair) {
		return VersionFeatures.VERSION1.getBucketPrefixSha1(pair);
	}
	
	public void testSha1MatchShallow() throws ParseException, NoSuchAlgorithmException {
		Sha1Value featureSha1 = new Sha1Value("56e82e2d2452830bbdb9bc1ce353a2e159996308");
		MessageDigest SHA1 = MessageDigest.getInstance("SHA-1");
		Sha1Value sha1OfSha1 = new Sha1Value(SHA1.digest(featureSha1.get()));
		List<Sha1SyncPositionHash> hashes = new ArrayList<Sha1SyncPositionHash>();
		hashes.add(new Sha1SyncPositionHash().position("").summary(sha1OfSha1.toString()));
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(2).hashes(hashes);
		
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("-all"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction("sha1Sync", args);
		Object result;
		
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());

		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(2, func.getFeatureSha1s().size());
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());
		
		//assertEquals("ee99b7fbcfd28b5a84fd478d8c92d574d3e4875a", v1(func.getFeatureSha1s().get(1)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(1)).toString());
	
	}
	
	private Object feature() throws ParseException {
		return m_samples.buildSimpleFeature("FID1");
	}

}

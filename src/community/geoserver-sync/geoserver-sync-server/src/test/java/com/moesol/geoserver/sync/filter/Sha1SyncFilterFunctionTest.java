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


import static org.junit.Assert.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.factory.CommonFactoryFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.google.gson.Gson;
import com.moesol.geoserver.sync.core.IdAndValueSha1s;
import com.moesol.geoserver.sync.core.Sha1Value;
import com.moesol.geoserver.sync.core.VersionFeatures;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;
import com.moesol.geoserver.sync.samples.Samples;
import com.vividsolutions.jts.io.ParseException;

public class Sha1SyncFilterFunctionTest {
    private static final String EXPECTED_SHA1 = "db90c50160c32f3517b7d3b1c78be70a7f2ba992";
	private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    private final Samples m_samples = new Samples();
    
	@Before
	public void setUp() throws Exception {
    	Sha1SyncFilterFunction.clearThreadLocals();
	}
	
    @After
    public void tearDown() throws Exception {
		Sha1SyncFilterFunction.clearThreadLocals();
	}

    @Test
	public void testTwoFewArgs() throws ParseException {
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(0);
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal(new Gson().toJson(sync)));

		try {
			Sha1SyncFilterFunction func = new Sha1SyncFilterFunction();
			func.setParameters(args);
			func.evaluate(null);
		} catch (IllegalArgumentException e) {
			assertEquals("Function sha1Sync expected 2 arguments, got 1", e.getMessage());
		}
    }
	
    @Test
	public void testEvaluateObject() throws ParseException {
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(0);
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("-all"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction();
		func.setParameters(args);
		Object result;
		
		Object feature = feature();
		result = func.evaluate(feature);
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		//
		System.out.println("EVALUATE OBJECT: " + func.getFeatureSha1s());
		//
		assertEquals("feature: " + feature + ",sha1s: " + func.getFeatureSha1s(),
				EXPECTED_SHA1, 
				v1(func.getFeatureSha1s().get(0)).toString());
		result = func.evaluate(feature);
		assertEquals("true", result.toString());
		
		assertEquals(2, func.getFeatureSha1s().size());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(1)).toString());
	}

    @Test
	public void testEvaluateObjectWithStar() throws ParseException {
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(0);
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("*"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction();
		func.setParameters(args);
		Object result;
		
		Object feature = feature();
		result = func.evaluate(feature);
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		//
		System.out.println("EVALUATE OBJECT: " + func.getFeatureSha1s());
		//
		assertEquals("feature: " + feature + ",sha1s: " + func.getFeatureSha1s(),
				EXPECTED_SHA1, 
				v1(func.getFeatureSha1s().get(0)).toString());
		result = func.evaluate(feature);
		assertEquals("true", result.toString());
		
		assertEquals(2, func.getFeatureSha1s().size());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(1)).toString());
	}
	
    @Test
	public void testEvaluateObjectWithoutName() throws ParseException {
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(0);
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("* -name"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction();
		func.setParameters(args);
		Object result;
		
		Object feature = feature();
		result = func.evaluate(feature);
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		//
		System.out.println("EVALUATE OBJECT: " + func.getFeatureSha1s());
		//
		assertEquals("feature: " + feature + ",sha1s: " + func.getFeatureSha1s(),
				"a0d545781a8852e26514091f08f2384942bf4061", 
				v1(func.getFeatureSha1s().get(0)).toString());
		result = func.evaluate(feature);
		assertEquals("true", result.toString());
		
		assertEquals(2, func.getFeatureSha1s().size());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals("a0d545781a8852e26514091f08f2384942bf4061", v1(func.getFeatureSha1s().get(1)).toString());
	}
    
    @Test
	public void testSha1Match() throws ParseException, NoSuchAlgorithmException {
		Sha1Value featureSha1 = new Sha1Value(EXPECTED_SHA1);
		MessageDigest SHA1 = MessageDigest.getInstance("SHA-1");
		Sha1Value sha1OfSha1 = new Sha1Value(SHA1.digest(featureSha1.get()));
		List<Sha1SyncPositionHash> hashes = new ArrayList<Sha1SyncPositionHash>();
		hashes.add(new Sha1SyncPositionHash().position("").summary(sha1OfSha1.toString()));
		Sha1SyncJson sync = new Sha1SyncJson().level(0).max(1).hashes(hashes);
		
		List<Expression> args = new ArrayList<Expression>();
		args.add(ff.literal("-all"));
		args.add(ff.literal(new Gson().toJson(sync)));
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction();
		func.setParameters(args);
		Object result;
		
		result = func.evaluate(feature());
		assertEquals("false", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(0)).toString());
		
		result = func.evaluate(feature());
		assertEquals("false", result.toString());
		assertEquals(2, func.getFeatureSha1s().size());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(0)).toString());

		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(1)).toString());
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
	
    @Test
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
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction();
		func.setParameters(args);
		Object result;
		
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		assertEquals("db90c50160c32f3517b7d3b1c78be70a7f2ba992", v1(func.getFeatureSha1s().get(0)).toString());
		
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(2, func.getFeatureSha1s().size());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(1)).toString());
	}
	
	private Sha1Value v1(IdAndValueSha1s pair) {
		return VersionFeatures.VERSION1.getBucketPrefixSha1(pair);
	}
	
    @Test
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
		Sha1SyncFilterFunction func = new Sha1SyncFilterFunction();
		func.setParameters(args);
		Object result;
		
		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(1, func.getFeatureSha1s().size());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(0)).toString());

		result = func.evaluate(feature());
		assertEquals("true", result.toString());
		assertEquals(2, func.getFeatureSha1s().size());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(0)).toString());
		assertEquals(EXPECTED_SHA1, v1(func.getFeatureSha1s().get(1)).toString());
	}
	
	private Object feature() throws ParseException {
		return m_samples.buildSimpleFeature("FID1");
	}

}

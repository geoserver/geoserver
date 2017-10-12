/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.styling.Rule;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

public class ClassifierTestSupport extends SLDServiceBaseTest {

	private static final int DEFAULT_INTERVALS = 2;

	protected SimpleFeatureCollection pointCollection, lineCollection;

	FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

	protected SimpleFeatureType dataType;

	protected SimpleFeature[] testFeatures;

//	public void testClassifyForFeatureDefault() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "foo");
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
//				DEFAULT_INTERVALS);
//		checkRule(rules[0], "#680000", org.opengis.filter.And.class);
//		checkRule(rules[1], "#B20000", org.opengis.filter.And.class);
//	}
//
//	public void testClassifyOpenRange() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "id");
//		attributes.put("intervals", 3);
//		attributes.put("open", true);
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//		checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsLessThanOrEqualTo.class);
//		checkRule(rules[1], "#8C0000", org.opengis.filter.And.class);
//		checkRule(rules[2], "#C30000", org.opengis.filter.PropertyIsGreaterThan.class);
//	}
//
//	public void testQuantile() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "foo");
//		attributes.put("intervals", 3);
//		attributes.put("open", true);
//		attributes.put("method", "quantile");
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		assertTrue(rules[0].getTitle().contains("20.0"));
//		assertTrue(rules[1].getTitle().contains("20.0"));
//		assertTrue(rules[1].getTitle().contains("61.0"));
//		assertTrue(rules[2].getTitle().contains("61.0"));
//	}
//
//	public void testJenks() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "foo");
//		attributes.put("intervals", 3);
//		attributes.put("open", true);
//		attributes.put("method", "jenks");
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		assertTrue(rules[0].getTitle().contains("12.0"));
//		assertTrue(rules[1].getTitle().contains("12.0"));
//		assertTrue(rules[1].getTitle().contains("29.0"));
//		assertTrue(rules[2].getTitle().contains("29.0"));
//	}
//
//	public void testEqualInterval() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "foo");
//		attributes.put("intervals", 3);
//		attributes.put("open", true);
//		attributes.put("method", "equalInterval");
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		assertTrue(rules[0].getTitle().contains("32.6"));
//		assertTrue(rules[1].getTitle().contains("32.6"));
//		assertTrue(rules[1].getTitle().contains("61.3"));
//		assertTrue(rules[2].getTitle().contains("61.3"));
//	}
//
//	public void testUnique() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "name");
//		attributes.put("intervals", 3);
//		attributes.put("method", "uniqueInterval");
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[1], "#8C0000", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[2], "#C30000", org.opengis.filter.PropertyIsEqualTo.class);
//		TreeSet<String> orderedRules = new TreeSet<String>();
//		orderedRules.add(rules[0].getTitle());
//		orderedRules.add(rules[1].getTitle());
//		orderedRules.add(rules[2].getTitle());
//		Iterator iter = orderedRules.iterator();
//		assertEquals("bar", iter.next());
//		assertEquals("foo", iter.next());
//		assertEquals("foobar", iter.next());
//	}
//
//	public void testBlueRamp() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "name");
//		attributes.put("intervals", 3);
//		attributes.put("method", "uniqueInterval");
//		attributes.put("ramp", "blue");
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		checkRule(rules[0], "#000055", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[1], "#00008C", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[2], "#0000C3", org.opengis.filter.PropertyIsEqualTo.class);
//
//	}
//
//	public void testReverse() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "name");
//		attributes.put("intervals", 3);
//		attributes.put("method", "uniqueInterval");
//		attributes.put("ramp", "blue");
//		attributes.put("reverse", true);
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		checkRule(rules[0], "#0000C3", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[1], "#00008C", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[2], "#000055", org.opengis.filter.PropertyIsEqualTo.class);
//
//	}
//
//	public void testNormalize() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "id");
//		attributes.put("intervals", 3);
//		attributes.put("open", true);
//		attributes.put("normalize", true);
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsLessThanOrEqualTo.class);
//		org.opengis.filter.PropertyIsLessThanOrEqualTo filter = (org.opengis.filter.PropertyIsLessThanOrEqualTo) rules[0]
//				.getFilter();
//		assertTrue(filter.getExpression1() instanceof FilterFunction_parseDouble);
//	}
//
//	public void testCustomRamp() throws IOException {
//		attributes.put("layer", FEATURETYPE_LAYER);
//		attributes.put("attribute", "name");
//		attributes.put("intervals", 3);
//		attributes.put("method", "uniqueInterval");
//		attributes.put("ramp", "custom");
//		attributes.put("startColor", "0xFF0000");
//		attributes.put("endColor", "0x0000FF");
//
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
//
//		checkRule(rules[0], "#FF0000", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[1], "#AA0055", org.opengis.filter.PropertyIsEqualTo.class);
//		checkRule(rules[2], "#5500AA", org.opengis.filter.PropertyIsEqualTo.class);
//
//	}
//
//	private void checkRule(Rule rule, String color, Class<?> filterType) {
//		assertNotNull(rule.getFilter());
//		assertTrue(filterType.isAssignableFrom(rule.getFilter().getClass()));
//		assertNotNull(rule.getSymbolizers());
//		assertEquals(1, rule.getSymbolizers().length);
//		assertTrue(rule.getSymbolizers()[0] instanceof PolygonSymbolizer);
//		PolygonSymbolizer symbolizer = (PolygonSymbolizer) rule.getSymbolizers()[0];
//		assertNotNull(symbolizer.getFill());
//		assertEquals(color, symbolizer.getFill().getColor().toString());
//	}
//
//	public void testClassifyForCoverageIsEmpty() throws IOException {
//
//		attributes.put("layer", COVERAGE_LAYER);
//		initRequestUrl(request, "xml");
//		resource.handleGet();
//		assertNotNull(responseEntity);
//		assertTrue(responseEntity instanceof Representation);
//		Representation representation = (Representation) responseEntity;
//		String resultXml = representation.getText().replace("\r", "").replace("\n", "");
//		assertEquals("<list/>", resultXml);
//	}

	private Rule[] checkRules(String resultXml, int classes) {
		Rule[] rules = checkSLD(resultXml);
		assertEquals(classes, rules.length);
		return rules;
	}

	@Override
	protected String getServiceUrl() {
		return "classify";
	}
}
/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.geoxacml.test;

import java.io.FileInputStream;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.geotools.xacml.test.TestSupport;

import com.sun.xacml.PDP;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

/**
 * @author Christian Mueller
 * 
 *         Test for topological functions
 * 
 */
public class TopologicalTest extends TestCase {

    public TopologicalTest() {
        super();

    }

    public TopologicalTest(String arg0) {
        super(arg0);

    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
    }

    public void testContains() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("topology", "ContainsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "ContainsRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testContains1() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("topology", "ContainsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "ContainsRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testWithin() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "WithinPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "WithinRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testWithin1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "WithinPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "WithinRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testOverlaps() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("topology", "OverlapsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "OverlapsRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testOverlaps1() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("topology", "OverlapsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "OverlapsRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIntersects() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology",
                "IntersectsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "IntersectsRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIntersects1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology",
                "IntersectsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "IntersectsRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testCrosses() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "CrossesPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "CrossesRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testCrosses1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "CrossesPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "CrossesRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testTouches() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "TouchesPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "TouchesRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testTouches1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "TouchesPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "TouchesRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testDisjoint() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("topology", "DisjointPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "DisjointRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testDisjoint1() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("topology", "DisjointPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "DisjointRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testEquals() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "EqualsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "EqualsRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testEquals1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("topology", "EqualsPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "topology", "EqualsRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

}

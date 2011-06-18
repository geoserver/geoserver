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
 *         Tests for geomtry construct functions
 */
public class ConstructTest extends TestCase {

    public ConstructTest() {
        super();

    }

    public ConstructTest(String arg0) {
        super(arg0);

    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
    }

    public void testBuffer() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct", "BufferPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "BufferRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testBuffer1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct", "BufferPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "BufferRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testUnion() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct", "UnionPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "UnionRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testUnion1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct", "UnionPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "UnionRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIntersection() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "IntersectionPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "IntersectionRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIntersection1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "IntersectionPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "IntersectionRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testDifference() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "DifferencePolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "DifferenceRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testDifference1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "DifferencePolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "DifferenceRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testSymDifference() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "SymDifferencePolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "SymDifferenceRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testSymDifference1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "SymDifferencePolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "SymDifferenceRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testBoundary() {

        PDP pdp = TestSupport.getPDP(TestSupport
                .getGeoXACMLFNFor("construct", "BoundaryPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "BoundaryRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testBoundary1() {

        PDP pdp = TestSupport.getPDP(TestSupport
                .getGeoXACMLFNFor("construct", "BoundaryPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "BoundaryRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testCentroid() {

        PDP pdp = TestSupport.getPDP(TestSupport
                .getGeoXACMLFNFor("construct", "CentroidPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "CentroidRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testCentroid1() {

        PDP pdp = TestSupport.getPDP(TestSupport
                .getGeoXACMLFNFor("construct", "CentroidPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "CentroidRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testConvexHull() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "ConvexHullPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "ConvexHullRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testConvexHull1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("construct",
                "ConvexHullPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "construct", "ConvexHullRequest1.xml")));
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

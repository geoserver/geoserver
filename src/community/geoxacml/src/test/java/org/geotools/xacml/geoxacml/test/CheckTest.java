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
 *         Test for gemotry check functions
 */
public class CheckTest extends TestCase {

    public CheckTest() {
        super();

    }

    public CheckTest(String arg0) {
        super(arg0);

    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
    }

    public void testIsClosed() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("check", "IsClosedPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "check", "IsClosedRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIsClosed1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("check", "IsClosedPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "check", "IsClosedRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIsSimple() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("check", "IsSimplePolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "check", "IsSimpleRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIsSimple1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("check", "IsSimplePolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "check", "IsSimpleRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIsValid() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("check", "IsValidPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "check", "IsValidRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testIsValid1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("check", "IsValidPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "check", "IsValidRequest1.xml")));
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

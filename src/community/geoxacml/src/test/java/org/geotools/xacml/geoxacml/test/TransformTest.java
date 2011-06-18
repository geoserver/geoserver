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
 *         Test for CRS transformations
 */
public class TransformTest extends TestCase {

    public TransformTest() {
        super();

    }

    public TransformTest(String arg0) {
        super(arg0);

    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
    }

    public void testEPSG_4326() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("transform",
                "EPSG_4326_Policy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "transform", "EPSG_4326_Request.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testEPSG_4326_1() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("transform",
                "EPSG_4326_Policy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "transform", "EPSG_4326_Request1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
    }

    public void testEPSG_4326_2() {

        PDP pdp = TestSupport.getPDP(TestSupport.getGeoXACMLFNFor("transform",
                "EPSG_4326_Policy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "transform", "EPSG_4326_Request2.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        ResponseCtx response = pdp.evaluate(request);
        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_INDETERMINATE);
        assertTrue(result.getStatus().getCode().iterator().next().equals(
                Status.STATUS_PROCESSING_ERROR));
    }

}

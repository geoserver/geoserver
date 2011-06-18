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
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.geotools.xacml.test.TestSupport;
import org.geotools.xacml.transport.XACMLLocalTransportFactory;
import org.geotools.xacml.transport.XACMLTransport;

import com.sun.xacml.PDP;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

/**
 * @author Christian Mueller
 * 
 *         Tests for bag functions
 * 
 */
public class XACMLTransporterTest extends TestCase {

    public XACMLTransporterTest() {
        super();

    }

    public XACMLTransporterTest(String arg0) {
        super(arg0);

    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
    }

    public void testXACMTransportSingleRequest() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("wildcard", "WildCardPolicy.xml"));

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "wildcard", "WildCardRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // serial

        XACMLTransport transport = new XACMLLocalTransportFactory(pdp, false).getXACMLTransport();
        ResponseCtx response = transport.evaluateRequestCtx(request);

        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));

        // multithreaded transporter, one request

        transport = new XACMLLocalTransportFactory(pdp, true).getXACMLTransport();
        response = transport.evaluateRequestCtx(request);

        result = (Result) response.getResults().iterator().next();
        assertTrue(result.getDecision() == Result.DECISION_PERMIT);
        assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));

    }

    public void testXACMLTransportMultipleRequestsSerial() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("wildcard", "WildCardPolicy.xml"));
        XACMLTransport transport = new XACMLLocalTransportFactory(pdp, false).getXACMLTransport();
        List<RequestCtx> requestList = createRequestList();
        List<ResponseCtx> responseList = transport.evaluateRequestCtxList(requestList);
        checkResponseList(responseList);

    }

    public void testXACMLTransportMultipleRequestsMultiThreaded() {

        PDP pdp = TestSupport
                .getPDP(TestSupport.getGeoXACMLFNFor("wildcard", "WildCardPolicy.xml"));
        XACMLTransport transport = new XACMLLocalTransportFactory(pdp, true).getXACMLTransport();
        List<RequestCtx> requestList = createRequestList();
        List<ResponseCtx> responseList = transport.evaluateRequestCtxList(requestList);
        checkResponseList(responseList);

    }

    private void checkResponseList(List<ResponseCtx> responseList) {
        for (int i = 0; i < responseList.size(); i++) {
            ResponseCtx response = responseList.get(i);
            Result result = (Result) response.getResults().iterator().next();
            if (i % 2 == 0) {
                assertTrue(result.getDecision() == Result.DECISION_PERMIT);
                assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
            } else {
                assertTrue(result.getDecision() == Result.DECISION_NOT_APPLICABLE);
                assertTrue(result.getStatus().getCode().iterator().next().equals(Status.STATUS_OK));
            }

        }
    }

    private List<RequestCtx> createRequestList() {

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "wildcard", "WildCardRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        RequestCtx request1 = null;
        try {
            request1 = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "wildcard", "WildCardRequest1.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        List<RequestCtx> resultList = new ArrayList<RequestCtx>();
        for (int i = 0; i < 10; i++) {
            resultList.add(request);
            resultList.add(request1);
        }
        return resultList;
    }
}

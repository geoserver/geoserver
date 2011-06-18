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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.geotools.xacml.test.TestSupport;
import org.geotools.xacml.transport.XACMLHttpTransportFactory;
import org.geotools.xacml.transport.XACMLTransport;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

/**
 * @author Christian Mueller
 * 
 *         Tests for bag functions
 * 
 */
public class XACMLHttpTransporterTest extends TestCase {

    public XACMLHttpTransporterTest() {
        super();

    }

    public XACMLHttpTransporterTest(String arg0) {
        super(arg0);

    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
    }

    private String getPDPUrl() {
        try {
            // URL url = getClass().getResource("remotePDP.properties");
            BufferedReader in = new BufferedReader(new FileReader(
                    "src/test/resources/remotePDP.properties"));
            String urlString = in.readLine();
            return urlString;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getNoRemoteGeoserverMsg() {
        return "Test skipped, no remote geoserver running on " + getPDPUrl();
    }

    public void testXACMTransportSingleRequest() {

        RequestCtx request = null;
        try {
            request = RequestCtx.getInstance(new FileInputStream(TestSupport.getGeoXACMLFNFor(
                    "wildcard", "WildCardRequest.xml")));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // serial
        ResponseCtx response = null;
        XACMLTransport transport = new XACMLHttpTransportFactory(getPDPUrl(), false)
                .getXACMLTransport();
        try {
            response = transport.evaluateRequestCtx(request);
        } catch (RuntimeException rtex) {
            if (rtex.getCause() instanceof ConnectException) {
                Logger.getAnonymousLogger().info(getNoRemoteGeoserverMsg());
                return;
            }

        }

        Result result = (Result) response.getResults().iterator().next();
        assertTrue(result != null);

        // multithreaded transporter, one request

        transport = new XACMLHttpTransportFactory(getPDPUrl(), true).getXACMLTransport();
        try {
            response = transport.evaluateRequestCtx(request);
        } catch (RuntimeException rtex) {
            if (rtex.getCause() instanceof ConnectException) {
                Logger.getAnonymousLogger().info(getNoRemoteGeoserverMsg());
                return;
            }

        }

        result = (Result) response.getResults().iterator().next();
        assertTrue(result != null);

    }

    public void testXACMLTransportMultipleRequestsSerial() {

        XACMLTransport transport = new XACMLHttpTransportFactory(getPDPUrl(), false)
                .getXACMLTransport();
        List<RequestCtx> requestList = createRequestList();
        List<ResponseCtx> responseList = null;
        try {
            responseList = transport.evaluateRequestCtxList(requestList);
        } catch (RuntimeException rtex) {
            if (rtex.getCause() instanceof ConnectException) {
                Logger.getAnonymousLogger().info(getNoRemoteGeoserverMsg());
                return;
            }

        }

        checkResponseList(responseList);

    }

    public void testXACMLTransportMultipleRequestsMultiThreaded() {

        XACMLTransport transport = new XACMLHttpTransportFactory(getPDPUrl(), true)
                .getXACMLTransport();
        List<RequestCtx> requestList = createRequestList();
        List<ResponseCtx> responseList = null;
        try {
            responseList = transport.evaluateRequestCtxList(requestList);
        } catch (RuntimeException rtex) {
            if (rtex.getCause() instanceof ConnectException) {
                Logger.getAnonymousLogger().info(getNoRemoteGeoserverMsg());
                return;
            }

        }

        checkResponseList(responseList);

    }

    private void checkResponseList(List<ResponseCtx> responseList) {
        for (int i = 0; i < responseList.size(); i++) {
            ResponseCtx response = responseList.get(i);
            assertTrue(response != null);
            Result result = (Result) response.getResults().iterator().next();
            assertTrue(result != null);
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

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

package org.geotools.xacml.transport;

import java.util.ArrayList;
import java.util.List;

import com.sun.xacml.PDP;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * Transport Object for a local PDP. Since XACML requests are independent of each other, it is
 * possible to start each request of a request list as a single thread. This class is thread safe
 * 
 * @author Christian Muller
 * 
 */
public class XACMLLocalTransport extends XACMLAbstractTransport {
    /**
     * Thread class for evaluating a XACML request
     * 
     * @author Christian Mueller
     * 
     */
    public class LocalThread extends Thread {
        private RequestCtx requestCtx = null;;

        public RequestCtx getRequestCtx() {
            return requestCtx;
        }

        private ResponseCtx responseCtx = null;

        public ResponseCtx getResponseCtx() {
            return responseCtx;
        }

        LocalThread(RequestCtx requestCtx) {
            this.requestCtx = requestCtx;
        }

        @Override
        public void run() {
            responseCtx = pdp.evaluate(requestCtx);
        }

    }

    private PDP pdp;

    private boolean multiThreaded = false;

    public XACMLLocalTransport(PDP pdp, boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
        this.pdp = pdp;
    }

    public ResponseCtx evaluateRequestCtx(RequestCtx request) {
        log(request);
        ResponseCtx response = pdp.evaluate(request);
        log(response);
        return response;
    }

    public List<ResponseCtx> evaluateRequestCtxList(List<RequestCtx> requests) {
        if (multiThreaded)
            return evaluateRequestCtxListMultiThreaded(requests);
        else
            return evaluateRequestCtxListSerial(requests);
    }

    private List<ResponseCtx> evaluateRequestCtxListSerial(List<RequestCtx> requests) {
        List<ResponseCtx> resultList = new ArrayList<ResponseCtx>();
        for (RequestCtx request : requests) {
            log(request);
            ResponseCtx response = pdp.evaluate(request);
            log(response);
            resultList.add(response);

        }
        return resultList;
    }

    private List<ResponseCtx> evaluateRequestCtxListMultiThreaded(List<RequestCtx> requests) {
        List<ResponseCtx> resultList = new ArrayList<ResponseCtx>(requests.size());
        List<LocalThread> threadList = new ArrayList<LocalThread>(requests.size());

        if (requests.size() == 1) { // no threading for only one request
            resultList.add(evaluateRequestCtx(requests.get(0)));
            return resultList;
        }

        for (RequestCtx request : requests) {
            LocalThread t = new LocalThread(request);
            t.start();
            threadList.add(t);
        }
        for (LocalThread t : threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log(t.getRequestCtx());
            log(t.getResponseCtx());
            resultList.add(t.getResponseCtx());
        }
        return resultList;

    }

}

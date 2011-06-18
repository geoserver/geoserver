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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.Indenter;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * Transport Object for a remote PDP reachable by an http POST request. Since XACML requests are
 * independent of each other, it is possible to start each request of a request list as a single
 * thread. This class itself is threadsafe
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLHttpTransport extends XACMLAbstractTransport {
    /**
     * Thread class for evaluating a XACML request
     * 
     * @author Christian Mueller
     * 
     */

    private static InheritableThreadLocal<Map<String, ResponseCtx>> DigestMap = new InheritableThreadLocal<Map<String, ResponseCtx>>();;

    public class HttpThread extends Thread {
        private RequestCtx requestCtx = null;

        public RequestCtx getRequestCtx() {
            return requestCtx;
        }

        private ResponseCtx responseCtx = null;

        private RuntimeException runtimeException = null;

        public RuntimeException getRuntimeException() {
            return runtimeException;
        }

        public ResponseCtx getResponseCtx() {
            return responseCtx;
        }

        HttpThread(RequestCtx requestCtx) {
            this.requestCtx = requestCtx;
        }

        @Override
        public void run() {
            try {
                responseCtx = sendHttpPost(requestCtx);
            } catch (RuntimeException ex) {
                this.runtimeException = ex;
            }
        }

    }

    private URL pdpURL;

    private boolean multiThreaded = false;

    public XACMLHttpTransport(URL pdpURL, boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
        this.pdpURL = pdpURL;
    }

    public ResponseCtx evaluateRequestCtx(RequestCtx request) {
        initDigestMap();
        log(request);
        ResponseCtx response = sendHttpPost(request);
        log(response);
        return response;

    }

    public List<ResponseCtx> evaluateRequestCtxList(List<RequestCtx> requests) {
        initDigestMap();
        if (multiThreaded)
            return evaluateRequestCtxListMultiThreaded(requests);
        else
            return evaluateRequestCtxListSerial(requests);
    }

    private List<ResponseCtx> evaluateRequestCtxListSerial(List<RequestCtx> requests) {
        List<ResponseCtx> resultList = new ArrayList<ResponseCtx>();
        for (RequestCtx request : requests) {
            log(request);
            ResponseCtx response = sendHttpPost(request);
            log(response);
            resultList.add(response);
        }
        return resultList;
    }

    private List<ResponseCtx> evaluateRequestCtxListMultiThreaded(List<RequestCtx> requests) {
        List<ResponseCtx> resultList = new ArrayList<ResponseCtx>(requests.size());
        List<HttpThread> threadList = new ArrayList<HttpThread>(requests.size());

        if (requests.size() == 1) { // no threading for only one request
            resultList.add(evaluateRequestCtx(requests.get(0)));
            return resultList;
        }

        for (RequestCtx request : requests) {
            HttpThread t = new HttpThread(request);
            t.start();
            threadList.add(t);
        }
        for (HttpThread t : threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log(t.getRequestCtx());
            if (t.getRuntimeException() == null) {
                log(t.getResponseCtx());
                resultList.add(t.getResponseCtx());
            } else
                throw t.getRuntimeException();
        }
        return resultList;

    }

    private void initDigestMap() {
        if (DigestMap.get() == null)
            DigestMap.set(new HashMap<String, ResponseCtx>());
    }

    private ResponseCtx sendHttpPost(RequestCtx requestCtx) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        requestCtx.encode(bout, new Indenter(0), true);
        byte[] byteArray = bout.toByteArray();
        byte[] msgDigest = getDigestBytes(byteArray);

        if (msgDigest != null) {
            ResponseCtx responseCtx = DigestMap.get().get(new String(msgDigest));
            if (responseCtx != null) {
                return responseCtx;
            }
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) pdpURL.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-type", "text/xml, application/xml");
            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();
            out.write(byteArray);
            out.close();
            InputStream in = conn.getInputStream();
            ResponseCtx result = ResponseCtx.getInstance(in);
            in.close();
            if (msgDigest != null)
                DigestMap.get().put(new String(msgDigest), result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    byte[] getDigestBytes(byte[] bytes) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
                    "No MD5 Algorithm available");
            return null;
        }
        md.update(bytes);
        return md.digest();
    }

}

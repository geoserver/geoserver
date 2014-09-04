/*
 * Copyright (c) 2007 - 2013 OpenPlans
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * Modified by David Winslow <dwinslow@openplans.org> on 2007-12-13.
 */
package org.geoserver.filters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class GZIPResponseWrapper extends HttpServletResponseWrapper {
    protected HttpServletResponse origResponse = null;
    protected AlternativesResponseStream stream = null;
    protected PrintWriter writer = null;
    protected Set formatsToCompress;
    protected String requestedURL;
    protected Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.filters");

    public GZIPResponseWrapper(HttpServletResponse response, Set toCompress, String url) {
        super(response);
        requestedURL = url;
        origResponse = response;
        // TODO: allow user-configured format list here
        formatsToCompress = toCompress;
    }

    protected AlternativesResponseStream createOutputStream() throws IOException {
        return new AlternativesResponseStream(origResponse, formatsToCompress);
    }

    public void setContentType(String type){
//        if (stream != null && stream.isDirty()){
//            logger.warning("Setting mimetype after acquiring stream! was:" +
//                    getContentType() + "; set to: " + type + "; url was: " + requestedURL); 
//        }
        origResponse.setContentType(type);
    }

    public void finishResponse() {
        try {
            if (writer != null) {
                writer.close();
            } else {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {}
    }

    public void flushBuffer() throws IOException {
        getResponse().flushBuffer();
        if (writer!= null){
            writer.flush();
        } else if (stream != null) {
            stream.flush();
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called!");
        }

        if (stream == null)
            stream = createOutputStream();
        return (stream);
    }

    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return (writer);
        }

        if (stream != null) {
            throw new IllegalStateException("getOutputStream() has already been called!");
        }

        stream = createOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
        return (writer);
    }

    public void setContentLength(int length) {}
}

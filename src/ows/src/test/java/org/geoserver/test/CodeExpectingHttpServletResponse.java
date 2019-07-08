/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * HttpServletResponse wrapper to help in making assertions about expected status codes.
 *
 * @author David Winslow, OpenGeo
 */
public class CodeExpectingHttpServletResponse extends HttpServletResponseWrapper {
    private int myErrorCode;
    private boolean error;

    public CodeExpectingHttpServletResponse(HttpServletResponse req) {
        super(req);
        myErrorCode = 200;
    }

    public void setStatus(int sc) {
        myErrorCode = sc;
        super.setStatus(sc);
    }

    @SuppressWarnings("deprecation")
    public void setStatus(int sc, String sm) {
        myErrorCode = sc;
        super.setStatus(sc, sm);
    }

    public void sendError(int sc) throws IOException {
        error = true;
        myErrorCode = sc;
        super.sendError(sc);
    }

    public void sendError(int sc, String sm) throws IOException {
        error = true;
        myErrorCode = sc;
        super.sendError(sc, sm);
    }

    public int getErrorCode() {
        return myErrorCode;
    }

    public int getStatusCode() {
        return myErrorCode;
    }

    public boolean isError() {
        return error;
    }
}

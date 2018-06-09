/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class TimeoutCancellingResponse extends HttpServletResponseWrapper {
    TimeoutVerifier timeoutVerifier;

    public TimeoutCancellingResponse(
            HttpServletResponse response, TimeoutVerifier timeoutVerifier) {
        super(response);
        this.timeoutVerifier = timeoutVerifier;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        ServletOutputStream delegate = super.getOutputStream();
        return new TimeoutCancellingStream(timeoutVerifier, delegate);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(getOutputStream());
    }
}

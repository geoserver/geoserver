/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A utility class to handle HTTP responses in a servlet environment, specifically for writing single/multipart
 * responses.
 *
 * <p>This class provides methods to begin a part with a specified content type and to obtain the servlet output stream
 * for writing data.
 */
@SuppressWarnings("PMD.CloseResource") // output is managed by the servlet
public class HttpResponseWriter {

    /**
     * Factory method to create an instance of HttpResponseWriter or MultipartResponseWriter based on the number of
     * outputs expected.
     */
    public static HttpResponseWriter getResponseWriter(HttpServletResponse response, int outputCount)
            throws IOException {
        if (outputCount > 1) {
            return new MultipartResponseWriter(response);
        } else {
            return new HttpResponseWriter(response);
        }
    }

    private final HttpServletResponse response;
    private ServletOutputStream outputStream;

    public HttpResponseWriter(HttpServletResponse response) {
        this.response = response;
    }

    public void beginPart(String contentType, String partId) throws IOException {
        response.setContentType(contentType);
    }

    public void endResponse() throws IOException {
        // nothing to do here
    }

    public ServletOutputStream getServletOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = response.getOutputStream();
        }
        return outputStream;
    }

    public static class MultipartResponseWriter extends HttpResponseWriter {
        private static final String CRLF = "\r\n";
        private static final String BOUNDARY = "boundary-927fd34c-2d13-46d3-9c61-53335183f341";
        boolean firstPart = true;

        public MultipartResponseWriter(HttpServletResponse response) throws IOException {
            super(response);
            response.setContentType("multipart/related; boundary=" + BOUNDARY);
        }

        @Override
        public void beginPart(String contentType, String partId) throws IOException {
            ServletOutputStream out = getServletOutputStream();
            if (firstPart) {
                firstPart = false;
            } else {
                out.print(CRLF);
            }

            out.print("--" + BOUNDARY + CRLF);
            out.print("Content-Type: " + contentType + CRLF);
            out.print("Content-ID: <%s>".formatted(partId) + CRLF);

            out.print(CRLF);
        }

        @Override
        public void endResponse() throws IOException {
            ServletOutputStream out = getServletOutputStream();
            out.println(CRLF + "--" + BOUNDARY + "--");
            out.flush();
        }
    }
}

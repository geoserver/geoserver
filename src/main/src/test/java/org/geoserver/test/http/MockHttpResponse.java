/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.geotools.data.ows.HTTPResponse;

/**
 * Helper class to mock HTTP responses
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MockHttpResponse implements HTTPResponse {

    String contentType;

    Map<String, String> headers;

    byte[] response;

    String responseCharset;

    public MockHttpResponse(String response, String contentType, String... headers) {
        this(response.getBytes(), contentType, headers);
    }

    public MockHttpResponse(URL response, String contentType, String... headers)
            throws IOException {
        this(IOUtils.toByteArray(response.openStream()), contentType, headers);
    }

    public MockHttpResponse(byte[] response, String contentType, String... headers) {
        this.response = response;
        this.contentType = contentType;
        this.headers = new HashMap<String, String>();

        if (headers != null) {
            if (headers.length % 2 != 0) {
                throw new IllegalArgumentException(
                        "The headers must be a alternated sequence of keys "
                                + "and values, should have an even number of entries");
            }

            for (int i = 0; i < headers.length; i += 2) {
                String key = headers[i];
                String value = headers[i++];
                this.headers.put(key, value);
            }
        }
    }

    public void dispose() {
        // nothing to do
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getResponseHeader(String headerName) {
        return headers.get(headerName);
    }

    public InputStream getResponseStream() throws IOException {
        return new ByteArrayInputStream(response);
    }

    /**
     * @return {@code null}
     * @see org.geotools.data.ows.HTTPResponse#getResponseCharset()
     */
    @Override
    public String getResponseCharset() {
        return responseCharset;
    }

    @Override
    public String toString() {
        String contents = null;
        if (responseCharset != null) {
            contents = new String(response, Charset.forName(responseCharset));
        } else {
            contents = new String(response);
        }
        return contentType + " - " + contents;
    }

    public void setResponseCharset(String responseCharset) {
        this.responseCharset = responseCharset;
    }
}

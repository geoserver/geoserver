/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.commons.io.IOUtils;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.data.ows.HTTPResponse;

/**
 * A simple mock http client, allows to set expectations on requests and provide canned responses on
 * them
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MockHttpClient extends AbstractHttpClient {

    Map<Request, HTTPResponse> expectedRequests =
            new LinkedHashMap<MockHttpClient.Request, HTTPResponse>();

    /**
     * Binds a certain URL to a response. The order of the query string parameters is not relevant,
     * the code will match the same set of KVP params regardless of their sequence and case of their
     * keys (from OGC specs, keys are case insensitive, values are case sensitive)
     */
    public void expectGet(URL url, HTTPResponse response) {
        expectedRequests.put(new Request(url), response);
    }

    /** Binds a certain POST request to a response. */
    public void expectPost(
            URL url, String postContent, String postContentType, HTTPResponse response) {
        expectPOST(url, postContent.getBytes(), postContentType, response);
    }

    public void expectPOST(
            URL url, byte[] postContent, String postContentType, HTTPResponse response) {
        expectedRequests.put(new Request(url, postContent, postContentType), response);
    }

    @Override
    public HTTPResponse post(URL url, InputStream postContent, String postContentType)
            throws IOException {
        return getResponse(new Request(url, toByteArray(postContent), postContentType));
    }

    private byte[] toByteArray(InputStream is) throws IOException {
        try {
            return IOUtils.toByteArray(is);
        } finally {
            is.close();
        }
    }

    private HTTPResponse getResponse(Request request) {
        HTTPResponse response = expectedRequests.get(request);
        if (response == null) {
            StringBuilder sb =
                    new StringBuilder(
                            "Unexpected request \n"
                                    + request
                                    + "\nNo response is bound to it. Bound urls are: ");
            for (Request r : expectedRequests.keySet()) {
                sb.append("\n").append(r);
            }
            throw new IllegalArgumentException(sb.toString());
        }
        return response;
    }

    @Override
    public HTTPResponse get(URL url) throws IOException {
        return getResponse(new Request(url));
    }

    private static class Request {
        String path;

        Map<String, Object> kvp;

        String contentType;

        boolean isGetRequest;

        byte[] postContent;

        public Request(URL url) {
            this.path = url.getProtocol() + "://" + url.getHost() + url.getPath();
            Map<String, Object> parsedQueryString = KvpUtils.parseQueryString(url.toExternalForm());
            // we use a treemap as it makes it easier to see what's missing when no bound url is
            // found
            this.kvp = new TreeMap<String, Object>();
            for (Entry<String, Object> entry : parsedQueryString.entrySet()) {
                this.kvp.put(entry.getKey().toUpperCase(), entry.getValue());
            }
            this.isGetRequest = true;
        }

        public Request(URL url, byte[] postContent, String postContentType) {
            this(url);
            this.isGetRequest = false;
            this.postContent = postContent;
            this.contentType = postContentType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
            result = prime * result + (isGetRequest ? 1231 : 1237);
            result = prime * result + ((kvp == null) ? 0 : kvp.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result + Arrays.hashCode(postContent);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Request other = (Request) obj;
            if (contentType == null) {
                if (other.contentType != null) return false;
            } else if (!contentType.equals(other.contentType)) return false;
            if (isGetRequest != other.isGetRequest) return false;
            if (kvp == null) {
                if (other.kvp != null) return false;
            } else if (!kvp.equals(other.kvp)) return false;
            if (path == null) {
                if (other.path != null) return false;
            } else if (!path.equals(other.path)) return false;
            if (!Arrays.equals(postContent, other.postContent)) return false;
            return true;
        }

        @Override
        public String toString() {
            if (isGetRequest) {
                return "GET " + path + ", " + kvp;
            } else {
                return "POST "
                        + path
                        + ", "
                        + kvp
                        + ", content type "
                        + contentType
                        + ", content "
                        + Arrays.toString(postContent);
            }
        }
    }
}

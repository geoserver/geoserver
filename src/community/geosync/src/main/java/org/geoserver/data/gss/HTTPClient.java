package org.geoserver.data.gss;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.util.Assert;

/**
 * A reentrant HTTP client
 * 
 * @author groldan
 * 
 */
public class HTTPClient {

    private final HttpClient client;

    /**
     * 
     * @param maxConnectionsPerHost
     * @param connectionTimeout
     * @param readTimeout
     */
    public HTTPClient(final int maxConnectionsPerHost, final int connectionTimeout,
            final int readTimeout) {

        Assert.isTrue(maxConnectionsPerHost > 0,
                "maxConnectionsPerHost shall be a positive integer");
        Assert.isTrue(connectionTimeout >= 0,
                "connectionTimeout shall be a positive integer or zero");
        Assert.isTrue(readTimeout >= 0, "readTimeout shall be a positive integer or zero");

        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
        connectionManager.getParams().setConnectionTimeout(connectionTimeout);
        connectionManager.getParams().setSoTimeout(readTimeout);
        client = new HttpClient(connectionManager);
    }

    /**
     * Sends an HTTP GET request to the given {@code url} with the provided (possibly empty or null)
     * request headers, and returns the response content as a string.
     * <p>
     * Example: <code>
     *  final String[] requestHeaders = { "Authorization", "Basic " + new String(Base64.encodeBase64((username + ":" + password).getBytes())) };
     *  HttpMedthod get = httpClient.get("http://geoserver.org/gss", requestHeaders);
     *  try{
     *   ....
     *  }finally{
     *   get.releaseConnection();
     *  }
     * </code>
     * 
     * @param url
     * @param requestHeaders
     * @return
     * @throws IOException
     */
    public GetMethod get(final String url, final String user, final String password)
            throws IOException {

        final String[] requestHeaders;
        if (user != null && password != null) {
            requestHeaders = new String[] { "Authorization",
                    "Basic " + new String(Base64.encodeBase64((user + ":" + password).getBytes())) };
        } else {
            requestHeaders = null;
        }

        GetMethod get = new GetMethod(url);
        get.setFollowRedirects(true);

        final int numHeaders = requestHeaders == null ? 0 : requestHeaders.length / 2;
        for (int i = 0; i < numHeaders; i++) {
            String headerName = requestHeaders[2 * i];
            String headerValue = requestHeaders[1 + 2 * i];
            get.addRequestHeader(headerName, headerValue);
        }

        final int status;

        status = client.executeMethod(get);
        if (status != 200) {
            throw new IOException("HTTP communication failed, status report is: " + status + ", "
                    + get.getStatusText());
        }
        return get;
    }
}

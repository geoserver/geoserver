/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderIterator;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;

/**
 * Mock connection manager, to emulate remote connections and get mocked data from them.
 *
 * @author maurobartolomeoli@gmail.com
 */
@SuppressWarnings("deprecation")
public class MockHttpClientConnectionManager implements HttpClientConnectionManager {
    static SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private String response;
    private boolean enableCache;
    private int connections = 0;

    public MockHttpClientConnectionManager(String response, boolean enableCache) {
        super();
        this.response = response;
        this.enableCache = enableCache;
    }

    @Override
    public void closeExpiredConnections() {}

    @Override
    public void closeIdleConnections(long arg0, TimeUnit arg1) {}

    @Override
    public void connect(HttpClientConnection arg0, HttpRoute arg1, int arg2, HttpContext arg3)
            throws IOException {}

    @Override
    public void releaseConnection(
            HttpClientConnection arg0, Object arg1, long arg2, TimeUnit arg3) {}

    public int getConnections() {
        return connections;
    }

    @Override
    public ConnectionRequest requestConnection(HttpRoute arg0, Object arg1) {
        return new ConnectionRequest() {

            @Override
            public boolean cancel() {
                return false;
            }

            @Override
            public HttpClientConnection get(long arg0, TimeUnit arg1)
                    throws InterruptedException, ExecutionException,
                            ConnectionPoolTimeoutException {
                connections++;
                return new HttpClientConnection() {

                    @Override
                    public void shutdown() throws IOException {}

                    @Override
                    public void setSocketTimeout(int arg0) {}

                    @Override
                    public boolean isStale() {
                        return false;
                    }

                    @Override
                    public boolean isOpen() {
                        return true;
                    }

                    @Override
                    public int getSocketTimeout() {
                        return 0;
                    }

                    @Override
                    public HttpConnectionMetrics getMetrics() {
                        return null;
                    }

                    @Override
                    public void close() throws IOException {}

                    @Override
                    public void sendRequestHeader(HttpRequest arg0)
                            throws HttpException, IOException {}

                    @Override
                    public void sendRequestEntity(HttpEntityEnclosingRequest arg0)
                            throws HttpException, IOException {}

                    @Override
                    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
                        return new HttpResponse() {

                            List<Header> headers = new ArrayList<Header>();

                            @Override
                            public void addHeader(Header arg0) {}

                            @Override
                            public void addHeader(String arg0, String arg1) {}

                            @Override
                            public boolean containsHeader(String arg0) {
                                return false;
                            }

                            @Override
                            public Header[] getAllHeaders() {
                                return headers.toArray(new Header[] {});
                            }

                            public Header getHeader(String header) {
                                if ("transfer-encoding".equalsIgnoreCase(header)) {
                                    return new BasicHeader(header, "identity");
                                }
                                if ("date".equalsIgnoreCase(header)) {

                                    return new BasicHeader(
                                            header,
                                            dateFormat.format(new GregorianCalendar().getTime()));
                                }
                                if ("cache-control".equalsIgnoreCase(header)) {
                                    return new BasicHeader(
                                            header, enableCache ? "public" : "no-cache");
                                }
                                if ("content-length".equalsIgnoreCase(header)) {
                                    return new BasicHeader(header, response.length() + "");
                                }
                                if ("content-encoding".equalsIgnoreCase(header)) {
                                    return new BasicHeader(header, "identity");
                                }
                                if ("age".equalsIgnoreCase(header)) {
                                    return new BasicHeader(header, "0");
                                }
                                if ("expires".equalsIgnoreCase(header) && enableCache) {
                                    GregorianCalendar expires = new GregorianCalendar();
                                    expires.add(GregorianCalendar.MINUTE, 30);
                                    return new BasicHeader(
                                            header, dateFormat.format(expires.getTime()));
                                }
                                return new BasicHeader(header, "");
                            }

                            @Override
                            public Header getFirstHeader(String header) {
                                Header value = getHeader(header);
                                headers.add(value);
                                return value;
                            }

                            @Override
                            public Header[] getHeaders(String header) {

                                return new Header[] {getFirstHeader(header)};
                            }

                            @Override
                            public Header getLastHeader(String header) {
                                return new BasicHeader(header, "");
                            }

                            @Override
                            public org.apache.http.params.HttpParams getParams() {
                                // TODO Auto-generated method stub
                                return null;
                            }

                            @Override
                            public ProtocolVersion getProtocolVersion() {
                                return HttpVersion.HTTP_1_1;
                            }

                            @Override
                            public HeaderIterator headerIterator() {
                                return new BasicHeaderIterator(
                                        headers.toArray(new Header[] {}), "mock");
                            }

                            @Override
                            public HeaderIterator headerIterator(String header) {
                                return new BasicHeaderIterator(
                                        headers.toArray(new Header[] {}), "mock");
                            }

                            @Override
                            public void removeHeader(Header arg0) {}

                            @Override
                            public void removeHeaders(String arg0) {}

                            @Override
                            public void setHeader(Header arg0) {}

                            @Override
                            public void setHeader(String arg0, String arg1) {}

                            @Override
                            public void setHeaders(Header[] arg0) {}

                            @Override
                            public void setParams(org.apache.http.params.HttpParams arg0) {}

                            @Override
                            public HttpEntity getEntity() {
                                BasicHttpEntity entity = new BasicHttpEntity();
                                entity.setContentLength(response.length());
                                entity.setContent(
                                        new ByteArrayInputStream(
                                                response.getBytes(StandardCharsets.UTF_8)));
                                return entity;
                            }

                            @Override
                            public Locale getLocale() {
                                return Locale.ENGLISH;
                            }

                            @Override
                            public StatusLine getStatusLine() {
                                return new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
                            }

                            @Override
                            public void setEntity(HttpEntity arg0) {}

                            @Override
                            public void setLocale(Locale arg0) {}

                            @Override
                            public void setReasonPhrase(String arg0) throws IllegalStateException {}

                            @Override
                            public void setStatusCode(int arg0) throws IllegalStateException {}

                            @Override
                            public void setStatusLine(StatusLine arg0) {}

                            @Override
                            public void setStatusLine(ProtocolVersion arg0, int arg1) {}

                            @Override
                            public void setStatusLine(
                                    ProtocolVersion arg0, int arg1, String arg2) {}
                        };
                    }

                    @Override
                    public void receiveResponseEntity(HttpResponse arg0)
                            throws HttpException, IOException {}

                    @Override
                    public boolean isResponseAvailable(int arg0) throws IOException {
                        return true;
                    }

                    @Override
                    public void flush() throws IOException {}
                };
            }
        };
    }

    @Override
    public void routeComplete(HttpClientConnection arg0, HttpRoute arg1, HttpContext arg2)
            throws IOException {}

    @Override
    public void shutdown() {}

    @Override
    public void upgrade(HttpClientConnection arg0, HttpRoute arg1, HttpContext arg2)
            throws IOException {}
}

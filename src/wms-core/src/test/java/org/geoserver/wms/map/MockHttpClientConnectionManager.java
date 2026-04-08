/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 * Mock connection manager, to emulate remote connections and get mocked data from them.
 *
 * @author maurobartolomeoli@gmail.com
 */
@SuppressWarnings("deprecation")
public class MockHttpClientConnectionManager implements HttpClientConnectionManager {

    static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        DATE_FMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final String response;
    private final boolean enableCache;
    private int connections = 0;

    public MockHttpClientConnectionManager(String response, boolean enableCache) {
        this.response = response;
        this.enableCache = enableCache;
    }

    public int getConnections() {
        return connections;
    }

    // ---- HttpClientConnectionManager (HC5) ----

    @Override
    public LeaseRequest lease(String id, HttpRoute route, Timeout requestTimeout, Object state) {
        // Return a LeaseRequest whose get(...) yields our fake endpoint.
        return new LeaseRequest() {
            private volatile boolean cancelled = false;

            @Override
            public ConnectionEndpoint get(Timeout timeout) throws InterruptedException, CancellationException {
                if (cancelled) {
                    throw new CancellationException("lease request cancelled");
                }
                connections++;
                return new MockEndpoint();
            }

            @Override
            public boolean cancel() {
                cancelled = true;
                return true;
            }
        };
    }

    @Override
    public void connect(ConnectionEndpoint endpoint, TimeValue connectTimeout, HttpContext context) throws IOException {
        // No-op: our mock endpoint is always "connected" from the test's POV.
    }

    @Override
    public void upgrade(ConnectionEndpoint endpoint, HttpContext context) throws IOException {
        // No-op for mock
    }

    @Override
    public void release(ConnectionEndpoint endpoint, Object newState, TimeValue validDuration) {
        // Close quietly
        endpoint.close(CloseMode.GRACEFUL);
    }

    @Override
    public void close(CloseMode closeMode) {
        // No-op for mock
    }

    @Override
    public void close() throws IOException {
        // No-op for mock
    }

    // ---- Minimal mock endpoint that executes requests by returning a fabricated response ----

    private final class MockEndpoint extends ConnectionEndpoint {

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void setSocketTimeout(Timeout timeout) {
            // no-op
        }

        @Override
        public ClassicHttpResponse execute(
                String id, ClassicHttpRequest request, HttpRequestExecutor executor, HttpContext context)
                throws IOException, HttpException {
            return null;
        }

        @Override
        public ClassicHttpResponse execute(
                String id,
                ClassicHttpRequest request,
                ConnectionEndpoint.RequestExecutor requestExecutor,
                HttpContext context)
                throws IOException, HttpException {

            // Build a basic 200 OK with the same headers your old mock produced
            BasicClassicHttpResponse resp = new BasicClassicHttpResponse(200, "OK");
            resp.setVersion(HttpVersion.HTTP_1_1);

            // Required headers
            addHeader(resp, "transfer-encoding", "identity");
            addHeader(resp, "date", DATE_FMT.format(new GregorianCalendar().getTime()));
            addHeader(resp, "cache-control", enableCache ? "public" : "no-cache");
            addHeader(resp, "content-encoding", "identity");
            addHeader(resp, "age", "0");

            if (enableCache) {
                GregorianCalendar expires = new GregorianCalendar();
                expires.add(GregorianCalendar.MINUTE, 30);
                addHeader(resp, "expires", DATE_FMT.format(expires.getTime()));
            }

            // Entity
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            BasicHttpEntity entity =
                    new BasicHttpEntity(new ByteArrayInputStream(bytes), bytes.length, ContentType.DEFAULT_BINARY);
            resp.setEntity(entity);

            addHeader(resp, "content-length", Long.toString(bytes.length));

            return resp;
        }

        private void addHeader(BasicClassicHttpResponse r, String name, String value) {
            r.addHeader((Header) new BasicHeader(name, value));
        }

        @Override
        public void close() throws IOException {
            // nothing to do
        }

        @Override
        public void close(CloseMode closeMode) {
            // nothing to do
        }
    }
}

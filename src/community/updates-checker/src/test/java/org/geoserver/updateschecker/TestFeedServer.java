/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.platform.GeoServerExtensionsHelper;

/**
 * A local HTTPS server that serves the feed at {@code /versions.json}, and points the module's feed URL at itself.
 * {@link #start(String)} serves a fixed body with a status the test can change; {@link #start(HttpHandler)} lets the
 * test answer every request itself.
 */
public final class TestFeedServer {

    public static final String PATH = "/versions.json";

    private final HttpsServer server;
    private final boolean systemProperty;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private volatile int status = 200;
    private volatile byte[] body = new byte[0];

    private TestFeedServer(HttpsServer server, boolean systemProperty) {
        this.server = server;
        this.systemProperty = systemProperty;
    }

    /** Serves {@code body} with HTTP 200 until {@link #setStatus} or {@link #setBody} says otherwise. */
    public static TestFeedServer start(String body) throws Exception {
        TestFeedServer feed = new TestFeedServer(TestFeedTls.server(), false);
        feed.setBody(body);
        feed.begin(feed::respond);
        return feed;
    }

    /** Hands every request to the handler, which must close the exchange. */
    public static TestFeedServer start(HttpHandler handler) throws Exception {
        TestFeedServer feed = new TestFeedServer(TestFeedTls.server(), false);
        feed.begin(handler);
        return feed;
    }

    /**
     * Like {@link #start(String)}, for a test that runs a whole GeoServer: the feed URL goes in as a system property,
     * which that application reads, not as a test override.
     */
    public static TestFeedServer startForApplication(String body) throws Exception {
        TestFeedServer feed = new TestFeedServer(TestFeedTls.server(), true);
        feed.setBody(body);
        feed.begin(feed::respond);
        return feed;
    }

    private void begin(HttpHandler handler) {
        server.createContext(PATH, handler);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "test-feed-server");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        if (systemProperty) {
            System.setProperty(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, url(PATH));
        } else {
            GeoServerExtensionsHelper.property(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, url(PATH));
        }
    }

    // HttpExchange is not AutoCloseable before Java 14, so the finally block cannot become try-with-resources
    @SuppressWarnings("PMD.UseTryWithResources")
    private void respond(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        try {
            byte[] bytes = body;
            boolean hasBody = status == 200 && bytes.length > 0;
            exchange.sendResponseHeaders(status, hasBody ? bytes.length : -1);
            if (hasBody) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
        } catch (IOException e) {
            // the client went away
        } finally {
            exchange.close();
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setBody(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
    }

    /** @return how many requests the fixed-body server has answered. */
    public int getRequests() {
        return requests.get();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    /** @return the https URL of a path on this server. */
    public String url(String path) {
        return TestFeedTls.url(server, path);
    }

    /** Stops listening but keeps the feed URL override, so the address now refuses connections. */
    public void close() {
        server.stop(0);
    }

    /** Stops the server and removes the feed URL override. */
    public void stop() {
        close();
        if (stopped.compareAndSet(false, true)) {
            TestFeedTls.serverStopped();
        }
        if (systemProperty) {
            System.clearProperty(UpdatesCheckerFeedClient.FEED_URL_PROPERTY);
        } else {
            GeoServerExtensionsHelper.clear();
        }
    }
}

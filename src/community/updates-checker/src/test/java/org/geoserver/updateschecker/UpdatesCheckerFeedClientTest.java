/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Exercises the feed client against a real local HTTP server, so redirects, ETags and size limits are real. */
public class UpdatesCheckerFeedClientTest {

    private TestFeedServer feedServer;
    private LogCapture log;

    private volatile int status = 200;
    private volatile String etag = "W/\"v1\"";
    private volatile byte[] body;
    private volatile String contentEncoding;
    private volatile String location;
    private volatile long declaredLength = -1;
    private volatile boolean streamBody = false;

    private final List<String> ifNoneMatchSeen = Collections.synchronizedList(new ArrayList<>());
    private final List<String> acceptEncodingSeen = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger requests = new AtomicInteger();

    @Before
    public void startServer() throws Exception {
        body = UpdatesCheckerFeedParserTest.sampleFeed();
        feedServer = TestFeedServer.start(this::handle);
        log = LogCapture.start();
    }

    @After
    public void stopServer() {
        feedServer.stop();
        log.stop();
    }

    // HttpExchange is not AutoCloseable before Java 14, so the finally block cannot become try-with-resources
    @SuppressWarnings("PMD.UseTryWithResources")
    private void handle(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        ifNoneMatchSeen.add(exchange.getRequestHeaders().getFirst("If-None-Match"));
        acceptEncodingSeen.add(exchange.getRequestHeaders().getFirst("Accept-Encoding"));
        try {
            if (status == 304) {
                exchange.sendResponseHeaders(304, -1);
                return;
            }
            if (location != null) {
                exchange.getResponseHeaders().add("Location", location);
            }
            if (etag != null) {
                exchange.getResponseHeaders().add("ETag", etag);
            }
            if (contentEncoding != null) {
                exchange.getResponseHeaders().add("Content-Encoding", contentEncoding);
            }
            byte[] bytes = body;
            if (streamBody) {
                exchange.sendResponseHeaders(status, 0);
                try (OutputStream out = exchange.getResponseBody()) {
                    byte[] chunk = new byte[64 * 1024];
                    java.util.Arrays.fill(chunk, (byte) ' ');
                    for (int sent = 0; sent < UpdatesCheckerFeedClient.MAX_BODY_BYTES + 200_000; sent += chunk.length) {
                        out.write(chunk);
                    }
                }
            } else if (declaredLength >= 0) {
                exchange.sendResponseHeaders(status, declaredLength);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(new byte[0]);
                }
            } else {
                exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) {
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                }
            }
        } finally {
            exchange.close();
        }
    }

    @Test
    public void fetchesAndParsesTheFeed() {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State before = client.getState();
        assertNull(before.getFeed());
        assertFalse(before.isAttempted());

        UpdatesCheckerFeedClient.State state = client.fetch();

        assertNotNull("the fetch failed: " + state.getLastError(), state.getFeed());
        assertEquals(12, state.getFeed().getSeries().size());
        assertTrue(state.isAttempted());
        assertFalse(state.isLastAttemptFailed());
        assertNull(state.getLastError());
        assertTrue(state.getCheckedAtMillis() > 0);
        assertSame(state, client.getState());
        assertEquals("the first request is unconditional", Collections.singletonList(null), ifNoneMatchSeen);
    }

    @Test
    public void noAcceptEncodingIsSent() {
        new UpdatesCheckerFeedClient().fetch();
        assertEquals(Collections.singletonList(null), acceptEncodingSeen);
    }

    @Test
    public void storedEtagIsReplayedVerbatimAndANotModifiedKeepsTheCachedFeed() throws Exception {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State first = client.fetch();

        status = 304;
        UpdatesCheckerFeedClient.State second = client.fetch();

        assertEquals(java.util.Arrays.asList((String) null, "W/\"v1\""), ifNoneMatchSeen);
        assertSame("a 304 keeps the cached body without re-parsing", first.getFeed(), second.getFeed());
        assertFalse(second.isLastAttemptFailed());
        assertTrue(second.getCheckedAtMillis() >= first.getCheckedAtMillis());
    }

    @Test
    public void aChangedFeedReplacesTheCachedOneAndItsEtag() {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State first = client.fetch();

        etag = "W/\"v2\"";
        UpdatesCheckerFeedClient.State second = client.fetch();
        assertNotSame(first.getFeed(), second.getFeed());

        client.fetch();
        assertEquals("W/\"v2\"", ifNoneMatchSeen.get(2));
    }

    @Test
    public void failureKeepsThePreviousFeedAndIsLoggedAtWarn() {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State good = client.fetch();

        status = 500;
        UpdatesCheckerFeedClient.State bad = client.fetch();

        assertTrue(bad.isLastAttemptFailed());
        assertSame(good.getFeed(), bad.getFeed());
        assertEquals("Version feed returned HTTP 500", bad.getLastError().base());
        assertEquals("the time of the last good copy is kept", good.getCheckedAtMillis(), bad.getCheckedAtMillis());
        assertTrue(log.warned("Could not fetch the version feed"));
    }

    @Test
    public void failureWithNothingCachedLeavesNoFeed() {
        status = 503;
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertNull(state.getFeed());
        assertTrue(state.isAttempted());
        assertTrue(state.isLastAttemptFailed());
    }

    @Test
    public void onlyHttp200And304AreAccepted() {
        for (int code : new int[] {201, 204, 404, 429, 500}) {
            status = code;
            UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
            assertTrue("HTTP " + code, state.isLastAttemptFailed());
            assertNull(state.getFeed());
        }
    }

    @Test
    public void redirectsAreNotFollowed() {
        status = 302;
        location = feedServer.url("/elsewhere");
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertTrue(state.isLastAttemptFailed());
        assertEquals("Version feed returned HTTP 302", state.getLastError().base());
        assertEquals("the redirect target must not be requested", 1, requests.get());
    }

    @Test
    public void aStreamedBodyOverTheLimitIsDiscarded() {
        streamBody = true;
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertTrue(state.isLastAttemptFailed());
        assertNull(state.getFeed());
        assertTrue(state.getLastError().base().contains("larger than"));
    }

    @Test
    public void aDeclaredLengthOverTheLimitIsRejectedWithoutReading() {
        declaredLength = UpdatesCheckerFeedClient.MAX_BODY_BYTES + 1L;
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertTrue(state.isLastAttemptFailed());
        assertTrue(state.getLastError().base().contains("larger than"));
    }

    @Test
    public void aCompressedResponseIsRejected() {
        contentEncoding = "gzip";
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertTrue(state.isLastAttemptFailed());
        assertTrue(state.getLastError().base().contains("Content-Encoding"));
    }

    @Test
    public void anExplicitIdentityEncodingIsFine() {
        contentEncoding = "identity";
        assertFalse(new UpdatesCheckerFeedClient().fetch().isLastAttemptFailed());
    }

    @Test
    public void unusableBodiesAreFailuresThatKeepTheCachedFeed() {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State good = client.fetch();

        for (String text : new String[] {"not json", "{}", "{\"schema_version\": 1}", "[1,2]"}) {
            body = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            etag = "W/\"bad\"";
            UpdatesCheckerFeedClient.State state = client.fetch();
            assertTrue(text, state.isLastAttemptFailed());
            assertSame(text, good.getFeed(), state.getFeed());
        }
        // the bad responses' ETags were not stored: the next request still carries the good one
        body = new byte[0];
        client.fetch();
        assertEquals("W/\"v1\"", ifNoneMatchSeen.get(ifNoneMatchSeen.size() - 1));
    }

    @Test
    public void aNewerSchemaIsKeptAsAFlaggedFeed() {
        body = "{\"schema_version\": 7}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertFalse(state.isLastAttemptFailed());
        assertTrue(state.getFeed().isSchemaTooNew());
    }

    @Test
    public void anUnreachableServerIsAFailureWithAOneLineWarning() {
        feedServer.close();
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State state = client.fetch();
        assertTrue(state.isLastAttemptFailed());
        assertNull(state.getFeed());
        for (LogRecord r : log.records()) {
            if (r.getLevel() == Level.WARNING) {
                assertNull("routine network trouble gets no stack trace", r.getThrown());
            }
        }
        assertTrue(log.warned("Could not fetch the version feed"));
    }

    @Test
    public void warningsFromTheFeedAreLoggedWithoutControlCharacters() {
        body = ("{\"schema_version\": 1, \"series\": [{\"series\": \"bad\\nline\"}]}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        new UpdatesCheckerFeedClient().fetch();
        assertTrue(log.warned("Version feed: series entry 0 is unreadable"));
        for (LogRecord r : log.records()) {
            assertFalse(r.getMessage().contains("\n"));
        }
    }

    @Test
    public void theFeedUrlCanBeOverriddenByAnApplicationProperty() {
        GeoServerExtensionsHelper.property(
                UpdatesCheckerFeedClient.FEED_URL_PROPERTY, "https://example.com/versions.json");
        assertEquals(
                "https://example.com/versions.json",
                new UpdatesCheckerFeedClient().getFeedUri().toString());
    }

    @Test
    public void withoutAnOverrideTheCompiledInDefaultIsUsed() {
        GeoServerExtensionsHelper.clear();
        String url = new UpdatesCheckerFeedClient().getFeedUri().toString();
        assertEquals("https://raw.githubusercontent.com/geoserver/geoserver/updates-checker-v1/versions.json", url);
    }

    @Test
    public void aBlankOrInvalidOverrideFallsBackToTheDefault() {
        String expected = "https://raw.githubusercontent.com/geoserver/geoserver/updates-checker-v1/versions.json";
        GeoServerExtensionsHelper.property(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, "not a valid uri");
        assertEquals(expected, new UpdatesCheckerFeedClient().getFeedUri().toString());
        GeoServerExtensionsHelper.property(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, "   ");
        assertEquals(expected, new UpdatesCheckerFeedClient().getFeedUri().toString());
    }

    @Test
    public void plainHttpIsNeverUsedWhateverTheHost() {
        String expected = "https://raw.githubusercontent.com/geoserver/geoserver/updates-checker-v1/versions.json";
        String[] refused = {
            "http://example.com/versions.json",
            "http://127.0.0.1:" + feedServer.getPort() + "/versions.json",
            "http://localhost/versions.json",
            "HTTP://example.com/versions.json",
            "ftp://example.com/versions.json",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "//example.com/versions.json",
            "https:///versions.json",
            "https://user:secret@example.com/versions.json"
        };
        for (String value : refused) {
            GeoServerExtensionsHelper.property(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, value);
            assertEquals(
                    value, expected, new UpdatesCheckerFeedClient().getFeedUri().toString());
        }
    }

    @Test
    public void aRefusedHttpOverrideIsLoggedAndNeverContacted() {
        GeoServerExtensionsHelper.property(
                UpdatesCheckerFeedClient.FEED_URL_PROPERTY,
                "http://127.0.0.1:" + feedServer.getPort() + "/versions.json");
        int before = requests.get();
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        // the compiled-in default is used instead; it is not reachable from a test, so the fetch fails quietly
        assertEquals("https", client.getFeedUri().getScheme());
        assertEquals("the plain http address was never contacted", before, requests.get());
        boolean warned = false;
        for (LogRecord r : log.records()) {
            warned |= r.getMessage().contains("only https URLs");
        }
        assertTrue("the refusal is logged", warned);
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Exercises the feed client against a local WireMock server. */
public class UpdatesCheckerFeedClientTest {

    private static final String DEFAULT_URL =
            "https://raw.githubusercontent.com/geoserver/geoserver/updates-checker-v1/versions.json";

    private TestFeedServer feedServer;
    private WireMockServer wireMock;
    private LogCapture log;

    @Before
    public void startServer() throws Exception {
        feedServer = TestFeedServer.start("");
        wireMock = feedServer.wireMock();
        serve(200, "W/\"v1\"", UpdatesCheckerFeedParserTest.sampleFeed());
        log = LogCapture.start();
    }

    @After
    public void stopServer() {
        feedServer.stop();
        log.stop();
    }

    /** Answers every request for the feed with this status, ETag and body. */
    private void serve(int status, String etag, byte[] body) {
        feedServer.respondWith(
                aResponse().withStatus(status).withHeader("ETag", etag).withBody(body));
    }

    private void serve(int status, String etag, String body) {
        serve(status, etag, body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private int requestsWith(String header, String value) {
        return wireMock.findAll(getRequestedFor(urlEqualTo(TestFeedServer.PATH)).withHeader(header, equalTo(value)))
                .size();
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
        wireMock.verify(
                1,
                getRequestedFor(urlEqualTo(TestFeedServer.PATH))
                        .withoutHeader("If-None-Match")
                        .withoutHeader("Accept-Encoding"));
    }

    @Test
    public void aNotModifiedAnswerToTheStoredEtagKeepsTheCachedFeed() {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State first = client.fetch();

        serve(304, "W/\"v1\"", "");
        UpdatesCheckerFeedClient.State second = client.fetch();

        assertEquals("the ETag is sent back exactly as received", 1, requestsWith("If-None-Match", "W/\"v1\""));
        assertSame("a 304 keeps the cached body without re-parsing", first.getFeed(), second.getFeed());
        assertFalse(second.isLastAttemptFailed());
        assertTrue(second.getCheckedAtMillis() >= first.getCheckedAtMillis());
    }

    @Test
    public void aChangedFeedReplacesTheCachedOneAndItsEtag() throws Exception {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State first = client.fetch();

        serve(200, "W/\"v2\"", UpdatesCheckerFeedParserTest.sampleFeed());
        UpdatesCheckerFeedClient.State second = client.fetch();
        assertNotSame(first.getFeed(), second.getFeed());

        client.fetch();
        assertEquals(1, requestsWith("If-None-Match", "W/\"v2\""));
    }

    @Test
    public void failureKeepsThePreviousFeedAndIsLoggedAtWarn() {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State good = client.fetch();

        feedServer.setStatus(500);
        UpdatesCheckerFeedClient.State bad = client.fetch();

        assertTrue(bad.isLastAttemptFailed());
        assertSame(good.getFeed(), bad.getFeed());
        assertEquals("Version feed returned HTTP 500", bad.getLastError().base());
        assertEquals("the time of the last good copy is kept", good.getCheckedAtMillis(), bad.getCheckedAtMillis());
        assertTrue(log.warned("Could not fetch the version feed"));
    }

    @Test
    public void failureWithNothingCachedLeavesNoFeed() {
        feedServer.setStatus(503);
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertNull(state.getFeed());
        assertTrue(state.isAttempted());
        assertTrue(state.isLastAttemptFailed());
    }

    @Test
    public void onlyHttp200And304AreAccepted() {
        for (int code : new int[] {201, 204, 404, 429, 500}) {
            feedServer.setStatus(code);
            UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
            assertTrue("HTTP " + code, state.isLastAttemptFailed());
            assertNull(state.getFeed());
        }
    }

    @Test
    public void redirectsAreNotFollowed() throws Exception {
        feedServer.respondWith(aResponse().withStatus(302).withHeader("Location", feedServer.url("/elsewhere")));
        wireMock.stubFor(get(urlEqualTo("/elsewhere"))
                .willReturn(aResponse().withBody(UpdatesCheckerFeedParserTest.sampleFeed())));

        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();

        assertTrue(state.isLastAttemptFailed());
        assertEquals("Version feed returned HTTP 302", state.getLastError().base());
        wireMock.verify(0, getRequestedFor(urlEqualTo("/elsewhere")));
    }

    @Test
    public void aBodyOverTheLimitIsDiscarded() {
        byte[] tooBig = new byte[UpdatesCheckerFeedClient.MAX_BODY_BYTES + 1];
        Arrays.fill(tooBig, (byte) ' ');
        serve(200, "W/\"big\"", tooBig);

        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();

        assertTrue(state.isLastAttemptFailed());
        assertNull(state.getFeed());
        assertTrue(state.getLastError().base().contains("larger than"));
    }

    @Test
    public void aCompressedResponseIsRejected() throws Exception {
        feedServer.respondWith(
                aResponse().withHeader("Content-Encoding", "gzip").withBody(UpdatesCheckerFeedParserTest.sampleFeed()));
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertTrue(state.isLastAttemptFailed());
        assertTrue(state.getLastError().base().contains("Content-Encoding"));
    }

    @Test
    public void anExplicitIdentityEncodingIsFine() throws Exception {
        feedServer.respondWith(aResponse()
                .withHeader("Content-Encoding", "identity")
                .withBody(UpdatesCheckerFeedParserTest.sampleFeed()));
        assertFalse(new UpdatesCheckerFeedClient().fetch().isLastAttemptFailed());
    }

    @Test
    public void unusableBodiesAreFailuresThatKeepTheCachedFeed() {
        UpdatesCheckerFeedClient client = new UpdatesCheckerFeedClient();
        UpdatesCheckerFeedClient.State good = client.fetch();

        for (String text : new String[] {"not json", "{}", "{\"schema_version\": 1}", "[1,2]"}) {
            serve(200, "W/\"bad\"", text);
            UpdatesCheckerFeedClient.State state = client.fetch();
            assertTrue(text, state.isLastAttemptFailed());
            assertSame(text, good.getFeed(), state.getFeed());
        }

        // the bad responses' ETags were not stored: the next request still carries the good one
        serve(200, "W/\"bad\"", "");
        client.fetch();
        assertEquals(0, requestsWith("If-None-Match", "W/\"bad\""));
        assertEquals(5, requestsWith("If-None-Match", "W/\"v1\""));
    }

    @Test
    public void aNewerSchemaIsKeptAsAFlaggedFeed() {
        serve(200, "W/\"v7\"", "{\"schema_version\": 7}");
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
        assertFalse(state.isLastAttemptFailed());
        assertTrue(state.getFeed().isSchemaTooNew());
    }

    @Test
    public void anUnreachableServerIsAFailureWithAOneLineWarning() {
        feedServer.close();
        UpdatesCheckerFeedClient.State state = new UpdatesCheckerFeedClient().fetch();
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
        feedServer.setBody("{\"schema_version\": 1, \"series\": [{\"series\": \"bad\\nline\"}]}");
        new UpdatesCheckerFeedClient().fetch();
        assertTrue(log.warned("Version feed: series entry 0 is unreadable"));
        for (LogRecord r : log.records()) {
            assertFalse(r.getMessage().contains("\n"));
        }
    }

    @Test
    public void theFeedUrlCanBeOverriddenByAnApplicationProperty() {
        for (String url : new String[] {"https://example.com/versions.json", "http://example.com:8080/versions.json"}) {
            GeoServerExtensionsHelper.property(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, url);
            assertEquals(url, new UpdatesCheckerFeedClient().getFeedUri().toString());
        }
    }

    @Test
    public void withoutAnOverrideTheCompiledInDefaultIsUsed() {
        GeoServerExtensionsHelper.clear();
        assertEquals(DEFAULT_URL, new UpdatesCheckerFeedClient().getFeedUri().toString());
    }

    @Test
    public void aBlankOrInvalidOverrideFallsBackToTheDefault() {
        GeoServerExtensionsHelper.property(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, "not a valid uri");
        assertEquals(DEFAULT_URL, new UpdatesCheckerFeedClient().getFeedUri().toString());
        GeoServerExtensionsHelper.property(UpdatesCheckerFeedClient.FEED_URL_PROPERTY, "   ");
        assertEquals(DEFAULT_URL, new UpdatesCheckerFeedClient().getFeedUri().toString());
    }

    @Test
    public void anOverrideThatIsNotAWebAddressIsRefusedAndLogged() {
        String[] refused = {
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
                    value,
                    DEFAULT_URL,
                    new UpdatesCheckerFeedClient().getFeedUri().toString());
        }
        assertTrue("the refusal is logged", log.warned("only http and https URLs"));
    }
}

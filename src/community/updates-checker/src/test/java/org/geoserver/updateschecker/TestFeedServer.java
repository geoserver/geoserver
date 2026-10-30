/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.geoserver.platform.GeoServerExtensionsHelper;

/**
 * A WireMock server on a free port that serves the feed at {@code /versions.json}, and points the module's feed URL at
 * itself.
 */
public final class TestFeedServer {

    public static final String PATH = "/versions.json";

    private final WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
    private final boolean systemProperty;
    private int status = 200;
    private String body;

    private TestFeedServer(String body, boolean systemProperty) {
        this.body = body;
        this.systemProperty = systemProperty;
    }

    /** Serves {@code body} with HTTP 200 until {@link #setStatus} or {@link #setBody} says otherwise. */
    public static TestFeedServer start(String body) {
        return new TestFeedServer(body, false).begin();
    }

    /**
     * Like {@link #start(String)}, for a test that runs a whole GeoServer: the feed URL goes in as a system property,
     * which that application reads, not as a test override.
     */
    public static TestFeedServer startForApplication(String body) {
        return new TestFeedServer(body, true).begin();
    }

    private TestFeedServer begin() {
        server.start();
        respond();
        if (systemProperty) {
            System.setProperty(FeedClient.FEED_URL_PROPERTY, url(PATH));
        } else {
            GeoServerExtensionsHelper.property(FeedClient.FEED_URL_PROPERTY, url(PATH));
        }
        return this;
    }

    private void respond() {
        respondWith(aResponse().withStatus(status).withBody(status == 200 ? body : ""));
    }

    /** Answers every request for the feed with this response, in place of the status and body. */
    public void respondWith(ResponseDefinitionBuilder response) {
        server.resetMappings();
        server.stubFor(get(urlEqualTo(PATH)).willReturn(response));
    }

    public void setStatus(int status) {
        this.status = status;
        respond();
    }

    public void setBody(String body) {
        this.body = body;
        respond();
    }

    /** @return how many requests the feed has received. */
    public int getRequests() {
        return server.findAll(getRequestedFor(urlEqualTo(PATH))).size();
    }

    public WireMockServer wireMock() {
        return server;
    }

    /** @return the http URL of a path on this server. */
    public String url(String path) {
        return "http://127.0.0.1:" + server.port() + path;
    }

    /** Stops listening but keeps the feed URL override, so the address now refuses connections. */
    public void close() {
        server.stop();
    }

    /** Stops the server and removes the feed URL override. */
    public void stop() {
        close();
        if (systemProperty) {
            System.clearProperty(FeedClient.FEED_URL_PROPERTY);
        } else {
            GeoServerExtensionsHelper.clear();
        }
    }
}

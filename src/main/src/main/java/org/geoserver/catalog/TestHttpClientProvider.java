/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geotools.data.ows.HTTPClient;

/**
 * Provides mock HTTP clients bound to the {@link #MOCKSERVER} address, to be used for testing.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TestHttpClientProvider {

    public static final String MOCKSERVER = "http://mock.test.geoserver.org";

    static final Map<String, HTTPClient> CLIENTS = new ConcurrentHashMap<String, HTTPClient>();

    private static boolean TEST_MODE = false;

    /** Binds the specified http client to the specified path */
    public static void bind(HTTPClient client, URL url) {
        bind(client, url.toExternalForm());
    }

    /** Binds the specified http client to the specified path */
    public static void bind(HTTPClient client, String url) {
        if (!url.startsWith(MOCKSERVER)) {
            throw new IllegalArgumentException("The URL must start with " + MOCKSERVER);
        }
        CLIENTS.put(url, client);
    }

    public static HTTPClient get(String url) {
        if (!url.startsWith(MOCKSERVER)) {
            throw new IllegalArgumentException(
                    "The url " + url + " does not start with " + MOCKSERVER);
        }
        if (!TEST_MODE) {
            throw new IllegalArgumentException("The provider is not in test mode now");
        }
        HTTPClient httpClient = CLIENTS.get(url);
        if (httpClient == null) {
            throw new IllegalArgumentException(
                    "The mock url "
                            + url
                            + " is not bound "
                            + "to any mock http client, current bindings are towards: "
                            + CLIENTS.keySet());
        }
        return httpClient;
    }

    /** Used to check if any binding is associated into the mock server */
    public static boolean testModeEnabled() {
        return TEST_MODE;
    }

    /** Clears all bindings to HTTPClient objects */
    public static void clear() {
        CLIENTS.clear();
    }

    /** Puts the mock HTTP client provider out of test mode */
    public static void endTest() {
        TEST_MODE = false;
        CLIENTS.clear();
    }

    /** Puts the provider in test mode */
    public static void startTest() {
        TEST_MODE = true;
    }
}

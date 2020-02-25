/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.net.URL;
import org.geotools.data.ows.HTTPClient;
import org.junit.rules.ExternalResource;

/**
 * Wraps TestHttpClientProvider as a JUnit rule for convenience
 *
 * @author Kevin Smith, Boundless
 */
public class TestHttpClientRule extends ExternalResource {

    public String getServer() {
        return TestHttpClientProvider.MOCKSERVER;
    }

    @Override
    protected void before() throws Throwable {
        TestHttpClientProvider.startTest();
    }

    @Override
    protected void after() {
        TestHttpClientProvider.endTest();
    }

    /** Binds the specified http client to the specified path */
    public void bind(HTTPClient client, URL url) {
        TestHttpClientProvider.bind(client, url);
    }

    /** Binds the specified http client to the specified path */
    public void bind(HTTPClient client, String url) {
        TestHttpClientProvider.bind(client, url);
    }
}

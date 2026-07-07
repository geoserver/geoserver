/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.LinkedHashMap;
import java.util.Map;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geowebcache.util.URLMangler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResponseUtilsURLManglerTest {

    private URLMangler urlMangler;

    @Before
    public void setUp() throws Exception {
        urlMangler = new ResponseUtilsURLMangler();
    }

    @Test
    public void testBuildURL() {
        String url = urlMangler.buildURL("http://foo.example.com", "/foo", "/bar");
        Assert.assertEquals("http://foo.example.com/foo/bar", url);
    }

    @Test
    public void testBuildTrailingSlashes() throws Exception {
        String url = urlMangler.buildURL("http://foo.example.com/", "/foo/", "/bar");
        Assert.assertEquals("http://foo.example.com/foo/bar", url);
    }

    @Test
    public void testBuildNoLeadingSlashes() throws Exception {
        String url = urlMangler.buildURL("http://foo.example.com/", "foo/", "bar");
        Assert.assertEquals("http://foo.example.com/foo/bar", url);
    }

    /**
     * Verifies that the 4-arg URL mangler keeps the returned URL path only while still allowing the query map to be
     * enriched by GeoServer URL manglers.
     */
    @Test
    public void testBuildURLWithQueryParameters() throws Exception {
        GeoServerExtensionsHelper.singleton(
                "projecttokenMangler",
                new org.geoserver.ows.URLMangler() {
                    @Override
                    public void mangleURL(
                            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
                        kvp.put("projecttoken", "abc123");
                    }
                },
                org.geoserver.ows.URLMangler.class);

        try {
            Map<String, String> queryParameters = new LinkedHashMap<>();
            queryParameters.put("format", "image/png");

            URLMangler.UrlAndParams result =
                    urlMangler.buildURL("http://foo.example.com/", "/foo/", "/bar", queryParameters);

            Assert.assertEquals("http://foo.example.com/foo/bar", result.url());
            Assert.assertEquals("image/png", result.queryParameters().get("format"));
            Assert.assertEquals("abc123", result.queryParameters().get("projecttoken"));
        } finally {
            GeoServerExtensionsHelper.clear();
        }
    }

    /**
     * Verifies that the original base URL thread-local still takes precedence when building a URL with propagated query
     * parameters.
     */
    @Test
    public void testBuildURLWithOriginalBaseUrl() throws Exception {
        GeoServerExtensionsHelper.singleton(
                "projecttokenMangler",
                new org.geoserver.ows.URLMangler() {
                    @Override
                    public void mangleURL(
                            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
                        kvp.put("projecttoken", "abc123");
                    }
                },
                org.geoserver.ows.URLMangler.class);

        GwcServiceDispatcherCallback.GWC_ORIGINAL_BASEURL.set("http://proxy.example.com/geoserver");
        try {
            Map<String, String> queryParameters = new LinkedHashMap<>();

            URLMangler.UrlAndParams result =
                    urlMangler.buildURL("http://foo.example.com/", "/foo/", "/bar", queryParameters);

            Assert.assertEquals("http://proxy.example.com/geoserver/foo/bar", result.url());
            Assert.assertEquals("abc123", result.queryParameters().get("projecttoken"));
        } finally {
            GwcServiceDispatcherCallback.GWC_ORIGINAL_BASEURL.remove();
            GeoServerExtensionsHelper.clear();
        }
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geowebcache.util.URLMangler;
import org.geowebcache.util.URLManglerUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResponseUtilsURLManglerTest {

    private URLMangler urlMangler;

    @Before
    public void setUp() {
        urlMangler = new ResponseUtilsURLMangler();
    }

    @Test
    public void testBuildURL() {
        Assert.assertEquals(
                "http://foo.example.com/foo/bar",
                URLManglerUtils.buildURL(
                        "http://foo.example.com", "/foo", "/bar", null, urlMangler, URLMangler.URLType.SERVICE));
    }

    /** Verifies GeoServer-provided query parameters are appended after an existing URL query. */
    @Test
    public void testBuildURLWithQueryParameters() {
        GeoServerExtensionsHelper.singleton(
                "projecttokenMangler",
                (org.geoserver.ows.URLMangler) (base, path, kvp, type) -> kvp.put("projecttoken", "abc123"),
                org.geoserver.ows.URLMangler.class);
        try {
            Assert.assertEquals(
                    "http://foo.example.com/foo/bar?format=image/png&projecttoken=abc123",
                    URLManglerUtils.buildURL(
                            "http://foo.example.com/",
                            "/foo",
                            "/bar?format=image/png",
                            null,
                            urlMangler,
                            URLMangler.URLType.SERVICE));
        } finally {
            GeoServerExtensionsHelper.clear();
        }
    }

    /** Verifies the dispatcher callback's original base URL is preserved by the adapter. */
    @Test
    public void testBuildURLWithOriginalBaseUrl() {
        GwcServiceDispatcherCallback.GWC_ORIGINAL_BASEURL.set("http://proxy.example.com/geoserver");
        try {
            Assert.assertEquals(
                    "http://proxy.example.com/geoserver/foo/bar",
                    URLManglerUtils.buildURL(
                            "http://foo.example.com/", "/foo", "/bar", null, urlMangler, URLMangler.URLType.SERVICE));
        } finally {
            GwcServiceDispatcherCallback.GWC_ORIGINAL_BASEURL.remove();
        }
    }
}

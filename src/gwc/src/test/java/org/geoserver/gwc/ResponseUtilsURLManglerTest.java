/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

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
}

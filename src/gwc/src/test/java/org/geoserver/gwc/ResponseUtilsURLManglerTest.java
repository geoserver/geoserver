package org.geoserver.gwc;

import junit.framework.TestCase;
import org.geowebcache.util.URLMangler;

public class ResponseUtilsURLManglerTest extends TestCase {

    private URLMangler urlMangler;

    @Override
    protected void setUp() throws Exception {
        urlMangler = new ResponseUtilsURLMangler();
    }

    public void testBuildURL() {
        String url = urlMangler.buildURL("http://foo.example.com", "/foo", "/bar");
        assertEquals("http://foo.example.com/foo/bar", url);
    }

    public void testBuildTrailingSlashes() throws Exception {
        String url = urlMangler.buildURL("http://foo.example.com/", "/foo/", "/bar");
        assertEquals("http://foo.example.com/foo/bar", url);
    }

    public void testBuildNoLeadingSlashes() throws Exception {
        String url = urlMangler.buildURL("http://foo.example.com/", "foo/", "bar");
        assertEquals("http://foo.example.com/foo/bar", url);
    }
}

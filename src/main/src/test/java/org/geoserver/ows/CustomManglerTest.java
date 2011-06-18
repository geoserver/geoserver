package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.util.Collections;

import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.test.GeoServerTestSupport;

public class CustomManglerTest extends GeoServerTestSupport {

    private static final String BASEURL = "http://localhost:8080/geoserver";

    @Override
    protected String[] getSpringContextLocations() {
        // register the custom mangler
        return new String[] { "classpath*:/applicationContext.xml",
                "classpath*:/applicationSecurityContext.xml", "classpath*:/customManglerContext.xml" };
    }

    public void testBasic() {
        String url = buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test?here=iam", url);
    }

    public void testKVP() {
        String url = buildURL(BASEURL, "test", Collections.singletonMap("param", "value()"),
                URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test?param=value%28%29&here=iam", url);
    }
}

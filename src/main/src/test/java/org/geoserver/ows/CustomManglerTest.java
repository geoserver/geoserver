/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class CustomManglerTest extends GeoServerSystemTestSupport {

    private static final String BASEURL = "http://localhost:8080/geoserver";

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/customManglerContext.xml");
    }

    @Test
    public void testBasic() {
        String url = buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test?here=iam", url);
    }

    @Test
    public void testKVP() {
        String url =
                buildURL(
                        BASEURL,
                        "test",
                        Collections.singletonMap("param", "value()"),
                        URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test?param=value%28%29&here=iam", url);
    }
}

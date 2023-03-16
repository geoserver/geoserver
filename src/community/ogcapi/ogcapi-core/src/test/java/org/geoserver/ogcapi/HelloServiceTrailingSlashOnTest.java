/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

/** Tests default behavior for trailing slash match (enabled) */
public class HelloServiceTrailingSlashOnTest extends OGCApiTestSupport {

    @Test
    public void testLandingPageRegardlessTrailingSlash() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/hello/v1/", 200);
        assertEquals("Landing page", doc.read("message"));
        doc = getAsJSONPath("ogc/hello/v1", 200);
        assertEquals("Landing page", doc.read("message"));
    }

    @Test
    public void testHelloTrailingSlash() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/hello/v1/hello?message=abcd", 200);
        assertEquals("abcd", doc.read("message"));
        doc = getAsJSONPath("ogc/hello/v1/hello/?message=abcd", 200);
        assertEquals("abcd", doc.read("message"));
    }
}

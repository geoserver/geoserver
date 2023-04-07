/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileOutputStream;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests trailing slash match disabled, with exception for landing page */
public class HelloServiceTrailingSlashOffTest extends OGCApiTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        // disable trailing slash match before the Spring context is configured
        File root = testData.getDataDirectoryRoot();
        XStream xs = new XStreamPersisterFactory().createXMLPersister().getXStream();
        File file = new File(root, "global.xml");
        GeoServerInfo gs = (GeoServerInfo) xs.fromXML(file);
        gs.setTrailingSlashMatch(false);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            xs.toXML(gs, fos);
        }
    }

    @Test
    public void testLandingPageRegardlessTrailingSlash() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/hello/v1/", 200);
        assertEquals("Landing page", doc.read("message"));
        doc = getAsJSONPath("ogc/hello/v1", 200);
        assertEquals("Landing page", doc.read("message"));
    }

    @Test
    public void testWorkspaceLandingPageRegardlessTrailingSlash() throws Exception {
        DocumentContext doc = getAsJSONPath("cite/ogc/hello/v1/", 200);
        assertEquals("Landing page", doc.read("message"));
        doc = getAsJSONPath("cite/ogc/hello/v1", 200);
        assertEquals("Landing page", doc.read("message"));
        // this workspace does not exist
        assertEquals(404, getAsServletResponse("abcd/ogc/hello/v1").getStatus());
    }

    @Test
    public void testLayerLandingPageRegardlessTrailingSlash() throws Exception {
        DocumentContext doc = getAsJSONPath("cite/BasicPolygons/ogc/hello/v1/", 200);
        assertEquals("Landing page", doc.read("message"));
        doc = getAsJSONPath("cite/BasicPolygons/ogc/hello/v1", 200);
        assertEquals("Landing page", doc.read("message"));
        // this layer does not exist
        assertEquals(404, getAsServletResponse("cite/abcd/ogc/hello/v1").getStatus());
    }

    @Test
    public void testHelloTrailingSlash() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/hello/v1/hello?message=abcd", 200);
        assertEquals("abcd", doc.read("message"));
        MockHttpServletResponse sr = getAsServletResponse("ogc/hello/v1/hello/?message=abcd");
        assertEquals(404, sr.getStatus());
        doc = getAsJSONPath(sr);
        assertEquals("ResourceNotFound", doc.read("type"));
        assertEquals("No mapping for GET /geoserver/ogc/hello/v1/hello/", doc.read("title"));
    }
}

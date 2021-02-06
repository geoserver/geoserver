/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.response.GMLCoverageResponseDelegate;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testing {@link GMLCoverageResponseDelegate}
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GMLGetCoverageTest extends WCSTestSupport {

    @Test
    public void testGMLExtension() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageGML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/gml+xml", response.getContentType());
        // is it readable xml?
        dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
    }
}

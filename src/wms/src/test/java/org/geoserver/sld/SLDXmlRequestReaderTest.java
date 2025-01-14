/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sld;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Test suite for {@link SLDXmlRequestReader} */
public class SLDXmlRequestReaderTest extends WMSTestSupport {

    @Test
    public void testExtensionPoint() {
        List<XmlRequestReader> xmlReaders = GeoServerExtensions.extensions(XmlRequestReader.class);
        Optional<SLDXmlRequestReader> findExtension = xmlReaders.stream()
                .filter(SLDXmlRequestReader.class::isInstance)
                .map(SLDXmlRequestReader.class::cast)
                .findFirst();
        assertTrue(findExtension.isPresent());
    }

    @Test
    public void testGetMapSld() throws Exception {
        String path =
                "/wms?service=WMS&version=1.1.0&request=GetMap&width=100&height=100&format=image/png&bbox=-180,-90,180,90";
        String body = "    <StyledLayerDescriptor version=\"1.0.0\">\n"
                + "        <NamedLayer>\n"
                + "            <Name>wcs:World</Name>\n"
                + "            <NamedStyle><Name>generic</Name></NamedStyle>\n"
                + "        </NamedLayer>\n"
                + "    </StyledLayerDescriptor>\n";
        MockHttpServletResponse response = super.postAsServletResponse(path, body);
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testGetMapSldXXE() throws Exception {
        String path =
                "/wms?service=WMS&version=1.1.0&request=GetMap&width=100&height=100&format=image/png&bbox=-180,-90,180,90";
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE StyledLayerDescriptor [\n"
                + "<!ENTITY xxe SYSTEM \"file:///some/file\">]>\n"
                + "<StyledLayerDescriptor version=\"1.0.0\">\n"
                + "<NamedLayer><Name>&xxe;</Name></NamedLayer>\n"
                + "</StyledLayerDescriptor>";

        Logging.getLogger("geoserver.ows").setLevel(Level.OFF);
        MockHttpServletResponse response = super.postAsServletResponse(path, body);
        assertEquals(200, response.getStatus());
        super.assertContentType("application/vnd.ogc.se_xml", response);
        assertThat(response.getContentAsString(), containsString("Entity resolution disallowed for file"));
    }
}

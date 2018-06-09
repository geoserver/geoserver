/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import net.opengis.wcs11.GetCapabilitiesType;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;
import org.geotools.wcs.v1_1.WCSConfiguration;
import org.junit.Before;
import org.junit.Test;

public class GetCapabilitiesXmlParserTest {

    private WCSConfiguration configuration;

    private WcsXmlReader reader;

    @Before
    public void setUp() throws Exception {
        configuration = new WCSConfiguration();
        reader =
                new WcsXmlReader(
                        "GetCapabilities",
                        "1.1.1",
                        configuration,
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
    }

    @Test
    public void testBasic() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wcs:GetCapabilities service=\"WCS\" "
                        + "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        // smoke test, we only try out a very basic request
        GetCapabilitiesType cap =
                (GetCapabilitiesType) reader.read(null, new StringReader(request), null);
        assertEquals("WCS", cap.getService());
    }

    @Test
    public void testFull() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + //
                        "<wcs:GetCapabilities service=\"WCS\" "
                        + //
                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + //
                        "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                        + //
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + //
                        "<ows:AcceptVersions>"
                        + //
                        "  <ows:Version>1.0.0</ows:Version>"
                        + //
                        "</ows:AcceptVersions>"
                        + //
                        "<ows:Sections>"
                        + //
                        "  <ows:Section>Section1</ows:Section>"
                        + //
                        "</ows:Sections>"
                        + //
                        "<ows:AcceptFormats>"
                        + //
                        "  <ows:OutputFormat>text/xml</ows:OutputFormat>"
                        + //
                        "</ows:AcceptFormats>"
                        + //
                        "</wcs:GetCapabilities>";
        // System.out.println(request);
        GetCapabilitiesType cap =
                (GetCapabilitiesType) reader.read(null, new StringReader(request), null);
        assertEquals("WCS", cap.getService());
        assertEquals(1, cap.getAcceptVersions().getVersion().size());
        assertEquals("1.0.0", cap.getAcceptVersions().getVersion().get(0));
        assertEquals(1, cap.getSections().getSection().size());
        assertEquals("Section1", cap.getSections().getSection().get(0));
        assertEquals(1, cap.getAcceptFormats().getOutputFormat().size());
        assertEquals("text/xml", cap.getAcceptFormats().getOutputFormat().get(0));
    }
}

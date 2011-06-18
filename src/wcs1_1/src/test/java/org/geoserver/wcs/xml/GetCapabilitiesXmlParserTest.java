package org.geoserver.wcs.xml;

import java.io.StringReader;

import junit.framework.TestCase;
import net.opengis.wcs11.GetCapabilitiesType;

import org.geoserver.wcs.xml.v1_1_1.WCSConfiguration;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;

public class GetCapabilitiesXmlParserTest extends TestCase {

    private WCSConfiguration configuration;

    private WcsXmlReader reader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configuration = new WCSConfiguration();
        reader = new WcsXmlReader("GetCapabilities", "1.1.1", configuration);
    }

    public void testBasic() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wcs:GetCapabilities service=\"WCS\" "
                + "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        // smoke test, we only try out a very basic request
        GetCapabilitiesType cap = (GetCapabilitiesType) reader.read(null,
                new StringReader(request), null);
        assertEquals("WCS", cap.getService());
    }

    public void testFull() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + //
                "<wcs:GetCapabilities service=\"WCS\" " + //
                "xmlns:ows=\"http://www.opengis.net/ows/1.1\" " + //
                "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" " + //
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + //
                "<ows:AcceptVersions>" + //
                "  <ows:Version>1.0.0</ows:Version>" + //
                "</ows:AcceptVersions>" + //
                "<ows:Sections>" + //
                "  <ows:Section>Section1</ows:Section>" + //
                "</ows:Sections>" + //
                "<ows:AcceptFormats>" + //
                "  <ows:OutputFormat>text/xml</ows:OutputFormat>" + //
                "</ows:AcceptFormats>" + //
                "</wcs:GetCapabilities>";
//        System.out.println(request);
        GetCapabilitiesType cap = (GetCapabilitiesType) reader.read(null,
                new StringReader(request), null);
        assertEquals("WCS", cap.getService());
        assertEquals(1, cap.getAcceptVersions().getVersion().size());
        assertEquals("1.0.0", (String) cap.getAcceptVersions().getVersion().get(0));
        assertEquals(1, cap.getSections().getSection().size());
        assertEquals("Section1", (String) cap.getSections().getSection().get(0));
        assertEquals(1, cap.getAcceptFormats().getOutputFormat().size());
        assertEquals("text/xml", (String) cap.getAcceptFormats().getOutputFormat().get(0));
    }
}

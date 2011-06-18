package org.geoserver.wcs.xml;

import java.io.StringReader;

import junit.framework.TestCase;
import net.opengis.wcs11.DescribeCoverageType;

import org.geoserver.wcs.xml.v1_1_1.WCSConfiguration;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;

public class DescribeCoverageXmlParserTest extends TestCase {

    private WCSConfiguration configuration;

    private WcsXmlReader reader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configuration = new WCSConfiguration();
        reader = new WcsXmlReader("DescribeCoverage", "1.1.1", configuration);
    }

    public void testBasic() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
                "<wcs:DescribeCoverage service=\"WCS\" " + //
                "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
                "  xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\"\r\n" + // 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
                "  xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1.1 " + //
                "schemas/wcs/1.1.1/wcsAll.xsd\"\r\n" + //
                "  version=\"1.1.1\" >\r\n" + //
                "  <wcs:Identifier>wcs:BlueMarble</wcs:Identifier>\r\n" + // 
                "</wcs:DescribeCoverage>";

        // smoke test, we only try out a very basic request
        DescribeCoverageType cap = (DescribeCoverageType) reader.read(null,
                new StringReader(request), null);
        assertEquals("WCS", cap.getService());
        assertEquals("1.1.1", cap.getVersion());
        assertEquals(1, cap.getIdentifier().size());
        assertEquals("wcs:BlueMarble", cap.getIdentifier().get(0));
    }
}

package org.geoserver.wcs.xml;

import java.io.StringReader;

import junit.framework.TestCase;
import net.opengis.wcs10.GetCapabilitiesType;

import org.geoserver.wcs.xml.v1_0_0.WcsXmlReader;
import org.geotools.wcs.WCSConfiguration;

public class GetCapabilitiesXmlParserTest extends TestCase {

    private WCSConfiguration configuration;

    private WcsXmlReader reader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configuration = new WCSConfiguration();
        reader = new WcsXmlReader("GetCapabilities", "1.0.0", configuration);
    }

    public void testBasic() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wcs:GetCapabilities service=\"WCS\" updateSequence=\"1\" "
                + "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:wcs=\"http://www.opengis.net/wcs\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        // smoke test, we only try out a very basic request
        GetCapabilitiesType cap = (GetCapabilitiesType) reader.read(null,
                new StringReader(request), null);
        assertEquals("WCS", cap.getService());
        assertEquals("1.0.0", cap.getVersion());
        assertEquals("1", cap.getUpdateSequence());
    }

}

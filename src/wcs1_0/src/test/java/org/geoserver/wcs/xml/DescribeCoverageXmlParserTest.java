package org.geoserver.wcs.xml;

import java.io.StringReader;

import junit.framework.TestCase;
import net.opengis.wcs10.DescribeCoverageType;

import org.geoserver.wcs.xml.v1_0_0.WcsXmlReader;
import org.geotools.wcs.WCSConfiguration;

public class DescribeCoverageXmlParserTest extends TestCase {

    private WCSConfiguration configuration;

    private WcsXmlReader reader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configuration = new WCSConfiguration();
        reader = new WcsXmlReader("DescribeCoverage", "1.0.0", configuration);
    }

    public void testBasic() throws Exception {
        String request = "<DescribeCoverage"
                + "  version=\"1.0.0\""
                + "  service=\"WCS\""
                + "  xmlns=\"http://www.opengis.net/wcs\""
                + "  xmlns:nurc=\"http://www.nurc.nato.int\""
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + "  xsi:schemaLocation=\"http://www.opengis.net/wcs schemas/wcs/1.0.0/describeCoverage.xsd\">"
                + "    <Coverage>nurc:Pk50095</Coverage>    " + "</DescribeCoverage>";

        // smoke test, we only try out a very basic request
        DescribeCoverageType cap = (DescribeCoverageType) reader.read(null, new StringReader(
                request), null);
        assertEquals("WCS", cap.getService());
        assertEquals("1.0.0", cap.getVersion());
        assertEquals(1, cap.getCoverage().size());
        assertEquals("nurc:Pk50095", cap.getCoverage().get(0));
    }
}

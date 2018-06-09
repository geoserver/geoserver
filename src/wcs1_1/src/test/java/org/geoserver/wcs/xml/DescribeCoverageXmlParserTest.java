/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import net.opengis.wcs11.DescribeCoverageType;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;
import org.geotools.wcs.v1_1.WCSConfiguration;
import org.junit.Before;
import org.junit.Test;

public class DescribeCoverageXmlParserTest {

    private WCSConfiguration configuration;

    private WcsXmlReader reader;

    @Before
    public void setUp() throws Exception {
        configuration = new WCSConfiguration();
        reader =
                new WcsXmlReader(
                        "DescribeCoverage",
                        "1.1.1",
                        configuration,
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
    }

    @Test
    public void testBasic() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                        + //
                        "<wcs:DescribeCoverage service=\"WCS\" "
                        + //
                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n"
                        + //
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\"\r\n"
                        + //
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                        + //
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1.1 "
                        + //
                        "schemas/wcs/1.1.1/wcsAll.xsd\"\r\n"
                        + //
                        "  version=\"1.1.1\" >\r\n"
                        + //
                        "  <wcs:Identifier>wcs:BlueMarble</wcs:Identifier>\r\n"
                        + //
                        "</wcs:DescribeCoverage>";

        // smoke test, we only try out a very basic request
        DescribeCoverageType cap =
                (DescribeCoverageType) reader.read(null, new StringReader(request), null);
        assertEquals("WCS", cap.getService());
        assertEquals("1.1.1", cap.getVersion());
        assertEquals(1, cap.getIdentifier().size());
        assertEquals("wcs:BlueMarble", cap.getIdentifier().get(0));
    }
}

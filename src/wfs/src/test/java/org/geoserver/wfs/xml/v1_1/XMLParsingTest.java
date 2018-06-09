/* (c) 2014 -2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.NativeType;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.xml.Parser;
import org.junit.Test;

public class XMLParsingTest extends WFSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no need for test data
        testData.setUpSecurity();
    }

    @Test
    public void testNativeType() throws Exception {
        Parser p = new Parser(getXmlConfiguration11());
        NativeType nativ =
                (NativeType)
                        p.parse(
                                new ByteArrayInputStream(
                                        "<wfs:Native safeToIgnore='true' xmlns:wfs='http://www.opengis.net/wfs'>here is some text</wfs:Native>"
                                                .getBytes()));

        assertEquals("here is some text", nativ.getValue());
    }

    @Test
    public void testGetFeatureWithLock() throws Exception {
        Parser p = new Parser(getXmlConfiguration11());

        String request =
                "<wfs:GetFeatureWithLock xmlns:wfs=\"http://www.opengis.net/wfs\" expiry=\"10\" service=\"WFS\" \n"
                        + "outputFormat=\"text/xml; subtype=gml/3.1.1\" version=\"1.1.0\"\n"
                        + "xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "<wfs:Query typeName=\"topp:states\" srsName=\"EPSG:4326\" xmlns:topp=\"http://www.openplans.org/topp\">\n"
                        + "  <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                        + "<ogc:FeatureId fid=\"states.1\"/>\n"
                        + "</ogc:Filter>\n"
                        + "</wfs:Query>\n"
                        + "</wfs:GetFeatureWithLock>";

        GetFeatureWithLockType gf =
                (GetFeatureWithLockType) p.parse(new ByteArrayInputStream(request.getBytes()));

        assertEquals("WFS", gf.getService());
        assertEquals("1.1.0", gf.getVersion());
        assertEquals("text/xml; subtype=gml/3.1.1", gf.getOutputFormat());
        assertEquals(new BigInteger("10"), gf.getExpiry());
    }
}

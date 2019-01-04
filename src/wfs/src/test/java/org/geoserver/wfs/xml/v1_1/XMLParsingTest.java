/* (c) 2014 -2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Map;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.NativeType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.xsd.Parser;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

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

    @Test
    public void testUpdate() throws Exception {
        WFSConfiguration xmlConfiguration11 = getXmlConfiguration11();
        Parser p = new Parser(xmlConfiguration11);
        p.setHandleMixedContent(true);

        String request =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + //
                        "xmlns:topp=\"http://www.openplans.org/topp\" "
                        + //
                        "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + //
                        "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + //
                        "<wfs:Update typeName=\"topp:tasmania_roads\">"
                        + //
                        "<wfs:Property>"
                        + //
                        "<wfs:Name>TYPE</wfs:Name>"
                        + //
                        "<wfs:Value><![CDATA[street\n"
                        + //
                        "line2\n"
                        + //
                        "line3\n]]></wfs:Value>"
                        + //
                        "</wfs:Property>"
                        + //
                        "<ogc:Filter>"
                        + //
                        "<ogc:FeatureId fid=\"tasmania_roads.1\"/>"
                        + //
                        "</ogc:Filter>"
                        + //
                        "</wfs:Update>"
                        + //
                        "</wfs:Transaction>";

        TransactionType obj =
                (TransactionType) p.parse(new ByteArrayInputStream(request.getBytes()));
        UpdateElementType update = (UpdateElementType) obj.getUpdate().get(0);
        PropertyType property = (PropertyType) update.getProperty().get(0);

        assertEquals("street\nline2\nline3\n", property.getValue());
    }

    @Test
    public void testInsert() throws Exception {
        WFSConfiguration xmlConfiguration11 = getXmlConfiguration11();
        Parser p = new Parser(xmlConfiguration11);
        p.setHandleMixedContent(true);

        String request =
                "<wfs:Transaction xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "service=\"WFS\" version=\"1.1.0\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\">"
                        + "<wfs:Insert>"
                        + //
                        "<feature:t2 xmlns:feature=\"http://test\">"
                        + //
                        "<feature:geom>"
                        + //
                        "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\">"
                        + //
                        "<gml:pos>0 0</gml:pos>"
                        + //
                        "</gml:Point>"
                        + //
                        "</feature:geom>"
                        + //
                        "<feature:descr><![CDATA[1\n"
                        + //
                        "2\n"
                        + //
                        "3\n"
                        + //
                        "]]></feature:descr>"
                        + //
                        "</feature:t2>"
                        + //
                        "</wfs:Insert>"
                        + //
                        "</wfs:Transaction>";

        TransactionType obj =
                (TransactionType) p.parse(new ByteArrayInputStream(request.getBytes()));
        InsertElementType insert = (InsertElementType) obj.getInsert().get(0);
        SimpleFeature feature = (SimpleFeature) insert.getFeature().get(0);
        Map<?, ?> attr = (Map<?, ?>) feature.getAttribute("descr");
        String expected = "1\n2\n3\n";

        assertEquals("new lines eaten", expected, attr.get(null).toString());
    }
}

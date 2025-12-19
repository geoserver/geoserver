/* (c) 2014 -2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1;

import static org.junit.Assert.assertEquals;

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
import org.geoserver.wfs.xml.WFSXmlConfiguration;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.xsd.Parser;
import org.junit.Test;

public class XMLParsingTest extends WFSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no need for test data
        testData.setUpSecurity();
    }

    @Test
    public void testNativeType() throws Exception {
        Parser p = new Parser((org.geotools.xsd.Configuration) getXmlConfiguration11());
        NativeType nativ = (NativeType) p.parse(new ByteArrayInputStream(
                "<wfs:Native safeToIgnore='true' xmlns:wfs='http://www.opengis.net/wfs'>here is some text</wfs:Native>"
                        .getBytes()));

        assertEquals("here is some text", nativ.getValue());
    }

    @Test
    public void testGetFeatureWithLock() throws Exception {
        Parser p = new Parser((org.geotools.xsd.Configuration) getXmlConfiguration11());

        String request =
                """
                <wfs:GetFeatureWithLock xmlns:wfs="http://www.opengis.net/wfs" expiry="10" service="WFS"\s
                outputFormat="text/xml; subtype=gml/3.1.1" version="1.1.0"
                xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <wfs:Query typeName="topp:states" srsName="EPSG:4326" xmlns:topp="http://www.openplans.org/topp">
                  <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
                <ogc:FeatureId fid="states.1"/>
                </ogc:Filter>
                </wfs:Query>
                </wfs:GetFeatureWithLock>""";

        GetFeatureWithLockType gf = (GetFeatureWithLockType) p.parse(new ByteArrayInputStream(request.getBytes()));

        assertEquals("WFS", gf.getService());
        assertEquals("1.1.0", gf.getVersion());
        assertEquals("text/xml; subtype=gml/3.1.1", gf.getOutputFormat());
        assertEquals(BigInteger.valueOf(10), gf.getExpiry());
    }

    @Test
    public void testUpdate() throws Exception {
        WFSXmlConfiguration xmlConfiguration11 = getXmlConfiguration11();
        Parser p = new Parser((org.geotools.xsd.Configuration) xmlConfiguration11);
        p.setHandleMixedContent(true);

        String request =
                """
                <wfs:Transaction service="WFS" version="1.1.0" \
                xmlns:topp="http://www.openplans.org/topp" \
                xmlns:ogc="http://www.opengis.net/ogc" \
                xmlns:wfs="http://www.opengis.net/wfs">\
                <wfs:Update typeName="topp:tasmania_roads">\
                <wfs:Property>\
                <wfs:Name>TYPE</wfs:Name>\
                <wfs:Value><![CDATA[street
                line2
                line3
                ]]></wfs:Value>\
                </wfs:Property>\
                <ogc:Filter>\
                <ogc:FeatureId fid="tasmania_roads.1"/>\
                </ogc:Filter>\
                </wfs:Update>\
                </wfs:Transaction>""";

        TransactionType obj = (TransactionType) p.parse(new ByteArrayInputStream(request.getBytes()));
        UpdateElementType update = (UpdateElementType) obj.getUpdate().get(0);
        PropertyType property = (PropertyType) update.getProperty().get(0);

        assertEquals("street\nline2\nline3\n", property.getValue());
    }

    @Test
    public void testInsert() throws Exception {
        WFSXmlConfiguration xmlConfiguration11 = getXmlConfiguration11();
        Parser p = new Parser((org.geotools.xsd.Configuration) xmlConfiguration11);
        p.setHandleMixedContent(true);

        String request =
                """
                <wfs:Transaction xmlns:wfs="http://www.opengis.net/wfs" \
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
                service="WFS" version="1.1.0" xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd">\
                <wfs:Insert>\
                <feature:t2 xmlns:feature="http://test">\
                <feature:geom>\
                <gml:Point xmlns:gml="http://www.opengis.net/gml">\
                <gml:pos>0 0</gml:pos>\
                </gml:Point>\
                </feature:geom>\
                <feature:descr><![CDATA[1
                2
                3
                ]]></feature:descr>\
                </feature:t2>\
                </wfs:Insert>\
                </wfs:Transaction>""";

        TransactionType obj = (TransactionType) p.parse(new ByteArrayInputStream(request.getBytes()));
        InsertElementType insert = (InsertElementType) obj.getInsert().get(0);
        SimpleFeature feature = (SimpleFeature) insert.getFeature().get(0);
        Map<?, ?> attr = (Map<?, ?>) feature.getAttribute("descr");
        String expected = "1\n2\n3\n";

        assertEquals("new lines eaten", expected, attr.get(null).toString());
    }
}

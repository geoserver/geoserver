/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.util.ReaderUtils;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.v1_1_0.XmlSchemaEncoder;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DescribeFeatureResponseTest extends WFSTestSupport {

    Operation request() {
        Service service = getServiceDescriptor10();
        DescribeFeatureTypeType type = WfsFactory.eINSTANCE.createDescribeFeatureTypeType();
        type.setBaseUrl("http://localhost:8080/geoserver");

        Operation request = new Operation("wfs", service, null, new Object[] {type});
        return request;
    }

    @Test
    public void testSingle() throws Exception {
        FeatureTypeInfo meta = getFeatureTypeInfo(CiteTestData.BASIC_POLYGONS);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        XmlSchemaEncoder response = new XmlSchemaEncoder.V11(getGeoServer());
        response.write(new FeatureTypeInfo[] {meta}, output, request());

        Element schema = ReaderUtils.parse(new StringReader(new String(output.toByteArray())));
        assertEquals("xsd:schema", schema.getNodeName());

        NodeList types = schema.getElementsByTagName("xsd:complexType");
        assertEquals(1, types.getLength());
    }

    @Test
    public void testWithDifferntNamespaces() throws Exception {

        FeatureTypeInfo meta1 = getFeatureTypeInfo(CiteTestData.BASIC_POLYGONS);
        FeatureTypeInfo meta2 = getFeatureTypeInfo(CiteTestData.POLYGONS);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        XmlSchemaEncoder response = new XmlSchemaEncoder.V11(getGeoServer());
        response.write(new FeatureTypeInfo[] {meta1, meta2}, output, request());

        Element schema = ReaderUtils.parse(new StringReader(new String(output.toByteArray())));
        assertEquals("xsd:schema", schema.getNodeName());

        NodeList imprts = schema.getElementsByTagName("xsd:import");
        assertEquals(2, imprts.getLength());
    }
}

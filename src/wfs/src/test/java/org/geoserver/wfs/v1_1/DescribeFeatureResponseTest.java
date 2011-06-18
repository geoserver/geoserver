package org.geoserver.wfs.v1_1;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import junit.framework.Test;
import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.util.ReaderUtils;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.v1_1_0.XmlSchemaEncoder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DescribeFeatureResponseTest extends WFSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeFeatureResponseTest());
    }

    Operation request() {
        Service service = getServiceDescriptor10();
        DescribeFeatureTypeType type = WfsFactory.eINSTANCE.createDescribeFeatureTypeType();
        type.setBaseUrl("http://localhost:8080/geoserver");
        
        Operation request = new Operation("wfs", service, null, new Object[] { type });
        return request;
    }
    
    public void testSingle() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo meta = getFeatureTypeInfo(MockData.BASIC_POLYGONS);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        XmlSchemaEncoder response = new XmlSchemaEncoder.V11(getGeoServer());
        response.write(new FeatureTypeInfo[] { meta }, output, request());

        Element schema = ReaderUtils.parse(new StringReader(new String(output
                .toByteArray())));
        assertEquals("xsd:schema", schema.getNodeName());

        NodeList types = schema.getElementsByTagName("xsd:complexType");
        assertEquals(1, types.getLength());
    }

    public void testWithDifferntNamespaces() throws Exception {

        FeatureTypeInfo meta1 = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        FeatureTypeInfo meta2 = getFeatureTypeInfo(MockData.POLYGONS);
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        XmlSchemaEncoder response = new XmlSchemaEncoder.V11(getGeoServer());
        response.write(new FeatureTypeInfo[] { meta1, meta2 }, output, request());

        Element schema = ReaderUtils.parse(new StringReader(new String(output
                .toByteArray())));
        assertEquals("xsd:schema", schema.getNodeName());

        NodeList imprts = schema.getElementsByTagName("xsd:import");
        assertEquals(2, imprts.getLength());

    }

}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;

public class GML3FeatureProducerTest extends WFSTestSupport {

    GML3OutputFormat producer() {
        FeatureTypeSchemaBuilder sb = new FeatureTypeSchemaBuilder.GML3(getGeoServer());
        WFSConfiguration configuration = new WFSConfiguration(getGeoServer(), sb, new WFS(sb));
        return new GML3OutputFormat(getGeoServer(), configuration);
    }

    /**
     * Build a GetFeature operation to request the named types.
     *
     * @param names type names for which queries are present in the returned request
     * @return GetFeature operation to request the named types
     */
    Operation request(QName... names) {
        Service service = getServiceDescriptor10();
        GetFeatureType type = WfsFactory.eINSTANCE.createGetFeatureType();
        type.setBaseUrl("http://localhost:8080/geoserver");
        for (QName name : names) {
            QueryType queryType = WfsFactory.eINSTANCE.createQueryType();
            queryType.setTypeName(Collections.singletonList(name));
            type.getQuery().add(queryType);
        }
        Operation request = new Operation("wfs", service, null, new Object[] {type});
        return request;
    }

    @Test
    public void testSingle() throws Exception {
        FeatureSource<? extends FeatureType, ? extends Feature> source =
                getFeatureSource(MockData.SEVEN);
        FeatureCollection<? extends FeatureType, ? extends Feature> features = source.getFeatures();

        FeatureCollectionResponse fcType =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        fcType.getFeature().add(features);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        producer().write(fcType, output, request(MockData.SEVEN));

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(7, document.getElementsByTagName("cdf:Seven").getLength());
    }

    @Test
    public void testMultipleSameNamespace() throws Exception {
        FeatureCollectionResponse fcType =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fcType.getFeature().add(getFeatureSource(MockData.SEVEN).getFeatures());
        fcType.getFeature().add(getFeatureSource(MockData.FIFTEEN).getFeatures());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        producer().write(fcType, output, request(MockData.SEVEN, MockData.FIFTEEN));

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(
                7 + 15,
                document.getElementsByTagName("cdf:Seven").getLength()
                        + document.getElementsByTagName("cdf:Fifteen").getLength());
    }

    @Test
    public void testMultipleDifferentNamespace() throws Exception {
        FeatureCollectionResponse fcType =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fcType.getFeature().add(getFeatureSource(MockData.SEVEN).getFeatures());
        fcType.getFeature().add(getFeatureSource(MockData.POLYGONS).getFeatures());

        int npolys = getFeatureSource(MockData.POLYGONS).getFeatures().size();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        producer().write(fcType, output, request(MockData.SEVEN, MockData.POLYGONS));

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(
                7 + npolys,
                document.getElementsByTagName("cdf:Seven").getLength()
                        + document.getElementsByTagName("cgf:Polygons").getLength());
    }
}

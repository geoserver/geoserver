/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class KMLSuperOverlayTransformerTest extends WMSTestSupport {

    public static QName DISPERSED_FEATURES = new QName(MockData.SF_URI, "Dispersed",
            MockData.SF_PREFIX);

    WMSMapContent mapContent;

    Layer Layer;

 
    @Before
    public void prepare() throws Exception {
        
        Layer = createMapLayer(DISPERSED_FEATURES);

        mapContent = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        mapContent.addLayer(Layer);
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog =getCatalog();
        testData.addStyle("allsymbolizers","allsymbolizers.sld",getClass(), catalog);
        testData.addStyle("SingleFeature","singlefeature.sld",getClass(), catalog);
        testData.addStyle("Bridge","bridge.sld",getClass(), catalog);
        testData.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
        testData.addVectorLayer(DISPERSED_FEATURES,Collections.EMPTY_MAP,"Dispersed.properties",
                getClass(),catalog);
    }

    /**
     * Verify that two overlay tiles are produced for a request that encompasses the world.
     */
    @Test
    public void testWorldBoundsSuperOverlay() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(getWMS(),
                mapContent);
        transformer.setIndentation(2);

        mapContent.getViewport().setBounds(
                new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(Layer, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(5, document.getElementsByTagName("Region").getLength());
        assertEquals(4, document.getElementsByTagName("NetworkLink").getLength());
        assertEquals(0, document.getElementsByTagName("GroundOverlay").getLength());
    }

    /**
     * Verify that when a tile smaller than one hemisphere is requested, then subtiles are included 
     * in the result (but only the ones necessary for the data at hand)
     */
    @Test
    public void testSubtileSuperOverlay() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(getWMS(),
                mapContent);
        transformer.setIndentation(2);

        mapContent.getViewport().setBounds(
                new ReferencedEnvelope(0, 180, -90, 90, DefaultGeographicCRS.WGS84));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(Layer, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        print(document);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        // only two regions, the root one and one network link (that's all we need)
        assertEquals(2, document.getElementsByTagName("Region").getLength());
        assertEquals(1, document.getElementsByTagName("NetworkLink").getLength());
        assertEquals(0, document.getElementsByTagName("GroundOverlay").getLength());
    }

    @Test
    public void testKmltitleFormatOption() throws Exception {
        KMLSuperOverlayTransformer transformer = new KMLSuperOverlayTransformer(getWMS(),
                mapContent);
        transformer.setIndentation(2);

        mapContent.getViewport().setBounds(
                new ReferencedEnvelope(0, 180, -90, 90, DefaultGeographicCRS.WGS84));
        Map<String, Object> formatOptions = new HashMap<String, Object>();
        formatOptions.put("kmltitle", "myCustomLayerTitle");
        mapContent.getRequest().setFormatOptions(formatOptions);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(Layer, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals("myCustomLayerTitle", document.getElementsByTagName("Document").item(0)
                .getFirstChild().getNextSibling().getTextContent());
    }
}

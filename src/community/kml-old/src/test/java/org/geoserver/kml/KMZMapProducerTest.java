/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.kml.KMZMapResponse.KMZMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class KMZMapProducerTest extends WMSTestSupport {

    WMSMapContent mapContent;
    KMZMapOutputFormat mapProducer;
    KMZMapResponse mapEncoder;
    KMZMap producedMap;
    XpathEngine engine;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog =getCatalog();
        testData.addStyle("big-local-image","/big-local-image.sld",getClass(), catalog);
        testData.addStyle("big-mark","/big-mark.sld",getClass(), catalog);
        testData.copyTo(getClass().getResourceAsStream("/planet-42.png"), "styles/planet-42.png");
        
        HashMap m = new HashMap();
        m.put("kml", "http://www.opengis.net/kml/2.2");

        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    
    public void createMapContext(QName... names) throws Exception {

        // create a map context
        mapContent = new WMSMapContent();
        for(QName layerName: names) {
            mapContent.addLayer(createMapLayer(layerName));
        }
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        GetMapRequest getMapRequest = createGetMapRequest(names);
        mapContent.setRequest(getMapRequest);

        // create hte map producer
        mapProducer = new KMZMapOutputFormat(getWMS());
        mapEncoder = new KMZMapResponse(getWMS());
        producedMap = mapProducer.produceMap(mapContent);
    }
    
    public void createMapContext(QName layer, String style) throws Exception {

        // create a map context
        mapContent = new WMSMapContent();
        mapContent.addLayer(createMapLayer(layer, style));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        GetMapRequest getMapRequest = createGetMapRequest(new QName[]{layer});
        mapContent.setRequest(getMapRequest);

        // create hte map producer
        mapProducer = new KMZMapOutputFormat(getWMS());
        mapEncoder = new KMZMapResponse(getWMS());
        producedMap = mapProducer.produceMap(mapContent);
    }

    @Test
    public void test() throws Exception {
        createMapContext(MockData.BASIC_POLYGONS, MockData.BUILDINGS);
        // create the kmz
        File temp = File.createTempFile("test", "kmz");
        temp.delete();
        temp.mkdir();
        temp.deleteOnExit();

        File zip = new File(temp, "kmz.zip");
        zip.deleteOnExit();

        FileOutputStream output = new FileOutputStream(zip);
        mapEncoder.write(producedMap, output, null);

        output.flush();
        output.close();

        assertTrue(zip.exists());

        // unzip and test it
        ZipFile zipFile = new ZipFile(zip);

        assertNotNull(zipFile.getEntry("wms.kml"));
        assertNotNull(zipFile.getEntry("images/layer_0.png"));
        assertNotNull(zipFile.getEntry("images/layer_1.png"));

        zipFile.close();
    }

    
    @Test
    public void testEmbededPointImageSize() throws Exception {
        createMapContext(MockData.POINTS, "big-mark");
        
        File temp = File.createTempFile("test", "kmz");
        temp.delete();
        temp.mkdir();
        temp.deleteOnExit();
        
        File zip = new File(temp, "kmz.zip");
        zip.deleteOnExit();

        FileOutputStream output = new FileOutputStream(zip);
        mapEncoder.write(producedMap, output, null);

        output.flush();
        output.close();

        assertTrue(zip.exists());

        // unzip and test it
        ZipFile zipFile = new ZipFile(zip);
        
        ZipEntry kmlEntry = zipFile.getEntry("wms.kml");
        InputStream kmlStream = zipFile.getInputStream(kmlEntry);
        
        Document kmlResult = XMLUnit.buildTestDocument(new InputSource(kmlStream));
        
        Double scale = Double.parseDouble(XMLUnit.newXpathEngine().getMatchingNodes("(//kml:Style)[1]/kml:IconStyle/kml:scale", kmlResult).item(0).getTextContent());
        assertThat(scale, closeTo(49d/16d, 0.01));
        
        zipFile.close();

    }
}

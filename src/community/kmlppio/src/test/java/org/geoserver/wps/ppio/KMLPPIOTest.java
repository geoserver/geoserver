/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.Test;
import org.w3c.dom.Document;

/** Test Cases for KML 2.2 Encoder. */
public class KMLPPIOTest extends GeoServerTestSupport {

    private XpathEngine xpath;

    private KMLPPIO ppio;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("", "http://www.topografix.com/GPX/1/1");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpInternal() throws Exception {
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        SettingsInfo settings = global.getSettings();
        ContactInfo contact = settings.getContact();
        contact.setContactOrganization("GeoServer");
        contact.setOnlineResource("http://www.geoserver.org");
        gs.save(global);

        ppio = new KMLPPIO(gs);
    }

    @Test
    public void testEncodeLinestring() throws Exception {
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.LINES));
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) fti.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppio.encode(fc, bos);
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);
        checkValidationErorrs(dom, "./src/test/resources/org/geoserver/wps/ppio/ogckml22.xsd");

        // check the data was reprojected to wgs84
        assertEquals(
                "-92.99887316950249,4.523788751137377 -92.99842243632469,4.524241087719057",
                xpath.evaluate("//kml:LineString/kml:coordinates", dom));
        assertEquals(
                "t0001 ",
                xpath.evaluate(
                        "//kml:ExtendedData/kml:SchemaData/kml:SimpleData[@name='id']", dom));
    }

    @Test
    public void testEncodePolygon() throws Exception {
        FeatureTypeInfo fti =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) fti.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppio.encode(fc, bos);
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);
        checkValidationErorrs(dom, "./src/test/resources/org/geoserver/wps/ppio/ogckml22.xsd");
        assertEquals(
                "-1.0,5.0 2.0,5.0 2.0,2.0 -1.0,2.0 -1.0,5.0",
                xpath.evaluate(
                        "//kml:Placemark[@id='BasicPolygons.1107531493644']//kml:LinearRing/kml:coordinates",
                        dom));
    }

    @Test
    public void testEncodePoints() throws Exception {
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POINTS));
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) fti.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppio.encode(fc, bos);
        Document dom = dom(new ByteArrayInputStream(bos.toByteArray()));
        // print(dom);
        checkValidationErorrs(dom, "./src/test/resources/org/geoserver/wps/ppio/ogckml22.xsd");
        assertEquals(1, xpath.getMatchingNodes("//kml:Placemark", dom).getLength());
        assertEquals(
                "t0000",
                xpath.evaluate(
                        "//kml:ExtendedData/kml:SchemaData/kml:SimpleData[@name='id']", dom));
        assertEquals(
                "-92.99954926766114,4.52401492058674",
                xpath.evaluate("//kml:Point/kml:coordinates", dom));
        assertEquals(
                "t0000",
                xpath.evaluate(
                        "//kml:ExtendedData/kml:SchemaData/kml:SimpleData[@name='id']", dom));
    }
}

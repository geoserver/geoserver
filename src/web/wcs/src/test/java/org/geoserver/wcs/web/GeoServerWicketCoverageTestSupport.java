/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.security.AccessMode;
import org.geoserver.web.GeoServerWicketTestSupport;

public abstract class GeoServerWicketCoverageTestSupport extends GeoServerWicketTestSupport {

    protected static Catalog catalog;
    protected static XpathEngine xp;

    // WCS 1.1
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_DEM = new QName(WCS_URI, "DEM", WCS_PREFIX);
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName ROTATED_CAD = new QName(WCS_URI, "RotatedCad", WCS_PREFIX);
    public static QName WORLD = new QName(WCS_URI, "World", WCS_PREFIX);
    public static String TIFF = "tiff";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // addUser("admin", "geoxserver", null, Arrays.asList("ROLE_ADMINISTRATOR"));
        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("feature", "http://geoserver.sf.net");

        testData.registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        addWcs11Coverages(testData);
    }

    /** Adds the wcs 1.1 coverages. */
    public void addWcs11Coverages(SystemTestData testData) throws Exception {
        String styleName = "raster";
        testData.addStyle(styleName, "raster.sld", MockData.class, getCatalog());

        Map<LayerProperty, Object> props = new HashMap<SystemTestData.LayerProperty, Object>();
        props.put(LayerProperty.STYLE, styleName);

        // wcs 1.1
        testData.addRasterLayer(
                TASMANIA_DEM, "tazdem.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(
                TASMANIA_BM, "tazbm.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(
                ROTATED_CAD, "rotated.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(WORLD, "world.tiff", TIFF, props, MockData.class, getCatalog());
    }
}

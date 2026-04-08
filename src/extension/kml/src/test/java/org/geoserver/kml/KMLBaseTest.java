/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.w3c.dom.Document;

/** Base test class that centralizes common setup for KML/KMZ tests. */
public abstract class KMLBaseTest extends org.geoserver.wms.WMSTestSupport {

    protected static final QName BOULDER = new QName(MockData.SF_URI, "boulder", MockData.SF_PREFIX);
    protected static final QName STORM_OBS = new QName(MockData.CITE_URI, "storm_obs", MockData.CITE_PREFIX);
    protected static TimeZone oldTimeZone;

    protected XpathEngine xpath;

    @Before
    public void setUpXpath() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @BeforeClass
    public static void setTimeZone() {
        oldTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void clearTimeZone() {
        TimeZone.setDefault(oldTimeZone);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("notthere", "notthere.sld", getClass(), catalog);
        testData.addStyle("bridge", "bridge.sld", getClass(), catalog);
        testData.addStyle("scaleRange", "scaleRange.sld", getClass(), catalog);
        testData.addStyle("outputMode", "outputMode.sld", getClass(), catalog);
        testData.addVectorLayer(STORM_OBS, Collections.emptyMap(), "storm_obs.properties", getClass(), catalog);

        Map<LayerProperty, Object> properties = new HashMap<>();
        properties.put(
                LayerProperty.LATLON_ENVELOPE,
                new ReferencedEnvelope(-105.336, -105.112, 39.9, 40.116, CRS.decode("EPSG:4326")));
        properties.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope(3045967, 3108482, 1206627, 1285209, CRS.decode("EPSG:2876")));
        properties.put(LayerProperty.SRS, 2876);
        testData.addVectorLayer(BOULDER, properties, "boulder.properties", getClass(), catalog);

        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();

        setNativeBox(catalog, points);
        setNativeBox(catalog, lines);
        setNativeBox(catalog, polygons);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getStyles().add(catalog.getStyleByName("point"));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getStyles().add(catalog.getStyleByName("line"));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        lg.getStyles().add(catalog.getStyleByName("polygon"));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);

        // set up some layer titles
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.BRIDGES));
        fti.setTitle("Bridges Title");
        fti.setAbstract("Bridges Abstract");
        catalog.save(fti);
        fti = catalog.getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        fti.setTitle("Polygons Title");
        fti.setAbstract("");
        catalog.save(fti);
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_DEM));
        ci.setTitle("Tasmania DEM");
        ci.setAbstract(null);
        catalog.save(ci);
    }

    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }

    /**
     * Checks that a point's coordinate in the kml doc matches the expected value
     *
     * @param doc The KML doc
     * @param path The xpath leading to the coordinate
     * @param expected The expected ordinate values
     */
    public static void assertPointCoordinate(Document doc, String path, double... expected) throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String coordinates = xpath.evaluate(path, doc);
        org.hamcrest.MatcherAssert.assertThat(
                coordinates, org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString()));
        double[] ordinates = java.util.Arrays.stream(coordinates.split("\\s*,\\s*"))
                .mapToDouble(Double::parseDouble)
                .toArray();
        org.junit.Assert.assertEquals(expected.length, ordinates.length);
        for (int i = 0; i < expected.length; i++) {
            org.junit.Assert.assertEquals(expected[i], ordinates[i], 1e-6);
        }
    }
}

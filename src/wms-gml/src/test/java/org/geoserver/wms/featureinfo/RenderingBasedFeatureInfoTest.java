/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.GetMapTest;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

public class RenderingBasedFeatureInfoTest extends WMSTestSupport {

    private static final XpathEngine XPATH = XMLUnit.newXpathEngine();

    static {
        // setup XPATH engine namespaces
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("gs", "http://geoserver.org");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("cite", "http://www.opengis.net/cite");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        XPATH.setNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    public static QName GRID = new QName(MockData.CITE_URI, "grid", MockData.CITE_PREFIX);
    public static QName REPEATED = new QName(MockData.CITE_URI, "repeated", MockData.CITE_PREFIX);
    public static QName GIANT_POLYGON = new QName(MockData.CITE_URI, "giantPolygon", MockData.CITE_PREFIX);
    public static QName GEOM_FUNCTION = new QName(MockData.CITE_URI, "geom-function", MockData.CITE_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle("box-offset", "box-offset.sld", this.getClass(), getCatalog());
        testData.addStyle("transparent-fill", "transparent-fill.sld", this.getClass(), getCatalog());
        try (InputStream is = GetMapTest.class.getResourceAsStream("featureinfo/box-offset.png")) {
            FileUtils.copyInputStreamToFile(is, new File(testData.getDataDirectoryRoot(), "styles/box-offset.png"));
        }

        testData.addVectorLayer(
                GRID, Collections.emptyMap(), "grid.properties", RenderingBasedFeatureInfoTest.class, getCatalog());
        testData.addVectorLayer(
                REPEATED,
                Collections.emptyMap(),
                "repeated_lines.properties",
                RenderingBasedFeatureInfoTest.class,
                getCatalog());
        testData.addVectorLayer(
                GIANT_POLYGON, Collections.emptyMap(), "giantPolygon.properties", SystemTestData.class, getCatalog());
        testData.addVectorLayer(
                GEOM_FUNCTION,
                Collections.emptyMap(),
                "geom-function.properties",
                RenderingBasedFeatureInfoTest.class,
                getCatalog());

        testData.addStyle("ranged", "ranged.sld", this.getClass(), getCatalog());
        testData.addStyle("dynamic", "dynamic.sld", this.getClass(), getCatalog());
        testData.addStyle("symbol-uom", "symbol-uom.sld", this.getClass(), getCatalog());
        testData.addStyle("two-rules", "two-rules.sld", this.getClass(), getCatalog());
        testData.addStyle("two-fts", "two-fts.sld", this.getClass(), getCatalog());
        testData.addStyle("dashed", "dashed.sld", this.getClass(), getCatalog());
        testData.addStyle("dashed-exp", "dashed-exp.sld", this.getClass(), getCatalog());
        testData.addStyle("polydash", "polydash.sld", this.getClass(), getCatalog());
        testData.addStyle("doublepoly", "doublepoly.sld", this.getClass(), getCatalog());
        testData.addStyle("pureLabel", "purelabel.sld", this.getClass(), getCatalog());
        testData.addStyle("transform", "transform.sld", this.getClass(), getCatalog());
        testData.addStyle("geom-function", "geom-function.sld", this.getClass(), getCatalog());
    }

    @After
    public void cleanup() {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        // make sure GetFeatureInfo is not deactivated (this will only update the global service)
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(false);
        getGeoServer().save(wms);
    }

    @Test
    public void testGetFeatureInfoReprojectionWithoutRendering() throws Exception {
        // disable WMS get feature info with rendering
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        // request using EPSG:3857 a layer that uses EPSG:4326
        String url =
                "wms?REQUEST=GetFeatureInfo&BBOX=21.1507032494,76.8104486492,23.3770930655,79.0368384649&SERVICE=WMS"
                        + "&INFO_FORMAT=text/xml; subtype=gml/3.1.1&QUERY_LAYERS=cite%3ABridges&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100"
                        + "&format=image%2Fpng&styles=box-offset&srs=EPSG%3A3857&version=1.1.1&x=50&y=63&feature_count=50";
        Document result = getAsDOM(url);
        // check the response content, the features should have been reproject from EPSG:4326 to
        // EPSG:3857
        String srs = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/@srsName",
                result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("3857"), is(true));
        String rawCoordinates = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/gml:pos/text()",
                result);
        checkCoordinates(rawCoordinates, 0.0001, 22.26389816, 77.92364356);
        // disable feature reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the get feature info request
        result = getAsDOM(url);
        // check that features were not reprojected
        srs = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/@srsName",
                result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("4326"), is(true));
        rawCoordinates = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/gml:pos/text()",
                result);
        checkCoordinates(rawCoordinates, 0.0001, 0.0002, 0.0007);
    }

    @Test
    public void testGetFeatureInfoReprojectionWithRendering() throws Exception {
        // request using EPSG:3857 a layer that uses EPSG:4326
        String url =
                "wms?REQUEST=GetFeatureInfo&BBOX=-304226.149584,7404818.42511,947357.141801,10978414.0796&SERVICE=WMS"
                        + "&INFO_FORMAT=text/xml; subtype=gml/3.1.1&QUERY_LAYERS=GenericEntity&Layers=GenericEntity"
                        + "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=line&srs=EPSG%3A3857&version=1.1.1&x=284&y=269";
        Document result = getAsDOM(url);
        // check the response content, the features should have been reproject from EPSG:4326 to
        // EPSG:3857
        String srs = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/@srsName",
                result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("3857"), is(true));
        String exteriorLinearRing = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                        + "gml:exterior/gml:LinearRing/gml:posList/text()",
                result);
        checkCoordinates(
                exteriorLinearRing,
                0.0001,
                0,
                8511908.69220489,
                0,
                9349764.17414691,
                695746.81745796,
                9349764.17414691,
                695746.81745796,
                8511908.69220489,
                0,
                8511908.69220489);
        String interiorLinearRing = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                        + "gml:interior/gml:LinearRing/gml:posList/text()",
                result);
        checkCoordinates(
                interiorLinearRing,
                0.0001,
                222638.98158655,
                8741545.4358357,
                222638.98158655,
                8978686.31934769,
                445277.96317309,
                8859142.8005657,
                222638.98158655,
                8741545.4358357);
        // disable feature reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the get feature info request
        result = getAsDOM(url);
        // check that features were not reprojected
        srs = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/@srsName",
                result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("4326"), is(true));
        exteriorLinearRing = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                        + "gml:exterior/gml:LinearRing/gml:posList/text()",
                result);
        checkCoordinates(exteriorLinearRing, 0.0001, 0, 60.5, 0, 64, 6.25, 64, 6.25, 60.5, 0, 60.5);
        interiorLinearRing = XPATH.evaluate(
                "//wfs:FeatureCollection/gml:featureMembers/"
                        + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                        + "gml:interior/gml:LinearRing/gml:posList/text()",
                result);
        checkCoordinates(interiorLinearRing, 0.0001, 2, 61.5, 2, 62.5, 4, 62, 2, 61.5);
    }

    /**
     * Helper method that checks if the string represented coordinates correspond to the expected ones. The provided
     * precision will be used to compare the numeric values.
     */
    private void checkCoordinates(String rawCoordinates, double precision, double... expected) {
        assertThat(rawCoordinates, notNullValue());
        rawCoordinates = rawCoordinates.trim();
        String[] coordinates = rawCoordinates.split("\\s");
        assertThat(coordinates.length, is(expected.length));
        for (int i = 0; i < coordinates.length; i++) {
            checkNumberSimilar(coordinates[i], expected[i], precision);
        }
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.versioning;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.xml.v1_0_0.WFS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class TimeVersioningTest extends GeoServerSystemTestSupport {

    private XpathEngine WFS20_XPATH_ENGINE;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // create bounding box definitions
        ReferencedEnvelope envelope = new ReferencedEnvelope(-5, -5, 5, 5, DefaultGeographicCRS.WGS84);
        Map<SystemTestData.LayerProperty, Object> properties = new HashMap<>();
        properties.put(SystemTestData.LayerProperty.LATLON_ENVELOPE, envelope);
        properties.put(SystemTestData.LayerProperty.ENVELOPE, envelope);
        properties.put(SystemTestData.LayerProperty.SRS, 4326);
        // create versioned layer
        QName versionedLayerName = new QName(MockData.DEFAULT_URI, "versioned", MockData.DEFAULT_PREFIX);
        testData.addVectorLayer(versionedLayerName, properties, "versioned.properties", getClass(), getCatalog());
        // instantiate xpath engine
        WFS20_XPATH_ENGINE = buildXpathEngine(
                "wfs", "http://www.opengis.net/wfs/2.0",
                "gml", "http://www.opengis.net/gml/3.2");
    }

    @Before
    public void beforeTest() {
        // activate versioning for versioned layer
        TestsUtils.updateFeatureTypeTimeVersioning(getCatalog(), "gs:versioned", true, "NAME", "TIME");
    }

    @Test
    public void testInsertVersionedFeature() throws Exception {
        MockHttpServletResponse result = postAsServletResponse("wfs", TestsUtils.readResource("/requests/insert_request_1.xml"));
        assertThat(result.getStatus(), is(200));

        List<SimpleFeature> features = TestsUtils.searchFeatures(getCatalog(), "gs:versioned");
        List<SimpleFeature> foundFeatures = TestsUtils.searchFeatures(features, "NAME", "TIME", "Feature_2", new Date(), 300);
        assertThat(foundFeatures.size(), is(1));
        SimpleFeature foundFeature = foundFeatures.get(0);
        String description = (String) foundFeature.getAttribute("DESCRIPTION");
        assertThat(description, is("INSERT_1"));
    }

    @Test
    public void testGetFeatureVersioned() throws Exception {
        Document result = postAsDOM("wfs", TestsUtils.readResource("/requests/get_request_1.xml"));
    }

    @Test
    public void testUpdateVersionedFeature() throws Exception {
        Document result = postAsDOM("wfs", TestsUtils.readResource("/requests/update_request_1.xml"));
    }

    /**
     * Helper method that builds a xpath engine using some predefined
     * namespaces and all the catalog namespaces. The provided namespaces
     * will be added overriding any existing namespace.
     */
    private XpathEngine buildXpathEngine(String... namespaces) {
        // build xpath engine
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        Map<String, String> finalNamespaces = new HashMap<>();
        // add common namespaces
        finalNamespaces.put("ows", "http://www.opengis.net/ows");
        finalNamespaces.put("ogc", "http://www.opengis.net/ogc");
        finalNamespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        finalNamespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        finalNamespaces.put("xlink", "http://www.w3.org/1999/xlink");
        finalNamespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        // add al catalog namespaces
        getCatalog().getNamespaces().forEach(namespace ->
                finalNamespaces.put(namespace.getPrefix(), namespace.getURI()));
        // add provided namespaces
        if (namespaces.length % 2 != 0) {
            throw new RuntimeException("Invalid number of namespaces provided.");
        }
        for (int i = 0; i < namespaces.length; i += 2) {
            finalNamespaces.put(namespaces[i], namespaces[i + 1]);
        }
        // add namespaces to the xpath engine
        xpathEngine.setNamespaceContext(new SimpleNamespaceContext(finalNamespaces));
        return xpathEngine;
    }



    /**
     * Check the result of a WFS 2.0 get feature request targeting stations data set.
     */
    private void checkWfs20StationsGetFeatureResult(Document document) {
        checkCount(WFS20_XPATH_ENGINE, document, 1, "/wfs:FeatureCollection/wfs:member/" +
                "st_gml32:Station_gml32[@gml:id='st.1']/st_gml32:measurements/ms_gml32:Measurement[ms_gml32:name='temperature']");
        checkCount(WFS20_XPATH_ENGINE, document, 1, "/wfs:FeatureCollection/wfs:member/" +
                "st_gml32:Station_gml32[@gml:id='st.1']/st_gml32:location/gml:Point[gml:pos='1.0 -1.0']");
    }

    /**
     * Helper method that evaluates a xpath and checks if the number of nodes found
     * correspond to the expected number,
     */
    private void checkCount(XpathEngine xpathEngine, Document document, int expectedCount, String xpath) {
        try {
            // evaluate the xpath and compare the number of nodes found
            MatcherAssert.assertThat(xpathEngine.getMatchingNodes(xpath, document).getLength(), CoreMatchers.is(expectedCount));
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }
}

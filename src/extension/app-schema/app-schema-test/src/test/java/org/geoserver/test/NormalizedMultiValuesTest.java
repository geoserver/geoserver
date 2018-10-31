/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2018, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.feature.NameImpl;
import org.geotools.jdbc.JDBCFeatureStore;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;

/** Contains tests related with JDBC multiple values support. */
public final class NormalizedMultiValuesTest extends AbstractAppSchemaTestSupport {

    // xpath engines used to check WFS responses
    private XpathEngine WFS11_XPATH_ENGINE;
    private XpathEngine WFS20_XPATH_ENGINE;

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        WFS11_XPATH_ENGINE =
                StationsMockData.buildXpathEngine(
                        getTestData().getNamespaces(),
                        "ows",
                        "http://www.opengis.net/ows",
                        "wfs",
                        "http://www.opengis.net/wfs",
                        "gml",
                        "http://www.opengis.net/gml");
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE =
                StationsMockData.buildXpathEngine(
                        getTestData().getNamespaces(),
                        "ows",
                        "http://www.opengis.net/ows/1.1",
                        "wfs",
                        "http://www.opengis.net/wfs/2.0",
                        "gml",
                        "http://www.opengis.net/gml/3.2");
    }

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        return new MockData();
    }

    /** Helper class that will setup custom complex feature types using the stations data set. */
    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add GML 3.1 namespaces
            putNamespace(STATIONS_PREFIX_GML31, STATIONS_URI_GML31);
            putNamespace(MEASUREMENTS_PREFIX_GML31, MEASUREMENTS_URI_GML31);
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);
            // add GML 3.1 feature types
            Map<String, String> gml31Parameters = new HashMap<>();
            gml31Parameters.put("GML_PREFIX", "gml31");
            gml31Parameters.put("GML_PREFIX_UPPER", "GML31");
            gml31Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml");
            gml31Parameters.put(
                    "GML_LOCATION", "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");
            addAppSchemaFeatureType(
                    STATIONS_PREFIX_GML31,
                    "gml31",
                    "Station_gml31",
                    "/test-data/stations/multiValues/stations.xml",
                    gml31Parameters,
                    "/test-data/stations/multiValues/stations.xsd",
                    "/test-data/stations/multiValues/stations.properties",
                    "/test-data/stations/multiValues/measurements.xml",
                    "/test-data/stations/multiValues/measurements.xsd",
                    "/test-data/stations/multiValues/measurements.properties",
                    "/test-data/stations/multiValues/tags.properties");
            // add GML 3.2 feature types
            Map<String, String> gml32Parameters = new HashMap<>();
            gml32Parameters.put("GML_PREFIX", "gml32");
            gml32Parameters.put("GML_PREFIX_UPPER", "GML32");
            gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
            gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            addAppSchemaFeatureType(
                    STATIONS_PREFIX_GML32,
                    "gml32",
                    "Station_gml32",
                    "/test-data/stations/multiValues/stations.xml",
                    gml32Parameters,
                    "/test-data/stations/multiValues/stations.xsd",
                    "/test-data/stations/multiValues/stations.properties",
                    "/test-data/stations/multiValues/measurements.xml",
                    "/test-data/stations/multiValues/measurements.xsd",
                    "/test-data/stations/multiValues/measurements.properties",
                    "/test-data/stations/multiValues/tags.properties");
        }
    }

    @Test
    public void testGetAllNormalizedMultiValuesWfs11() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        // execute the WFS 1.1.0 request
        String request = "wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31";
        Document document = getAsDOM(request);
        // check that we have two complex features
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                2,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");
        // check that the expected stations and measurements are present
        checkStation1Gml31(document);
        checkStation2Gml31(document);
    }

    @Test
    public void testGetAllNormalizedMultiValuesWfs20() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        // execute the WFS 2.0 request
        String request = "wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32";
        Document document = getAsDOM(request);
        // check that we have two complex features
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                2,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32");

        // check that the expected stations and measurements are present
        checkStation1Gml32(document);
        checkStation2Gml32(document);
    }

    @Test
    public void testGetFilteredNormalizedMultiValuesWfs11() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        // execute the WFS 1.1.0 request
        String request = "wfs";
        Document document =
                postAsDOM(
                        request,
                        readResource(
                                "/test-data/stations/multiValues/requests/station_tag_filter_wfs11_1.xml"));
        // check that we got he correct station
        checkStation1Gml31(document);
    }

    @Test
    public void testGetFilteredNormalizedMultiValuesWfs20() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        // execute the WFS 2.0 request
        String request = "wfs";
        Document document =
                postAsDOM(
                        request,
                        readResource(
                                "/test-data/stations/multiValues/requests/station_tag_filter_wfs20_1.xml"));
        // check that we got he correct station
        checkStation1Gml32(document);
    }

    /** Helper method that checks that station 1 is present in the provided document. */
    private void checkStation1Gml31(Document document) {
        // check station exists
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']");
        // check stations tags
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.1']"
                        + "[st_gml31:tag='st_1_tag_a']/st_gml31:tag[@st_gml31:code='1']");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.1']"
                        + "[st_gml31:tag='st_1_tag_b']/st_gml31:tag[@st_gml31:code='2']");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.1']"
                        + "[st_gml31:tag='europe']/st_gml31:tag[@st_gml31:code='3']");
        // check measurements
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.1']"
                        + "/st_gml31:measurements/ms_gml31:Measurement_gml31"
                        + "[@gml:id='ms.1']"
                        + "[ms_gml31:tag='temperature_tag']"
                        + "[ms_gml31:tag='desert_tag']");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.1']"
                        + "/st_gml31:measurements/ms_gml31:Measurement_gml31"
                        + "[@gml:id='ms.2']"
                        + "[ms_gml31:tag='wind_tag']");
    }

    /** Helper method that checks that station 2 is present in the provided document. */
    private void checkStation2Gml31(Document document) {
        // check station exists
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.2']");
        // check stations tags
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.2']"
                        + "[st_gml31:tag='st_2_tag_a']/st_gml31:tag[@st_gml31:code='4']");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.2']"
                        + "[st_gml31:tag='st_2_tag_b']/st_gml31:tag[@st_gml31:code='5']");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.2']"
                        + "[st_gml31:tag='europe']/st_gml31:tag[@st_gml31:code='6']");
        // check measurements
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31"
                        + "[@gml:id='st.2']"
                        + "/st_gml31:measurements/ms_gml31:Measurement_gml31"
                        + "[@gml:id='ms.3']"
                        + "[ms_gml31:tag='pressure_tag']");
    }

    /** Helper method that checks that station 1 is present in the provided document. */
    private void checkStation1Gml32(Document document) {
        // check station exists
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id='st.1']");
        // check stations tags
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.1']"
                        + "[st_gml32:tag='st_1_tag_a']/st_gml32:tag[@st_gml32:code='1']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.1']"
                        + "[st_gml32:tag='st_1_tag_b']/st_gml32:tag[@st_gml32:code='2']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.1']"
                        + "[st_gml32:tag='europe']/st_gml32:tag[@st_gml32:code='3']");
        // check measurements
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.1']"
                        + "/st_gml32:measurements/ms_gml32:Measurement_gml32"
                        + "[@gml:id='ms.1']"
                        + "[ms_gml32:tag='temperature_tag']"
                        + "[ms_gml32:tag='desert_tag']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.1']"
                        + "/st_gml32:measurements/ms_gml32:Measurement_gml32"
                        + "[@gml:id='ms.2']"
                        + "[ms_gml32:tag='wind_tag']");
    }

    /** Helper method that checks that station 2 is present in the provided document. */
    private void checkStation2Gml32(Document document) {
        // check station exists
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id='st.2']");
        // check stations tags
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.2']"
                        + "[st_gml32:tag='st_2_tag_a']/st_gml32:tag[@st_gml32:code='4']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.2']"
                        + "[st_gml32:tag='st_2_tag_b']/st_gml32:tag[@st_gml32:code='5']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.2']"
                        + "[st_gml32:tag='europe']/st_gml32:tag[@st_gml32:code='6']");
        // check measurements
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32"
                        + "[@gml:id='st.2']"
                        + "/st_gml32:measurements/ms_gml32:Measurement_gml32"
                        + "[@gml:id='ms.3']"
                        + "[ms_gml32:tag='pressure_tag']");
    }

    /**
     * Helper method that evaluates a xpath and checks if the number of nodes found correspond to
     * the expected number,
     */
    private void checkCount(
            XpathEngine xpathEngine, Document document, int expectedCount, String xpath) {
        try {
            // evaluate the xpath and compare the number of nodes found
            assertThat(
                    xpathEngine.getMatchingNodes(xpath, document).getLength(), is(expectedCount));
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }

    /**
     * Helper method that checks if this is an online test not based on a JDBC data store.
     *
     * @return TRUE if this is an online test not based on a JDBC data store
     */
    private boolean notJdbcBased() throws Exception {
        // get the App-Schema data store
        FeatureTypeInfo featureTypeInfo = getCatalog().getFeatureTypeByName("Station_gml31");
        DataAccess dataAccess = featureTypeInfo.getStore().getDataStore(null);
        AppSchemaDataAccess appSchemaDataAccess = (AppSchemaDataAccess) dataAccess;
        // get the feature type mapping corresponding to the stations complex feature type
        Name name = new NameImpl("http://www.stations_gml31.org/1.0", "Station_gml31");
        FeatureSource featureSource = appSchemaDataAccess.getMappingByName(name).getSource();
        return !(featureSource instanceof JDBCFeatureStore);
    }
}

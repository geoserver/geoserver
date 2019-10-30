/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
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

public class UnboundedSequenceMultiValuesTest extends AbstractAppSchemaTestSupport {

    private static final String TEST_RESOURCES_FOLDER = "/test-data/stations/unboundedSequence/";

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
                    TEST_RESOURCES_FOLDER + "stations.xml",
                    gml31Parameters,
                    TEST_RESOURCES_FOLDER + "stations.xsd",
                    TEST_RESOURCES_FOLDER + "stations.properties",
                    TEST_RESOURCES_FOLDER + "measurements.xml",
                    TEST_RESOURCES_FOLDER + "measurements.xsd",
                    TEST_RESOURCES_FOLDER + "measurements.properties",
                    TEST_RESOURCES_FOLDER + "maintainers.properties",
                    TEST_RESOURCES_FOLDER + "tags.properties");
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
                    TEST_RESOURCES_FOLDER + "stations.xml",
                    gml32Parameters,
                    TEST_RESOURCES_FOLDER + "stations.xsd",
                    TEST_RESOURCES_FOLDER + "stations.properties",
                    TEST_RESOURCES_FOLDER + "measurements.xml",
                    TEST_RESOURCES_FOLDER + "measurements.xsd",
                    TEST_RESOURCES_FOLDER + "measurements.properties",
                    TEST_RESOURCES_FOLDER + "maintainers.properties",
                    TEST_RESOURCES_FOLDER + "tags.properties");
        }
    }

    @Test
    public void testGetAllNormalizedMultiValuesWfs() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        // execute the WFS 1.1.0 request
        Document document =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkStationGml31(document);
        // execute the WFS 2.0.0 request
        document = getAsDOM("wfs?request=GetFeature&version=2.0.0&typename=st_gml32:Station_gml32");
        checkStationGml32(document);
    }

    @Test
    public void testFilterMultiValuesWfs() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }

        String filter =
                "<wfs:GetFeature service=\"WFS\" version=\"2.0.0\"\n"
                        + "    xmlns:fes=\"http://www.opengis.net/fes/2.0\"\n"
                        + "    xmlns:st_gml32=\"http://www.stations_gml32.org/1.0\"\n"
                        + "    xmlns:wfs=\"http://www.opengis.net/wfs/2.0\">\n"
                        + "    <wfs:Query typeNames=\"st_gml32:Station_gml32\">\n"
                        + "        <fes:Filter>\n"
                        + "            <fes:PropertyIsEqualTo>\n"
                        + "                <fes:ValueReference>st_gml32:Station_gml32/st_gml32:maintainer/st_gml32:name\n"
                        + "                </fes:ValueReference>\n"
                        + "                <fes:Literal>mnt_c</fes:Literal>\n"
                        + "            </fes:PropertyIsEqualTo>\n"
                        + "        </fes:Filter>\n"
                        + "    </wfs:Query>\n"
                        + "</wfs:GetFeature>";
        Document document = postAsDOM("wfs", filter);

        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id='st.2']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                3,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32/st_gml32:maintainer/st_gml32:name");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32/st_gml32:maintainer/st_gml32:name"
                        + "[text()='mnt_c']");

        filter =
                readResource(
                        "/test-data/stations/unboundedSequence/requests/station_mainainer_filter_wfs11.xml");
        document = postAsDOM("wfs", filter);

        final String stationXpath =
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31";
        checkCount(WFS11_XPATH_ENGINE, document, 1, stationXpath + "[@gml:id='st.2']");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                3,
                stationXpath + "/st_gml31:maintainer/st_gml31:name");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                stationXpath + "/st_gml31:maintainer/st_gml31:name" + "[text()='mnt_c']");
    }

    private void checkStationGml31(Document document) {
        // check that we have two complex features
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                3,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                2,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:level");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:level[text()='71']");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:level[@xs:nil='true']");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                2,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:name");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:name[text()='mnt_a']");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:name[text()='mnt_b']");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                2,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:classType");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:classType[text()='st_1_mnt_a']");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.1']"
                        + "/st_gml31:maintainer/st_gml31:classType[text()='st_1_mnt_b']");
    }

    private void checkStationGml32(Document document) {
        final String stationsXpath = "/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32";
        // check that we have two complex features
        checkCount(WFS20_XPATH_ENGINE, document, 3, stationsXpath);
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                2,
                stationsXpath + "[@gml:id='st.1']" + "/st_gml32:maintainer/st_gml32:level");

        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                stationsXpath + "[@gml:id='st.1']/st_gml32:maintainer/st_gml32:level[text()='71']");

        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                stationsXpath
                        + "[@gml:id='st.1']/st_gml32:maintainer/st_gml32:level[@xs:nil='true']");
    }

    /**
     * Helper method that evaluates a xpath and checks if the number of nodes found correspond to
     * the expected number.
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


/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.util.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests that namespaces are correctly handled by WFS and app-schema when
 * features belonging to different namespaces are chained together.
 */
public final class NamespacesWfsTest extends AbstractAppSchemaTestSupport {

    private static final File TEST_ROOT_DIRECTORY;

    static {
        try {
            // create the tests root directory
            TEST_ROOT_DIRECTORY = IOUtils.createTempDirectory("app-schema-stations");
        } catch (Exception exception) {
            throw new RuntimeException("Error creating temporary directory.", exception);
        }
    }

    @AfterClass
    public static void afterTests() {
        try {
            // remove tests root directory
            IOUtils.delete(TEST_ROOT_DIRECTORY);
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error removing tests root directory '%s'.", TEST_ROOT_DIRECTORY.getAbsolutePath()));
        }
    }

    /**
     * Helper class that will setup custom complex feature types using the stations data set.
     * Parameterization will be used to setup complex features types for GML31 and GML32 based
     * on the same mappings files and schemas.
     */
    private static final class MockData extends AbstractAppSchemaMockData {

        // stations GML 3.1 namespaces
        private static final String STATIONS_PREFIX_GML31 = "st_gml31";
        private static final String STATIONS_URI_GML31 = "http://www.stations_gml31.org/1.0";
        private static final String MEASUREMENTS_PREFIX_GML31 = "ms_gml31";
        private static final String MEASUREMENTS_URI_GML31 = "http://www.measurements_gml31.org/1.0";

        // stations GML 3.2 namespaces
        private static final String STATIONS_PREFIX_GML32 = "st_gml32";
        private static final String STATIONS_URI_GML32 = "http://www.stations_gml32.org/1.0";
        private static final String MEASUREMENTS_PREFIX_GML32 = "ms_gml32";
        private static final String MEASUREMENTS_URI_GML32 = "http://www.measurements_gml32.org/1.0";

        @Override
        public void addContent() {
            // add GML 3.1 namespaces
            putNamespace(STATIONS_PREFIX_GML31, STATIONS_URI_GML31);
            putNamespace(MEASUREMENTS_PREFIX_GML31, MEASUREMENTS_URI_GML31);
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);
            // add GML 3.1 feature type
            Map<String, String> gml31Parameters = new HashMap<>();
            gml31Parameters.put("GML_PREFIX", "gml31");
            gml31Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml");
            gml31Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");
            addFeatureType(STATIONS_PREFIX_GML31, "gml31", gml31Parameters);
            // add GML 3.1 feature type
            Map<String, String> gml32Parameters = new HashMap<>();
            gml32Parameters.put("GML_PREFIX", "gml32");
            gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
            gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            addFeatureType(STATIONS_PREFIX_GML32, "gml32", gml32Parameters);
        }

        /**
         * Helper method that will add the stations feature type customizing
         * it for the desired GML version.
         */
        private void addFeatureType(String namespacePrefix, String gmlPrefix, Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = new File(TEST_ROOT_DIRECTORY, gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File stationsMappings = new File(gmlDirectory, String.format("stations_%s.xml", gmlPrefix));
            File stationsProperties = new File(gmlDirectory, String.format("stations_%s.properties", gmlPrefix));
            File stationsSchema = new File(gmlDirectory, String.format("stations_%s.xsd", gmlPrefix));
            File measurementsMappings = new File(gmlDirectory, String.format("measurements_%s.xml", gmlPrefix));
            File measurementsProperties = new File(gmlDirectory, String.format("measurements_%s.properties", gmlPrefix));
            File measurementsSchema = new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters("/test-data/stations/mappings/stations.xml", parameters, stationsMappings);
            substituteParameters("/test-data/stations/data/stations.properties", parameters, stationsProperties);
            substituteParameters("/test-data/stations/schemas/stations.xsd", parameters, stationsSchema);
            substituteParameters("/test-data/stations/mappings/measurements.xml", parameters, measurementsMappings);
            substituteParameters("/test-data/stations/data/measurements.properties", parameters, measurementsProperties);
            substituteParameters("/test-data/stations/schemas/measurements.xsd", parameters, measurementsSchema);
            // create the feature type
            addFeatureType(namespacePrefix, String.format("Station_%s", gmlPrefix),
                    stationsMappings.getAbsolutePath(),
                    stationsProperties.getAbsolutePath(),
                    stationsSchema.getAbsolutePath(),
                    measurementsMappings.getAbsolutePath(),
                    measurementsProperties.getAbsolutePath(),
                    measurementsSchema.getAbsolutePath());
        }

        /**
         * Helper method that reads a resource to a string performs the
         * parameterization and writes the result to the provided new file.
         */
        private static void substituteParameters(String resourceName, Map<String, String> parameters, File newFile) {
            // read the resource content
            String resourceContent = resourceToString(resourceName);
            for (Map.Entry<String, String> parameter : parameters.entrySet()) {
                // substitute the parameter on the resource content
                resourceContent = resourceContent.replace(
                        String.format("${%s}", parameter.getKey()), parameter.getValue());
            }
            try {
                // write the final resource content to the provided location
                Files.write(newFile.toPath(), resourceContent.getBytes());
            } catch (Exception exception) {
                throw new RuntimeException(String.format(
                        "Error writing content to file '%s'.", newFile.getAbsolutePath()), exception);
            }
        }

        /**
         * Helper method the reads a resource content to a string.
         */
        private static String resourceToString(String resourceName) {
            try (InputStream input = NamespacesWfsTest.class.getResourceAsStream(resourceName)) {
                return IOUtils.toString(input);
            } catch (Exception exception) {
                throw new RuntimeException(String.format(
                        "Error reading resource '%s' content.", resourceName), exception);
            }
        }
    }

    @Override
    protected MockData createTestData() {
        // instantiate our custom complex types
        return new MockData();
    }

    // xpath engines used to check WFS responses
    private XpathEngine WFS11_XPATH_ENGINE;
    private XpathEngine WFS20_XPATH_ENGINE;

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        WFS11_XPATH_ENGINE = buildXpathEngine(
                "wfs", "http://www.opengis.net/wfs",
                "gml", "http://www.opengis.net/gml");
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE = buildXpathEngine(
                "wfs", "http://www.opengis.net/wfs/2.0",
                "gml", "http://www.opengis.net/gml/3.2");
    }

    @Test
    public void globalServiceNamespacesWfs11() throws Exception {
        Document document = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkWfs11StationsGetFeatureResult(document);
    }

    @Test
    public void virtualServiceNamespacesWfs11() throws Exception {
        Document document = getAsDOM("st_gml31/wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkWfs11StationsGetFeatureResult(document);
    }

    @Test
    public void globalServiceNamespacesWfs20() throws Exception {
        Document document = getAsDOM("wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkWfs20StationsGetFeatureResult(document);
    }

    @Test
    public void virtualServiceNamespacesWfs20() throws Exception {
        Document document = getAsDOM("st_gml32/wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkWfs20StationsGetFeatureResult(document);
    }

    /**
     * Check the result of a WFS 1.1 get feature request targeting stations data set.
     */
    private void checkWfs11StationsGetFeatureResult(Document document) {
        checkCount(WFS11_XPATH_ENGINE, document, 1, "/wfs:FeatureCollection/gml:featureMember/" +
                "st_gml31:Station_gml31[@gml:id='st.1']/st_gml31:measurements/ms_gml31:Measurement[ms_gml31:name='temperature']");
        checkCount(WFS11_XPATH_ENGINE, document, 1, "/wfs:FeatureCollection/gml:featureMember/" +
                "st_gml31:Station_gml31[@gml:id='st.1']/st_gml31:location/gml:Point[gml:pos='1.0 -1.0']");
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
            assertThat(xpathEngine.getMatchingNodes(xpath, document).getLength(), is(expectedCount));
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }

    /**
     * Helper method that builds a xpath engine that will use
     * the provided GML namespaces.
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
        finalNamespaces.putAll(getTestData().getNamespaces());
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
}

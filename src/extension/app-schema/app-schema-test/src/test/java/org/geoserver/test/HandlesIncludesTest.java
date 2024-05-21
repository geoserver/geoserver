/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests that the App-Schema module can handle includes in the GML configuration files, even if the
 * mapping is duplicated.
 */
public class HandlesIncludesTest extends StationsAppSchemaTestSupport {

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        return new HandlesIncludesMockData();
    }

    /**
     * Tests that the measurement feature type is included in the station feature type after being
     * loaded separately without throwing a DataSource exception.
     */
    @Test
    public void testGMLOutput32_With_Include() throws Exception {
        Document document =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st_gml32:Station_gml32");
        assertXpathEvaluatesTo(
                "http://www.measurements_gml32.org/1.0",
                "namespace-uri(//*[local-name()='Measurement_gml32'][1])",
                document);
    }

    private static class HandlesIncludesMockData extends StationsMockData {
        @Override
        public void addContent() {

            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);

            // add GML 3.2 feature type
            Map<String, String> gml32Parameters = new HashMap<>();
            gml32Parameters.put("GML_PREFIX", "gml32");
            gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
            gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            addMeasurementFeatureType(
                    MEASUREMENTS_PREFIX_GML32,
                    "gml32",
                    "measurements",
                    "handlesIncludes/measurements.xml",
                    gml32Parameters);
            addStationFeatureType(
                    STATIONS_PREFIX_GML32,
                    "gml32",
                    "stations",
                    "handlesIncludes/stations.xml",
                    gml32Parameters);
        }

        /**
         * Helper method that will add the station feature type customizing it for the desired GML
         * version.
         */
        @Override
        protected void addStationFeatureType(
                String namespacePrefix,
                String gmlPrefix,
                String mappingsName,
                String mappingsPath,
                Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = getDirectoryForGmlPrefix(gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File stationsMappings =
                    new File(gmlDirectory, String.format("%s_%s.xml", mappingsName, gmlPrefix));
            File stationsProperties =
                    new File(gmlDirectory, String.format("stations_%s.properties", gmlPrefix));
            File stationsSchema =
                    new File(gmlDirectory, String.format("stations_%s.xsd", gmlPrefix));
            File measurementsSchema =
                    new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters(
                    "/test-data/stations/" + mappingsPath, parameters, stationsMappings);
            substituteParameters(
                    "/test-data/stations/handlesIncludes/stations.properties",
                    parameters,
                    stationsProperties);
            substituteParameters(
                    "/test-data/stations/handlesIncludes/stations.xsd", parameters, stationsSchema);
            substituteParameters(
                    "/test-data/stations/handlesIncludes/measurements.xsd",
                    parameters,
                    measurementsSchema);
            // extra features to add:
            addStationFeatures(stationsProperties);
            // create station feature type
            addFeatureType(
                    namespacePrefix,
                    String.format("Station_%s", gmlPrefix),
                    stationsMappings.getAbsolutePath(),
                    stationsProperties.getAbsolutePath(),
                    stationsSchema.getAbsolutePath(),
                    measurementsSchema.getAbsolutePath());
        }

        /**
         * Helper method that will add the measurement feature type customizing it for the desired
         * GML version.
         */
        @Override
        protected void addMeasurementFeatureType(
                String namespacePrefix,
                String gmlPrefix,
                String mappingsName,
                String mappingsPath,
                Map<String, String> parameters) {
            // create root directory
            File gmlDirectory = getDirectoryForGmlPrefix(gmlPrefix);
            gmlDirectory.mkdirs();
            // add the necessary files
            File measurementsMappings =
                    new File(gmlDirectory, String.format("%s_%s.xml", mappingsName, gmlPrefix));
            File measurementsProperties =
                    new File(gmlDirectory, String.format("measurements_%s.properties", gmlPrefix));
            File measurementsSchema =
                    new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
            // perform the parameterization
            substituteParameters(
                    "/test-data/stations/" + mappingsPath, parameters, measurementsMappings);
            substituteParameters(
                    "/test-data/stations/handlesIncludes/measurements.properties",
                    parameters,
                    measurementsProperties);
            substituteParameters(
                    "/test-data/stations/handlesIncludes/measurements.xsd",
                    parameters,
                    measurementsSchema);
            // add extra features
            addMeasurementFeatures(measurementsProperties);
            // create measurements feature type
            addFeatureType(
                    namespacePrefix,
                    String.format("Measurement_%s", gmlPrefix),
                    measurementsMappings.getAbsolutePath(),
                    measurementsProperties.getAbsolutePath(),
                    measurementsSchema.getAbsolutePath());
        }
    }
}

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * Helper class that will setup custom complex feature types using the stations data set. Parameterization will be used
 * to setup complex features types for GML31 and GML32 based on the same mappings files and schemas.
 */
public class StationsMockData extends AbstractAppSchemaMockData {

    private static final Logger LOGGER = Logging.getLogger(StationsMockData.class);

    // stations GML 3.1 namespaces
    protected static final String STATIONS_PREFIX_GML31 = "st_gml31";
    protected static final String STATIONS_URI_GML31 = "http://www.stations_gml31.org/1.0";
    protected static final String MEASUREMENTS_PREFIX_GML31 = "ms_gml31";
    protected static final String MEASUREMENTS_URI_GML31 = "http://www.measurements_gml31.org/1.0";

    // stations GML 3.2 namespaces
    protected static final String STATIONS_PREFIX_GML32 = "st_gml32";
    protected static final String STATIONS_URI_GML32 = "http://www.stations_gml32.org/1.0";
    protected static final String MEASUREMENTS_PREFIX_GML32 = "ms_gml32";
    protected static final String MEASUREMENTS_URI_GML32 = "http://www.measurements_gml32.org/1.0";

    // directory that should contain all the new files created during the setup of this data set
    private File testRootDirectory;

    /** Helper method that just quietly creates a temporary directory, */
    private static File createTestRootDirectory() {
        try {
            // create the tests root directory
            return IOUtils.createTempDirectory("app-schema-stations");
        } catch (Exception exception) {
            throw new RuntimeException("Error creating temporary directory.", exception);
        }
    }

    /** Helper method that builds a xpath engine that will use the provided GML namespaces. */
    public static XpathEngine buildXpathEngine(Map<String, String> baseNamespaces, String... namespaces) {
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
        finalNamespaces.putAll(baseNamespaces);
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

    /** Return a map containing the standard placeholders with the corresponding GML31 values. */
    public static Map<String, String> getGml31StandardParamaters() {
        Map<String, String> gml31Parameters = new HashMap<>();
        gml31Parameters.put("GML_PREFIX", "gml31");
        gml31Parameters.put("GML_PREFIX_UPPER", "GML31");
        gml31Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml");
        gml31Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");
        return gml31Parameters;
    }

    /** Return a map containing the standard placeholders with the corresponding GML32 values. */
    public static Map<String, String> getGml32StandardParamaters() {
        Map<String, String> gml32Parameters = new HashMap<>();
        gml32Parameters.put("GML_PREFIX", "gml32");
        gml32Parameters.put("GML_PREFIX_UPPER", "GML32");
        gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
        gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
        return gml32Parameters;
    }

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
        addMeasurementFeatureType(
                MEASUREMENTS_PREFIX_GML31, "gml31", "measurements", "base/measurements.xml", gml31Parameters);
        addStationFeatureType(STATIONS_PREFIX_GML31, "gml31", "stations", "base/stations.xml", gml31Parameters);
        // add GML 3.2 feature type
        Map<String, String> gml32Parameters = new HashMap<>();
        gml32Parameters.put("GML_PREFIX", "gml32");
        gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
        gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
        addMeasurementFeatureType(
                MEASUREMENTS_PREFIX_GML32, "gml32", "measurements", "base/measurements.xml", gml32Parameters);
        addStationFeatureType(STATIONS_PREFIX_GML32, "gml32", "stations", "base/stations.xml", gml32Parameters);
    }

    /**
     * Helper method that reads a resource to a string, performs the parameterization and writes the result to the
     * provided new file.
     */
    protected static void substituteParameters(String resourceName, Map<String, String> parameters, File newFile) {
        // read the resource content
        String resourceContent = resourceToString(resourceName);
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            // substitute the parameter on the resource content
            resourceContent = resourceContent.replace(String.format("${%s}", parameter.getKey()), parameter.getValue());
        }
        try {
            // write the final resource content to the provided location
            Files.write(newFile.toPath(), resourceContent.getBytes());
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error writing content to file '%s'.", newFile.getAbsolutePath()), exception);
        }
    }

    /** Helper method the reads a resource content to a string. */
    protected static String resourceToString(String resourceName) {
        try (InputStream input = NamespacesWfsTest.class.getResourceAsStream(resourceName)) {
            return IOUtils.toString(input);
        } catch (Exception exception) {
            throw new RuntimeException(String.format("Error reading resource '%s' content.", resourceName), exception);
        }
    }

    /** Helper method that will add the measurement feature type customizing it for the desired GML version. */
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
        File measurementsMappings = new File(gmlDirectory, String.format("%s_%s.xml", mappingsName, gmlPrefix));
        File measurementsProperties = new File(gmlDirectory, String.format("measurements_%s.properties", gmlPrefix));
        File measurementsSchema = new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
        // perform the parameterization
        substituteParameters("/test-data/stations/" + mappingsPath, parameters, measurementsMappings);
        substituteParameters("/test-data/stations/base/measurements.properties", parameters, measurementsProperties);
        substituteParameters("/test-data/stations/base/measurements.xsd", parameters, measurementsSchema);
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

    /** Helper method that will add the station feature type customizing it for the desired GML version. */
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
        File stationsMappings = new File(gmlDirectory, String.format("%s_%s.xml", mappingsName, gmlPrefix));
        File stationsProperties = new File(gmlDirectory, String.format("stations_%s.properties", gmlPrefix));
        File stationsSchema = new File(gmlDirectory, String.format("stations_%s.xsd", gmlPrefix));
        File measurementsSchema = new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
        // perform the parameterization
        substituteParameters("/test-data/stations/" + mappingsPath, parameters, stationsMappings);
        substituteParameters("/test-data/stations/base/stations.properties", parameters, stationsProperties);
        substituteParameters("/test-data/stations/base/stations.xsd", parameters, stationsSchema);
        substituteParameters("/test-data/stations/base/measurements.xsd", parameters, measurementsSchema);
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

    /** Helper method that will add the station feature type customizing it for the desired GML version. */
    protected void addStationFeatureType(
            String namespacePrefix,
            String gmlPrefix,
            String stationsMappingsName,
            String stationsMappingsPath,
            String measurementsMappingsName,
            String measurementsMappingsPath,
            Map<String, String> parameters) {
        // create root directory
        File gmlDirectory = getDirectoryForGmlPrefix(gmlPrefix);
        gmlDirectory.mkdirs();
        // add the necessary files
        File stationsMappings = new File(gmlDirectory, String.format("%s_%s.xml", stationsMappingsName, gmlPrefix));
        File measurementsMappings =
                new File(gmlDirectory, String.format("%s_%s.xml", measurementsMappingsName, gmlPrefix));
        File stationsProperties = new File(gmlDirectory, String.format("stations_%s.properties", gmlPrefix));
        File stationsSchema = new File(gmlDirectory, String.format("stations_%s.xsd", gmlPrefix));
        File measurementsProperties = new File(gmlDirectory, String.format("measurements_%s.properties", gmlPrefix));
        File measurementsSchema = new File(gmlDirectory, String.format("measurements_%s.xsd", gmlPrefix));
        // perform the parameterization
        substituteParameters("/test-data/stations/" + stationsMappingsPath, parameters, stationsMappings);
        substituteParameters("/test-data/stations/" + measurementsMappingsPath, parameters, measurementsMappings);
        substituteParameters("/test-data/stations/base/stations.properties", parameters, stationsProperties);
        substituteParameters("/test-data/stations/base/stations.xsd", parameters, stationsSchema);
        substituteParameters("/test-data/stations/base/measurements.xsd", parameters, measurementsSchema);
        // extra features to add:
        addStationFeatures(stationsProperties);
        // create station feature type
        addFeatureType(
                namespacePrefix,
                String.format("Station_%s", gmlPrefix),
                stationsMappings.getAbsolutePath(),
                stationsProperties.getAbsolutePath(),
                stationsSchema.getAbsolutePath(),
                measurementsSchema.getAbsolutePath(),
                measurementsMappings.getAbsolutePath(),
                measurementsProperties.getAbsolutePath());
    }

    /**
     * Helper method that will add the desired App-Schema defined feature type customizing it for the desired GML
     * version. If the provided GML prefix is NULL or empty it will be ignored.
     */
    protected void addAppSchemaFeatureType(
            String namespacePrefix,
            String gmlPrefix,
            String typeName,
            String mappingsFileResource,
            Map<String, String> parameters,
            String... otherResources) {
        // create root directory
        File gmlDirectory = getDirectoryForGmlPrefix(gmlPrefix);
        gmlDirectory.mkdirs();
        // create the mappings target file
        File targetMappingsFile = getTargetFile(mappingsFileResource, gmlPrefix, gmlDirectory);
        substituteParameters(mappingsFileResource, parameters, targetMappingsFile);
        // create target files for the other resources
        String[] otherResourcesFiles = new String[otherResources.length + 1];
        otherResourcesFiles[0] = targetMappingsFile.getAbsolutePath();
        for (int i = 0; i < otherResources.length; i++) {
            File targetFile = getTargetFile(otherResources[i], gmlPrefix, gmlDirectory);
            substituteParameters(otherResources[i], parameters, targetFile);
            otherResourcesFiles[i + 1] = targetFile.getAbsolutePath();
        }
        // create station feature type
        addFeatureType(namespacePrefix, typeName, targetMappingsFile.getAbsolutePath(), otherResourcesFiles);
    }

    /** Helper method that */
    private File getTargetFile(String resource, String gmlPrefix, File gmlDirectory) {
        int index = resource.lastIndexOf("/");
        if (index < 0) {
            throw new RuntimeException(String.format("Invalid resource '%s'.", resource));
        }
        String name = resource.substring(index + 1);
        index = name.lastIndexOf(".");
        if (index < 0) {
            throw new RuntimeException(String.format("Invalid resource name '%s' of resource '%s'.", name, resource));
        }
        String extension = name.substring(index);
        name = name.substring(0, index);
        if (gmlPrefix == null || gmlPrefix.isEmpty()) {
            // no gml prefix to add
            return new File(gmlDirectory, name + extension);
        }
        return new File(gmlDirectory, name + "_" + gmlPrefix + extension);
    }

    /**
     * Helper method that returns the directory, relative to tests root directory, that will contain the mappings,
     * schemas, properties, etc ... of the target GML version.
     */
    protected synchronized File getDirectoryForGmlPrefix(String gmlPrefix) {
        if (testRootDirectory == null) {
            // init the test directory
            testRootDirectory = createTestRootDirectory();
        }
        if (gmlPrefix == null || gmlPrefix.isEmpty()) {
            // no GML prefix, let's just use the root directory
            return testRootDirectory;
        }
        return new File(testRootDirectory, gmlPrefix);
    }

    private void addTextToFile(File file, String content) {

        try (Writer output = new BufferedWriter(new FileWriter(file, true))) {
            output.append(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void addStationFeatures(File file) {
        extraStationFeatures().ifPresent(content -> {
            addTextToFile(file, content);
        });
    }

    protected void addMeasurementFeatures(File file) {
        extraMeasurementFeatures().ifPresent(content -> {
            addTextToFile(file, content);
        });
    }

    /**
     * String of features to add to Stations feature type.
     *
     * @return Optional String of features
     */
    protected Optional<String> extraStationFeatures() {
        return Optional.empty();
    }

    /**
     * String of features to add to Measurements feature type.
     *
     * @return Optional String of features
     */
    protected Optional<String> extraMeasurementFeatures() {
        return Optional.empty();
    }

    @Override
    public void tearDown() {
        super.tearDown();
        try {
            // remove tests root directory
            IOUtils.delete(testRootDirectory);
        } catch (Exception exception) {
            // something bad happen, just log the exception and move on
            LOGGER.log(
                    Level.WARNING,
                    String.format("Error removing tests root directory '%s'.", testRootDirectory.getAbsolutePath()),
                    exception);
        }
    }
}

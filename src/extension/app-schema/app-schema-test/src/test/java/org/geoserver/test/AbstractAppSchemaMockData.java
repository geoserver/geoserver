/* 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geoserver.data.CatalogWriter;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geotools.data.complex.AppSchemaDataAccessTest;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Abstract base class for mock data based on the app-schema test data set.
 * 
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public abstract class AbstractAppSchemaMockData implements NamespaceTestData {

    /**
     * Folder for for test data.
     */
    private static final String TEST_DATA = "/test-data/";

    /**
     * Prefix for gsml namespace.
     */
    public static final String GSML_PREFIX = "gsml";

    /**
     * URI for gsml namespace.
     */
    public static final String GSML_URI = "urn:cgi:xmlns:CGI:GeoSciML:2.0";

    /**
     * Schema location URL for the the top-level gsml XSD.
     */
    public static final String GSML_SCHEMA_LOCATION_URL = "http://www.geosciml.org/geosciml/2.0/xsd/geosciml.xsd";

    /**
     * Map of namespace prefix to namespace URI.
     */
    @SuppressWarnings("serial")
    private static final Map<String, String> NAMESPACES = Collections
            .unmodifiableMap(new LinkedHashMap<String, String>() {
                {
                    put(GSML_PREFIX, GSML_URI);
                    put("gml", "http://www.opengis.net/gml");
                    put("xlink", "http://www.w3.org/1999/xlink");
                    put("sa", "http://www.opengis.net/sampling/1.0");
                    put("om", "http://www.opengis.net/om/1.0");
                    put("cv", "http://www.opengis.net/cv/0.2.1");
                    put("swe", "http://www.opengis.net/swe/1.0.1");
                    put("sml", "http://www.opengis.net/sensorML/1.0.1");
                }
            });

    /**
     * Use FeatureTypeInfo constants for srs handling as values
     */
    private static final String KEY_SRS_HANDLINGS = "srsHandling";

    /**
     * The feature type alias, a string
     */
    private static final String KEY_ALIAS = "alias";

    /**
     * The style name
     */
    private static final String KEY_STYLE = "style";

    /**
     * The srs code (a number) for this layer
     */
    private static final String KEY_SRS_NUMBER = "srs";

    /**
     * The lon/lat envelope as a JTS Envelope
     */
    private static final String KEY_LL_ENVELOPE = "ll_envelope";

    /**
     * The native envelope as a JTS Envelope
     */
    private static final String KEY_NATIVE_ENVELOPE = "native_envelope";

    private static final Envelope DEFAULT_ENVELOPE = new Envelope(-180, 180, -90, 90);

    /**
     * Map of data store name to data store connection parameters map.
     */
    private final Map<String, Map<String, Serializable>> datastoreParams = new LinkedHashMap<String, Map<String, Serializable>>();

    /**
     * Map of data store name to namespace prefix.
     */
    private final Map<String, String> datastoreNamespacePrefixes = new LinkedHashMap<String, String>();

    private final Map<String, String> namespaces;
    
    private final Map<String, String> layerStyles = new LinkedHashMap<String,String>();

    private File data;
    
    private File styles;

    /** the 'featureTypes' directory, under 'data' */
    private File featureTypesBaseDir;

    /**
     * Constructor with the default namespaces, schema directory, and catalog file.
     */
    public AbstractAppSchemaMockData() {
        this(NAMESPACES);
    }

    public AbstractAppSchemaMockData(Map<String, String> namespaces) {
        this.namespaces = new LinkedHashMap<String, String>(namespaces);
        // create the mock data directory
        try {
            data = IOUtils.createRandomDirectory("target", "app-schema-mock", "data");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        data.delete();
        data.mkdir();
        // create a featureTypes directory
        featureTypesBaseDir = new File(data, "featureTypes");
        featureTypesBaseDir.mkdir();
        
        // create the styles directory
        styles = new File(data, "styles");
        styles.mkdir();
        
        addContent();
        setUpCatalog();
    }

    /**
     * Return the namespace prefixx to namespace URI map for this data.
     * 
     * @see org.geoserver.test.NamespaceTestData#getNamespaces()
     */
    public Map<String, String> getNamespaces() {
        return Collections.unmodifiableMap(namespaces);
    }

    /**
     * Subclasses must override this method to add namespaces with
     * {@link #putNamespace(String, String)} and feature types with
     * {@link #addFeatureType(String, String, String, String...)}.
     */
    protected abstract void addContent();

    /**
     * Copy a file from the test-data directory to a feature type directory. if fileName contains
     * directory path eg, dir1/dir2/file.xml, the full path will be used to locate the resource.
     * After which the directory will be ignored.
     * 
     * @param namespacePrefix
     *            namespace prefix of the WFS feature type
     * @param typeName
     *            local name of the WFS feature type
     * @param fileName
     *            short name of the file in test-data to copy
     * @param data
     *            mock data root directory
     */
    private void copyFileToFeatureTypeDir(String namespacePrefix, String typeName, String fileName) {
        copy(AppSchemaDataAccessTest.class.getResourceAsStream(TEST_DATA + fileName),
                "featureTypes" + "/" + getDataStoreName(namespacePrefix, typeName) + "/"
                        + fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length()));
    }

    /**
     * Returns the root of the mock data directory,
     * 
     * @see org.geoserver.data.test.TestData#getDataDirectoryRoot()
     */
    public File getDataDirectoryRoot() {
        return data;
    }

    /**
     * Returns true.
     * 
     * @see org.geoserver.data.test.TestData#isTestDataAvailable()
     */
    public boolean isTestDataAvailable() {
        return true;
    }

    /**
     * Configures mock data directory.
     * 
     * @see org.geoserver.data.test.TestData#setUp()
     */
    public void setUp() {
        setUpCatalog();
        copy(MockData.class.getResourceAsStream("services.xml"), "services.xml");
    }

    /**
     * Removes the mock data directory.
     * 
     * @see org.geoserver.data.test.TestData#tearDown()
     */
    public void tearDown() {
        try {
            IOUtils.delete(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        data = null;
    }

    /**
     * Writes catalog.xml to the data directory.
     */
    private void setUpCatalog() {
        CatalogWriter writer = new CatalogWriter();
        writer.dataStores(datastoreParams, datastoreNamespacePrefixes, Collections
                .<String> emptySet());
        writer.coverageStores(new HashMap<String, Map<String, String>>(),
                new HashMap<String, String>(), Collections.<String> emptySet());
        writer.namespaces(namespaces);
        writer.styles(layerStyles);
        try {
            writer.write(new File(data, "catalog.xml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies from an {@link InputStream} to path under the mock data directory.
     * 
     * @param input
     *            source from which file content is copied
     * @param location
     *            path relative to mock data directory
     */
    private void copy(InputStream input, String location) {
        try {
            IOUtils.copy(input, new File(data, location));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write an info.xml file describing a feature type to the feature type directory.
     * 
     * <p>
     * 
     * Stolen from {@link MockData}.
     * 
     * @param namespacePrefix
     *            namespace prefix of the WFS feature type
     * @param typeName
     *            namespace prefix of the WFS feature type
     * @param featureTypeDir
     *            feature type directory
     * @param dataStoreName
     *            data store directory name
     */
    private static void writeInfoFile(String namespacePrefix, String typeName, File featureTypeDir,
            String dataStoreName) {
        // prepare extra params default
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(KEY_STYLE, "Default");
        params.put(KEY_SRS_HANDLINGS, 2);
        params.put(KEY_ALIAS, null);
        Integer srs = 4326;
        params.put(KEY_SRS_NUMBER, srs);
        try {
            featureTypeDir.mkdir();
            File info = new File(featureTypeDir, "info.xml");
            info.delete();
            info.createNewFile();
            FileWriter writer = new FileWriter(info);
            writer.write("<featureType datastore=\"" + dataStoreName + "\">");
            writer.write("<name>" + typeName + "</name>");
            if (params.get(KEY_ALIAS) != null)
                writer.write("<alias>" + params.get(KEY_ALIAS) + "</alias>");
            writer.write("<SRS>" + params.get(KEY_SRS_NUMBER) + "</SRS>");
            // this mock type may have wrong SRS compared to the actual one in the property files...
            // let's configure SRS handling not to alter the original one, and have 4326 used only
            // for capabilities
            writer.write("<SRSHandling>" + params.get(KEY_SRS_HANDLINGS) + "</SRSHandling>");
            writer.write("<title>" + typeName + "</title>");
            writer.write("<abstract>abstract about " + typeName + "</abstract>");
            writer.write("<numDecimals value=\"8\"/>");
            writer.write("<keywords>" + typeName + "</keywords>");
            Envelope llEnvelope = (Envelope) params.get(KEY_LL_ENVELOPE);
            if (llEnvelope == null)
                llEnvelope = DEFAULT_ENVELOPE;
            writer.write("<latLonBoundingBox dynamic=\"false\" minx=\"" + llEnvelope.getMinX()
                    + "\" miny=\"" + llEnvelope.getMinY() + "\" maxx=\"" + llEnvelope.getMaxX()
                    + "\" maxy=\"" + llEnvelope.getMaxY() + "\"/>");
            Envelope nativeEnvelope = (Envelope) params.get(KEY_NATIVE_ENVELOPE);
            if (nativeEnvelope != null)
                writer.write("<nativeBBox dynamic=\"false\" minx=\"" + nativeEnvelope.getMinX()
                        + "\" miny=\"" + nativeEnvelope.getMinY() + "\" maxx=\""
                        + nativeEnvelope.getMaxX() + "\" maxy=\"" + nativeEnvelope.getMaxY()
                        + "\"/>");
            String style = (String) params.get(KEY_STYLE);
            if (style == null)
                style = "Default";
            writer.write("<styles default=\"" + style + "\"/>");
            writer.write("</featureType>");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build the connection parameters map for a data store.
     * 
     * @param namespacePrefix
     *            namespace prefix of the WFS feature type
     * @param typeName
     *            local name of the WFS feature type
     * @param mappingFileName
     *            file name of the app-schema mapping file
     * @param featureTypesBaseDir
     *            feature types base directory
     * @param dataStoreName
     *            data store name
     * @return
     */
    @SuppressWarnings("serial")
    private static Map<String, Serializable> buildAppSchemaDatastoreParams(
            final String namespacePrefix, final String typeName, final String mappingFileName,
            final File featureTypesBaseDir, final String dataStoreName) {
        try {
            return new LinkedHashMap<String, Serializable>() {
                {
                    put("dbtype", "app-schema");
                    put("url", new File(new File(featureTypesBaseDir, dataStoreName),
                            mappingFileName).toURI().toURL());
                }
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add one feature type, copying its resources and registering, creating its info.xml, and
     * adding it to catalog.xml.
     * 
     * @param namespacePrefix
     *            namespace prefix of the WFS feature type
     * @param typeName
     *            local name of the WFS feature type
     * @param mappingFileName
     *            file name of the app-schema mapping file
     * @param supportFileNames
     *            names of other files to be copied into the feature type directory
     */
    public void addFeatureType(String namespacePrefix, String typeName, String mappingFileName,
            String... supportFileNames) {
        File featureTypeDir = getFeatureTypeDir(featureTypesBaseDir, namespacePrefix, typeName);
        String dataStoreName = getDataStoreName(namespacePrefix, typeName);
        try {
            writeInfoFile(namespacePrefix, typeName, featureTypeDir, dataStoreName);
            copyMappingAndSupportFiles(namespacePrefix, typeName, mappingFileName, supportFileNames);
            // if mappingFileName contains directory, eg, dir1/dir2/file.xml, we will ignore the
            // directory from here on
            addDataStore(dataStoreName, namespacePrefix, buildAppSchemaDatastoreParams(
                    namespacePrefix, typeName, mappingFileName.substring(mappingFileName
                            .lastIndexOf("/") + 1, mappingFileName.length()), featureTypesBaseDir,
                    dataStoreName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
       
    /**
     * Adds the specified style to the data directory
     * @param styleId the style id
     * @param filename filename of SLD file in test-data to be copied into the data directory
     * @throws IOException
     */
    public void addStyle(String styleId, String fileName) {
        layerStyles.put(styleId, styleId + ".sld");
        InputStream styleContents = getClass().getResourceAsStream(TEST_DATA + fileName);
        File to = new File(styles, styleId + ".sld");
        try {
            IOUtils.copy(styleContents, to);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }

    /**
     * Add a datastore and record its prefix in the lookup table.
     * 
     * @param dataStoreName
     * @param namespacePrefix
     * @param params
     */
    private void addDataStore(String dataStoreName, String namespacePrefix,
            Map<String, Serializable> params) {
        datastoreParams.put(dataStoreName, params);
        datastoreNamespacePrefixes.put(dataStoreName, namespacePrefix);
    }

    /**
     * Put a namespace into the map.
     * 
     * @param namspacePrefix
     *            namespace prefix
     * @param namespaceUri
     *            namespace URI
     */
    protected void putNamespace(String namspacePrefix, String namespaceUri) {
        namespaces.put(namspacePrefix, namespaceUri);
    }

    /**
     * Remove a namespace in a map.
     * 
     * @param namspacePrefix
     *            namespace prefix
     */
    protected void removeNamespace(String namspacePrefix) {
        namespaces.remove(namspacePrefix);
    }
    
    /**
     * Get the name of the data store for a feature type. This is used to construct the name of the
     * feature type directory as well as the name of the data store.
     * 
     * @param namespacePrefix
     *            namespace prefix of the WFS feature type
     * @param typeName
     *            local name of the WFS feature type
     * @return name of the data store for the feature type
     */
    protected static String getDataStoreName(String namespacePrefix, String typeName) {
        return namespacePrefix + "_" + typeName;
    }

    /**
     * Return the featureTypes directory under which individual feature type folders are stored.
     * 
     * @return the featureTypes directory
     */
    protected File getFeatureTypesBaseDir() {
        return featureTypesBaseDir;
    }

    /**
     * Get the file for the directory that contains the mapping and property files.
     * 
     * @param namespacePrefix
     *            namespace prefix of the WFS feature type
     * @param typeName
     *            local name of the WFS feature type
     * @return directory that contains the mapping and property files
     */
    private static File getFeatureTypeDir(File featureTypesBaseDir, String namespacePrefix,
            String typeName) {
        return new File(featureTypesBaseDir, getDataStoreName(namespacePrefix, typeName));
    }

    /**
     * Copy the mapping and property files to the feature type directory.
     * 
     * @param namespacePrefix
     *            namespace prefix of the WFS feature type
     * @param typeName
     *            local name of the WFS feature type
     * @param mappingFileName
     *            name of the mapping file for this feature type
     * @param supportFileNames
     *            names of the support files, such as properties files, for this feature type
     */
    private void copyMappingAndSupportFiles(String namespacePrefix, String typeName,
            String mappingFileName, String... supportFileNames) {
        copyFileToFeatureTypeDir(namespacePrefix, typeName, mappingFileName);
        for (String propertyFileName : supportFileNames) {
            copyFileToFeatureTypeDir(namespacePrefix, typeName, propertyFileName);
        }
    }

}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.geoserver.data.CatalogWriter;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.onlineTest.setup.AppSchemaTestOracleSetup;
import org.geoserver.test.onlineTest.setup.AppSchemaTestPostgisSetup;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;
import org.geoserver.util.IOUtils;
import org.geotools.data.complex.AppSchemaDataAccessTest;
import org.geotools.xml.resolver.SchemaCatalog;
import org.locationtech.jts.geom.Envelope;

/**
 * Abstract base class for mock data based on the app-schema test data set.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public abstract class AbstractAppSchemaMockData extends SystemTestData
        implements NamespaceTestData {

    /** Folder for for test data. */
    private static final String TEST_DATA = "/test-data/";

    /** Prefix for gsml namespace. */
    public static final String GSML_PREFIX = "gsml";

    /** URI for gsml namespace. */
    public static final String GSML_URI = "urn:cgi:xmlns:CGI:GeoSciML:2.0";

    /** Schema location URL for the the top-level gsml XSD. */
    public static final String GSML_SCHEMA_LOCATION_URL =
            "http://www.geosciml.org/geosciml/2.0/xsd/geosciml.xsd";

    /** PRefix for spec namespace. */
    public static final String SPEC_PREFIX = "spec";

    /** Map of namespace prefix to namespace URI for GML 32 schema. */
    @SuppressWarnings("serial")
    protected static final Map<String, String> GML32_NAMESPACES =
            Collections.unmodifiableMap(
                    new TreeMap<String, String>() {
                        {
                            put("cgu", "urn:cgi:xmlns:CGI:Utilities:3.0.0");
                            put("gco", "http://www.isotc211.org/2005/gco");
                            put("gmd", "http://www.isotc211.org/2005/gmd");
                            put("gml", "http://www.opengis.net/gml/3.2");
                            put("gsml", "urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0");
                            put("sa", "http://www.opengis.net/sampling/2.0");
                            put("spec", "http://www.opengis.net/samplingSpecimen/2.0");
                            put("swe", "http://www.opengis.net/swe/1.0/gml32");
                            put("wfs", "http://www.opengis.net/wfs/2.0");
                            put("xlink", "http://www.w3.org/1999/xlink");
                        }
                    });

    /** Map of namespace prefix to namespace URI. */
    @SuppressWarnings("serial")
    private static final Map<String, String> NAMESPACES =
            Collections.unmodifiableMap(
                    new LinkedHashMap<String, String>() {
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

    /** Use FeatureTypeInfo constants for srs handling as values */
    static final String KEY_SRS_HANDLINGS = "srsHandling";

    /** The feature type alias, a string */
    static final String KEY_ALIAS = "alias";

    /** The style name */
    static final String KEY_STYLE = "style";

    /** The srs code (a number) for this layer */
    static final String KEY_SRS_NUMBER = "srs";

    /** The lon/lat envelope as a JTS Envelope */
    static final String KEY_LL_ENVELOPE = "ll_envelope";

    /** The native envelope as a JTS Envelope */
    static final String KEY_NATIVE_ENVELOPE = "native_envelope";

    static final Envelope DEFAULT_ENVELOPE = new Envelope(-180, 180, -90, 90);

    /** Map of data store name to data store connection parameters map. */
    private final Map<String, Map<String, Serializable>> datastoreParams =
            new LinkedHashMap<String, Map<String, Serializable>>();

    /** Map of data store name to namespace prefix. */
    private final Map<String, String> datastoreNamespacePrefixes =
            new LinkedHashMap<String, String>();

    private final Map<String, String> namespaces;

    private final List<String> isolatedNamespaces = new ArrayList<>();

    private final Map<String, String> layerStyles = new LinkedHashMap<String, String>();

    private File styles;

    /** the 'featureTypes' directory, under 'data' */
    protected File featureTypesBaseDir;

    /**
     * Pair of property file name and feature type directory to create db tables for online tests
     */
    private Map<String, File> propertiesFiles;

    /** Indicates fixture id (postgis or oracle) if running in online mode */
    private String onlineTestId;

    /** SchemaCatalog to work with AppSchemaValidator for test requests validation. */
    private SchemaCatalog catalog;

    /**
     * True if running 3D online test. Only matters for Oracle, since a special wkt parser is
     * needed.
     */
    private boolean is3D = false;
    /** Constructor with the default namespaces, schema directory, and catalog file. */
    public AbstractAppSchemaMockData() {
        this(NAMESPACES);
    }

    static File newRandomDirectory() {
        try {
            return IOUtils.createRandomDirectory("target", "app-schema-mock", "data");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractAppSchemaMockData(Map<String, String> namespaces) {
        super(newRandomDirectory());
        this.namespaces = new LinkedHashMap<String, String>(namespaces);

        // create a featureTypes directory
        featureTypesBaseDir = new File(data, "featureTypes");
        featureTypesBaseDir.mkdir();

        // create the styles directory
        styles = new File(data, "styles");
        styles.mkdir();

        propertiesFiles = new HashMap<String, File>();

        addContent();
        // create corresponding tables in the test db using the properties files
        if (!propertiesFiles.isEmpty()) {
            try {
                createTablesInTestDatabase();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        setUpCatalog();
    }

    public boolean isOracleOnlineTest() {
        return "oracle".equals(onlineTestId);
    }

    public boolean isPostgisOnlineTest() {
        return "postgis".equals(onlineTestId);
    }

    /** @param catalogLocation file location relative to test-data dir. */
    protected void setSchemaCatalog(String catalogLocation) {
        if (catalogLocation != null) {
            URL resolvedCatalogLocation = getClass().getResource(TEST_DATA + catalogLocation);
            if (resolvedCatalogLocation == null) {
                throw new RuntimeException(
                        "Test catalog location must be relative to test-data directory!");
            }
            this.catalog = SchemaCatalog.build(resolvedCatalogLocation);
        }
    }

    public SchemaCatalog getSchemaCatalog() {
        return catalog;
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
     * Subclasses must override this method to add namespaces with {@link #putNamespace(String,
     * String)} and feature types with {@link #addFeatureType(String, String, String, String...)}.
     */
    protected abstract void addContent();

    /**
     * Helper method the will first try to resolve the resource as an existing file and open it
     * otherwise the resource will be open as an app-schema test data resource.
     */
    private InputStream openResource(String resource) {
        File resourceFile = new File(resource);
        if (resourceFile.exists()) {
            try {
                // the resource is a file so read from it
                return new FileInputStream(resourceFile);
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format("Error reading file '%s'.", resourceFile.getAbsolutePath()),
                        exception);
            }
        } else {
            // considering the resource to be an app-schema test data resource
            return AppSchemaDataAccessTest.class.getResourceAsStream(TEST_DATA + resource);
        }
    }

    /**
     * Copy a file from the test-data directory to a feature type directory. if fileName contains
     * directory path eg, dir1/dir2/file.xml, the full path will be used to locate the resource.
     * After which the directory will be ignored.
     *
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName local name of the WFS feature type
     * @param fileName short name of the file in test-data to copy
     * @param data mock data root directory
     */
    private void copyFileToFeatureTypeDir(String namespacePrefix, String typeName, String fileName)
            throws IOException {
        try (InputStream input = openResource(fileName)) {
            copy(
                    input,
                    "featureTypes"
                            + "/"
                            + getDataStoreName(namespacePrefix, typeName)
                            + "/"
                            + getFileNamePart(fileName));
        }
    }

    protected String getFileNamePart(String fileName) {
        if (fileName.indexOf(File.separator) > 0) {
            return fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.length());
        } else {
            return fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
        }
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
    public void setUp() throws IOException {
        setUpCatalog();
        setUpSecurity();
        copy(MockData.class.getResourceAsStream("services.xml"), "services.xml");
    }

    @Override
    public void setUpDefault() throws Exception {
        // do nothing
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

    /** Writes catalog.xml to the data directory. */
    private void setUpCatalog() {
        CatalogWriter writer = new CatalogWriter();
        writer.dataStores(
                datastoreParams, datastoreNamespacePrefixes, Collections.<String>emptySet());
        writer.coverageStores(
                new HashMap<String, Map<String, String>>(),
                new HashMap<String, String>(),
                Collections.<String>emptySet());
        writer.namespaces(namespaces, isolatedNamespaces);
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
     * @param input source from which file content is copied
     * @param location path relative to mock data directory
     */
    private void copy(InputStream input, String location) {
        try {
            IOUtils.copy(input, new File(data, location));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies a String content to a file in the path under the mock data directory.
     *
     * @param content file content
     * @param location path relative to mock data directory
     */
    private void copy(String content, String location) {
        File file = new File(data, location);
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Write an info.xml file describing a feature type to the feature type directory.
     *
     * <p>Stolen from {@link MockData}.
     *
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName namespace prefix of the WFS feature type
     * @param featureTypeDir feature type directory
     * @param dataStoreName data store directory name
     */
    private static void writeInfoFile(
            String namespacePrefix, String typeName, File featureTypeDir, String dataStoreName) {
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
            writer.write("<nativeName>" + typeName + "</nativeName>");
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
            if (llEnvelope == null) llEnvelope = DEFAULT_ENVELOPE;
            writer.write(
                    "<latLonBoundingBox dynamic=\"false\" minx=\""
                            + llEnvelope.getMinX()
                            + "\" miny=\""
                            + llEnvelope.getMinY()
                            + "\" maxx=\""
                            + llEnvelope.getMaxX()
                            + "\" maxy=\""
                            + llEnvelope.getMaxY()
                            + "\"/>");
            Envelope nativeEnvelope = (Envelope) params.get(KEY_NATIVE_ENVELOPE);
            if (nativeEnvelope != null)
                writer.write(
                        "<nativeBBox dynamic=\"false\" minx=\""
                                + nativeEnvelope.getMinX()
                                + "\" miny=\""
                                + nativeEnvelope.getMinY()
                                + "\" maxx=\""
                                + nativeEnvelope.getMaxX()
                                + "\" maxy=\""
                                + nativeEnvelope.getMaxY()
                                + "\"/>");
            String style = (String) params.get(KEY_STYLE);
            if (style == null) style = "Default";
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
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName local name of the WFS feature type
     * @param mappingFileName file name of the app-schema mapping file
     * @param featureTypesBaseDir feature types base directory
     * @param dataStoreName data store name
     */
    @SuppressWarnings("serial")
    protected static Map<String, Serializable> buildAppSchemaDatastoreParams(
            final String namespacePrefix,
            final String typeName,
            final String mappingFileName,
            final File featureTypesBaseDir,
            final String dataStoreName) {
        try {
            return new LinkedHashMap<String, Serializable>() {
                {
                    put("dbtype", "app-schema");
                    put(
                            "url",
                            new File(new File(featureTypesBaseDir, dataStoreName), mappingFileName)
                                    .toURI()
                                    .toURL());
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
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName local name of the WFS feature type
     * @param mappingFileName file name of the app-schema mapping file
     * @param supportFileNames names of other files to be copied into the feature type directory
     */
    public void addFeatureType(
            String namespacePrefix,
            String typeName,
            String mappingFileName,
            String... supportFileNames) {
        File featureTypeDir = getFeatureTypeDir(featureTypesBaseDir, namespacePrefix, typeName);
        String dataStoreName = getDataStoreName(namespacePrefix, typeName);
        try {
            writeInfoFile(namespacePrefix, typeName, featureTypeDir, dataStoreName);
            copyMappingAndSupportFiles(
                    namespacePrefix, typeName, mappingFileName, supportFileNames);
            // if mappingFileName contains directory, eg, dir1/dir2/file.xml, we will ignore the
            // directory from here on
            addDataStore(
                    dataStoreName,
                    namespacePrefix,
                    buildAppSchemaDatastoreParams(
                            namespacePrefix,
                            typeName,
                            getFileNamePart(mappingFileName),
                            featureTypesBaseDir,
                            dataStoreName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The same as {@link #addFeatureType(String, String, String, String...)} except this to enable
     * 3D WKT parser for Oracle. Use this one for tests with 3D data that needs to be run online.
     *
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName local name of the WFS feature type
     * @param mappingFileName file name of the app-schema mapping file
     * @param supportFileNames names of other files to be copied into the feature type directory
     */
    public void add3DFeatureType(
            String namespacePrefix,
            String typeName,
            String mappingFileName,
            String... supportFileNames) {
        addFeatureType(namespacePrefix, typeName, mappingFileName, supportFileNames);
        this.is3D = true;
    }

    /** Determine which setup class to use based on the fixture id specified in the vm arg. */
    private void createTablesInTestDatabase() throws Exception {
        AbstractReferenceDataSetup setup = null;
        if (isOracleOnlineTest()) {
            if (is3D) {
                setup = AppSchemaTestOracleSetup.get3DInstance(propertiesFiles);
            } else {
                setup = AppSchemaTestOracleSetup.getInstance(propertiesFiles);
            }
            // Run the sql script through setup
            setup.setUp();
            setup.tearDown();
        } else if (isPostgisOnlineTest()) {
            setup = AppSchemaTestPostgisSetup.getInstance(propertiesFiles);
            // Run the sql script through setup
            setup.setUp();
            setup.tearDown();
        }
    }

    /**
     * Adds the specified style to the data directory
     *
     * @param styleId the style id
     * @param fileName filename of SLD file in test-data to be copied into the data directory
     */
    public void addStyle(String styleId, String fileName) {
        layerStyles.put(styleId, styleId + ".sld");
        InputStream styleContents = getClass().getResourceAsStream(TEST_DATA + fileName);
        File to = new File(styles, styleId + ".sld");
        try {
            IOUtils.copy(styleContents, to);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Add a datastore and record its prefix in the lookup table. */
    protected void addDataStore(
            String dataStoreName, String namespacePrefix, Map<String, Serializable> params) {
        datastoreParams.put(dataStoreName, params);
        datastoreNamespacePrefixes.put(dataStoreName, namespacePrefix);
    }

    /**
     * Put a namespace into the map.
     *
     * @param namspacePrefix namespace prefix
     * @param namespaceUri namespace URI
     */
    protected void putNamespace(String namspacePrefix, String namespaceUri) {
        namespaces.put(namspacePrefix, namespaceUri);
    }

    /**
     * Put an isolated namespace into the namespaces map.
     *
     * @param namespacePrefix namespace prefix
     * @param namespaceUri namespace URI
     */
    protected void putIsolatedNamespace(String namespacePrefix, String namespaceUri) {
        namespaces.put(namespacePrefix, namespaceUri);
        isolatedNamespaces.add(namespacePrefix);
    }

    /**
     * Remove a namespace in a map.
     *
     * @param namespacePrefix namespace prefix
     */
    protected void removeNamespace(String namespacePrefix) {
        namespaces.remove(namespacePrefix);
        isolatedNamespaces.remove(namespacePrefix);
    }

    /**
     * Get the name of the data store for a feature type. This is used to construct the name of the
     * feature type directory as well as the name of the data store.
     *
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName local name of the WFS feature type
     * @return name of the data store for the feature type
     */
    protected String getDataStoreName(String namespacePrefix, String typeName) {
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
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName local name of the WFS feature type
     * @return directory that contains the mapping and property files
     */
    protected File getFeatureTypeDir(
            File featureTypesBaseDir, String namespacePrefix, String typeName) {
        return new File(featureTypesBaseDir, getDataStoreName(namespacePrefix, typeName));
    }

    /**
     * Copy the mapping and property files to the feature type directory.
     *
     * @param namespacePrefix namespace prefix of the WFS feature type
     * @param typeName local name of the WFS feature type
     * @param mappingFileName name of the mapping file for this feature type
     * @param supportFileNames names of the support files, such as properties files, for this
     *     feature type
     */
    protected void copyMappingAndSupportFiles(
            String namespacePrefix,
            String typeName,
            String mappingFileName,
            String... supportFileNames)
            throws IOException {
        onlineTestId = System.getProperty("testDatabase");
        if (onlineTestId != null) {
            onlineTestId = onlineTestId.toLowerCase().trim();
            // special handling for running app-schema-test with online mode
            try {
                // new content with modified dataStore "parameters" tag
                String newContent = modifyOnlineMappingFileContent(mappingFileName);
                copy(
                        newContent,
                        "featureTypes/"
                                + getDataStoreName(namespacePrefix, typeName)
                                + "/"
                                + getFileNamePart(mappingFileName));

                for (String propertyFileName : supportFileNames) {
                    if (propertyFileName.endsWith(".xml")) {
                        // also update the datastore "parameters" for supporting mapping files
                        newContent = modifyOnlineMappingFileContent(propertyFileName);
                        copy(
                                newContent,
                                "featureTypes/"
                                        + getDataStoreName(namespacePrefix, typeName)
                                        + "/"
                                        + getFileNamePart(propertyFileName));
                    } else {
                        copyFileToFeatureTypeDir(namespacePrefix, typeName, propertyFileName);
                        if (propertyFileName.endsWith(".properties")) {
                            // extract the file name if needed
                            File file = new File(propertyFileName);
                            if (file.exists()) {
                                // extract the file name
                                propertyFileName = file.getName();
                            }
                            propertiesFiles.put(
                                    propertyFileName,
                                    getFeatureTypeDir(
                                            featureTypesBaseDir, namespacePrefix, typeName));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            copyFileToFeatureTypeDir(namespacePrefix, typeName, mappingFileName);
            for (String propertyFileName : supportFileNames) {
                copyFileToFeatureTypeDir(namespacePrefix, typeName, propertyFileName);
            }
        }
    }

    /**
     * Modify the mapping file stream that is to be copied to the target directory. This is so the
     * mapping file copy has the right datastore parameters to use the test database.
     *
     * @param mappingFileName Mapping file to be copied
     * @return Modified content string
     */
    private String modifyOnlineMappingFileContent(String mappingFileName) throws IOException {
        InputStream is = openResource(mappingFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer content = new StringBuffer();
        boolean parametersStartFound = false;
        boolean parametersEndFound = false;
        boolean isOracle = onlineTestId.equals("oracle");
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (!parametersStartFound || (parametersStartFound && parametersEndFound)) {
                // before <parameters> or after </parameters>
                if (!parametersStartFound) {
                    // look for start tag
                    if (line.trim().equals("<parameters>")) {
                        parametersStartFound = true;
                        // copy <parameters> with new db params
                        if (isOracle) {
                            content.append(AppSchemaTestOracleSetup.DB_PARAMS);
                        } else {
                            content.append(AppSchemaTestPostgisSetup.DB_PARAMS);
                        }
                    } else {
                        // copy content
                        content.append(line);
                    }
                } else if (line.trim().startsWith("<sourceType>")) {
                    // make everything upper case due to OracleDialect not wrapping them in quotes
                    line = line.trim();
                    String sourceTypeTag = "<sourceType>";
                    content.append(sourceTypeTag);
                    String tableName =
                            line.substring(
                                    line.indexOf(sourceTypeTag) + sourceTypeTag.length(),
                                    line.indexOf("</sourceType>"));
                    content.append(tableName.toUpperCase());
                    content.append("</sourceType>");
                    content.append("\n");
                } else {
                    content.append(line);
                }
                content.append("\n");
            } else {
                // else skip <parameters> content and do nothing
                // look for end tag
                if (line.trim().equals("</parameters>")) {
                    parametersEndFound = true;
                }
            }
        }
        return content.toString();
    }
}

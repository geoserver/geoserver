/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.CatalogWriter;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.util.IOUtils;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;

/**
 * Class used to build a mock GeoServer data directory.
 *
 * <p>Data is based off the wms and wfs "cite" datasets.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MockData implements TestData {
    // Extra configuration keys for vector data
    /**
     * Use FeatureTypeInfo constants for srs handling as values or use {@link ProjectionPolicy}
     * values straight
     */
    public static final String KEY_SRS_HANDLINGS = "srsHandling";
    /** The feature type alias, a string */
    public static final String KEY_ALIAS = "alias";
    /** The style name */
    public static final String KEY_STYLE = "style";
    /** The srs code (a number) for this layer */
    public static final String KEY_SRS_NUMBER = "srs";
    /** The lon/lat envelope as a JTS Envelope */
    public static final String KEY_LL_ENVELOPE = "ll_envelope";
    /** The native envelope as a JTS Envelope */
    public static final String KEY_NATIVE_ENVELOPE = "native_envelope";

    static final Envelope DEFAULT_ENVELOPE = new Envelope(-180, 180, -90, 90);

    // //// WMS 1.1.1
    /** WMS 1.1.1 cite namespace + uri */
    public static String CITE_PREFIX = "cite";

    public static String CITE_URI = "http://www.opengis.net/cite";

    /** featuretype name for WMS 1.1.1 CITE BasicPolygons features */
    public static QName BASIC_POLYGONS = new QName(CITE_URI, "BasicPolygons", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Bridges features */
    public static QName BRIDGES = new QName(CITE_URI, "Bridges", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Buildings features */
    public static QName BUILDINGS = new QName(CITE_URI, "Buildings", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Divided Routes features */
    public static QName DIVIDED_ROUTES = new QName(CITE_URI, "DividedRoutes", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Forests features */
    public static QName FORESTS = new QName(CITE_URI, "Forests", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Lakes features */
    public static QName LAKES = new QName(CITE_URI, "Lakes", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Map Neatliine features */
    public static QName MAP_NEATLINE = new QName(CITE_URI, "MapNeatline", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Named Places features */
    public static QName NAMED_PLACES = new QName(CITE_URI, "NamedPlaces", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Ponds features */
    public static QName PONDS = new QName(CITE_URI, "Ponds", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Road Segments features */
    public static QName ROAD_SEGMENTS = new QName(CITE_URI, "RoadSegments", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Streams features */
    public static QName STREAMS = new QName(CITE_URI, "Streams", CITE_PREFIX);

    // /// WFS 1.0
    /** WFS 1.0 cdf namespace + uri */
    public static String CDF_PREFIX = "cdf";

    public static String CDF_URI = "http://www.opengis.net/cite/data";

    /** featuretype name for WFS 1.0 CITE Deletes features */
    public static QName DELETES = new QName(CDF_URI, "Deletes", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Fifteen features */
    public static QName FIFTEEN = new QName(CDF_URI, "Fifteen", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Inserts features */
    public static QName INSERTS = new QName(CDF_URI, "Inserts", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Inserts features */
    public static QName LOCKS = new QName(CDF_URI, "Locks", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Nulls features */
    public static QName NULLS = new QName(CDF_URI, "Nulls", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Other features */
    public static QName OTHER = new QName(CDF_URI, "Other", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Nulls features */
    public static QName SEVEN = new QName(CDF_URI, "Seven", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Updates features */
    public static QName UPDATES = new QName(CDF_URI, "Updates", CDF_PREFIX);

    /** cgf namespace + uri */
    public static String CGF_PREFIX = "cgf";

    public static String CGF_URI = "http://www.opengis.net/cite/geometry";

    /** featuretype name for WFS 1.0 CITE Lines features */
    public static QName LINES = new QName(CGF_URI, "Lines", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE MLines features */
    public static QName MLINES = new QName(CGF_URI, "MLines", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE MPoints features */
    public static QName MPOINTS = new QName(CGF_URI, "MPoints", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE MPolygons features */
    public static QName MPOLYGONS = new QName(CGF_URI, "MPolygons", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Points features */
    public static QName POINTS = new QName(CGF_URI, "Points", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Polygons features */
    public static QName POLYGONS = new QName(CGF_URI, "Polygons", CGF_PREFIX);

    // //// WFS 1.1
    /** sf namespace + uri */
    public static String SF_PREFIX = "sf";

    public static String SF_URI = "http://cite.opengeospatial.org/gmlsf";
    public static QName PRIMITIVEGEOFEATURE = new QName(SF_URI, "PrimitiveGeoFeature", SF_PREFIX);
    public static QName AGGREGATEGEOFEATURE = new QName(SF_URI, "AggregateGeoFeature", SF_PREFIX);
    public static QName GENERICENTITY = new QName(SF_URI, "GenericEntity", SF_PREFIX);

    // WCS 1.0
    public static QName GTOPO_DEM = new QName(CDF_URI, "W020N90", CDF_PREFIX);
    public static QName USA_WORLDIMG = new QName(CDF_URI, "usa", CDF_PREFIX);
    public static String DEM = "dem";
    public static String PNG = "png";
    // WCS 1.1
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_DEM = new QName(WCS_URI, "DEM", WCS_PREFIX);
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName ROTATED_CAD = new QName(WCS_URI, "RotatedCad", WCS_PREFIX);
    public static QName WORLD = new QName(WCS_URI, "World", WCS_PREFIX);
    public static String TIFF = "tiff";

    // DEFAULT
    public static String DEFAULT_PREFIX = "gs";
    public static String DEFAULT_URI = "http://geoserver.org";

    // public static QName ENTIT\u00C9G\u00C9N\u00C9RIQUE = new QName( SF_URI,
    // "Entit\u00E9G\u00E9n\u00E9rique", SF_PREFIX );

    // Extra types
    public static QName GEOMETRYLESS = new QName(CITE_URI, "Geometryless", CITE_PREFIX);

    /** List of all cite types names */
    public static QName[] TYPENAMES =
            new QName[] {
                // WMS 1.1.1
                BASIC_POLYGONS,
                BRIDGES,
                BUILDINGS,
                DIVIDED_ROUTES,
                FORESTS,
                LAKES,
                MAP_NEATLINE,
                NAMED_PLACES,
                PONDS,
                ROAD_SEGMENTS,
                STREAMS, // WFS 1.0
                DELETES,
                FIFTEEN,
                INSERTS,
                LOCKS,
                NULLS,
                OTHER,
                SEVEN,
                UPDATES,
                LINES,
                MLINES,
                MPOINTS,
                MPOLYGONS,
                POINTS,
                POLYGONS, // WFS 1.1
                PRIMITIVEGEOFEATURE,
                AGGREGATEGEOFEATURE,
                GENERICENTITY,
                GEOMETRYLESS /* ENTIT\u00C9G\u00C9N\u00C9RIQUE */
            };

    /** List of wms type names. */
    public static QName[] WMS_TYPENAMES =
            new QName[] {
                BASIC_POLYGONS,
                BRIDGES,
                BUILDINGS,
                DIVIDED_ROUTES,
                FORESTS,
                LAKES,
                MAP_NEATLINE,
                NAMED_PLACES,
                PONDS,
                ROAD_SEGMENTS,
                STREAMS
            };

    /** List of wfs 1.0 type names. */
    public static QName[] WFS10_TYPENAMES =
            new QName[] {
                DELETES, FIFTEEN, INSERTS, LOCKS, NULLS, OTHER, SEVEN, UPDATES, LINES, MLINES,
                MPOINTS, MPOLYGONS, POINTS, POLYGONS
            };

    /** List of wfs 1.1 type names. */
    public static QName[] WFS11_TYPENAMES =
            new QName[] {
                PRIMITIVEGEOFEATURE,
                AGGREGATEGEOFEATURE,
                GENERICENTITY /* ENTIT\u00C9G\u00C9N\u00C9RIQUE */
            };

    /** map of qname to srs */
    public static HashMap<QName, Integer> SRS = new HashMap<QName, Integer>();

    static {
        for (int i = 0; i < WFS10_TYPENAMES.length; i++) {
            SRS.put(WFS10_TYPENAMES[i], 32615);
        }
        for (int i = 0; i < WFS11_TYPENAMES.length; i++) {
            SRS.put(WFS11_TYPENAMES[i], 4326);
        }
    }

    /** the base of the data directory */
    File data;

    /** the 'featureTypes' directory, under 'data' */
    File featureTypes;

    /** the 'coverages' directory, under 'data' */
    File coverages;

    /** the 'styles' directory, under 'data' */
    File styles;

    /** the 'plugIns' directory under 'data */
    File plugIns;

    /** the 'validation' directory under 'data */
    File validation;

    /** the 'templates' director under 'data' */
    File templates;

    /** The datastore definition map */
    HashMap dataStores = new HashMap();

    /** The set of disabled data stores */
    Set disabledDataStores = new HashSet();

    /** The datastore to namespace map */
    private HashMap dataStoreNamepaces = new HashMap();

    /** The namespaces map */
    private HashMap namespaces = new HashMap();

    /** The styles map */
    private HashMap layerStyles = new HashMap();

    /** The coverage store map */
    private HashMap coverageStores = new HashMap();

    /** The set of disabled coverage stores */
    Set disabledCoverageStores = new HashSet();

    /** The coverage store id to namespace map */
    private HashMap coverageStoresNamespaces = new HashMap();

    public MockData() throws IOException {
        // setup the root
        data = IOUtils.createRandomDirectory("./target", "mock", "data");
        data.delete();
        data.mkdir();

        // create a featureTypes directory
        featureTypes = new File(data, "featureTypes");
        featureTypes.mkdir();

        // create a coverages directory
        coverages = new File(data, "coverages");
        coverages.mkdir();

        // create the styles directory
        styles = new File(data, "styles");
        styles.mkdir();
        // copy over the minimal style
        IOUtils.copy(
                MockData.class.getResourceAsStream("Default.sld"), new File(styles, "Default.sld"));

        // plugins
        plugIns = new File(data, "plugIns");
        plugIns.mkdir();

        // validation
        validation = new File(data, "validation");
        validation.mkdir();

        // templates
        templates = new File(data, "templates");
        templates.mkdir();

        // setup basic map information
        namespaces.put(DEFAULT_PREFIX, DEFAULT_URI);
        namespaces.put("", DEFAULT_URI);
        layerStyles.put("Default", "Default.sld");
    }

    public void setUp() throws IOException {
        setUpCatalog();
        copyTo(MockData.class.getResourceAsStream("services.xml"), "services.xml");
    }

    public boolean isTestDataAvailable() {
        return true;
    }

    /** @return The root of the data directory. */
    public File getDataDirectoryRoot() {
        return data;
    }

    /** @return the "featureTypes" directory under the root */
    public File getFeatureTypesDirectory() {
        return featureTypes;
    }

    /** @return the "coverages" directory under the root */
    public File getCoveragesDirectory() {
        return coverages;
    }

    /**
     * Copies some content to a file under the base of the data directory.
     *
     * <p>The <code>location</code> is considred to be a path relative to the data directory root.
     *
     * <p>Note that the resulting file will be deleted when {@link #tearDown()} is called.
     *
     * @param input The content to copy.
     * @param location A relative path
     */
    public void copyTo(InputStream input, String location) throws IOException {
        IOUtils.copy(input, new File(getDataDirectoryRoot(), location));
    }

    /**
     * Copies some content to a file udner a specific feature type directory of the data directory.
     * Example:
     *
     * <p><code>
     *    dd.copyToFeautreTypeDirectory(input,MockData.PrimitiveGeoFeature,"info.xml");
     *  </code>
     *
     * @param input The content to copy.
     * @param featureTypeName The name of the feature type.
     * @param location The resulting location to copy to relative to the feautre type directory.
     */
    public void copyToFeatureTypeDirectory(
            InputStream input, QName featureTypeName, String location) throws IOException {

        copyTo(
                input,
                "featureTypes"
                        + File.separator
                        + featureTypeName.getPrefix()
                        + "_"
                        + featureTypeName.getLocalPart()
                        + File.separator
                        + location);
    }

    /**
     * Adds the list of well known types to the data directory. Well known types are listed as
     * constants in the MockData class header, and are organized as arrays based on the cite test
     * they do come from
     */
    public void addWellKnownTypes(QName[] names) throws IOException {
        for (int i = 0; i < names.length; i++) {
            QName name = names[i];
            addWellKnownType(name, null);
        }
    }

    /**
     * Adds a single well known type with the custom properties specified
     *
     * @param extraProperties The extra properties to be used
     */
    public void addWellKnownType(QName name, Map extraProperties) throws IOException {
        URL properties = MockData.class.getResource(name.getLocalPart() + ".properties");
        URL style = MockData.class.getResource(name.getLocalPart() + ".sld");
        String styleName = null;
        if (style != null) {
            styleName = name.getLocalPart();
            addStyle(styleName, style);
        }

        if (extraProperties == null)
            addPropertiesType(name, properties, Collections.singletonMap(KEY_STYLE, styleName));
        else {
            Map props = new HashMap(extraProperties);
            props.put(KEY_STYLE, styleName);
            addPropertiesType(name, properties, props);
        }
    }

    public void removeFeatureType(QName typeName) throws IOException {
        String prefix = typeName.getPrefix();
        String type = typeName.getLocalPart();
        File featureTypeDir = new File(featureTypes, prefix + "_" + type);
        if (!featureTypeDir.exists()) {
            throw new FileNotFoundException(
                    "Type directory not found: " + featureTypeDir.getAbsolutePath());
        }
        File info = new File(featureTypeDir, "info.xml");
        if (!info.exists()) {
            throw new FileNotFoundException(
                    "FeatureType file not found: " + featureTypeDir.getAbsolutePath());
        }
        if (!IOUtils.delete(featureTypeDir)) {
            throw new IOException(
                    "FetureType directory not deleted: " + featureTypeDir.getAbsolutePath());
        }
    }

    /** Adds the wcs 1.0 coverages. */
    public void addWcs10Coverages() throws Exception {
        URL style = MockData.class.getResource("raster.sld");
        String styleName = "raster";
        addStyle(styleName, style);

        addCoverageFromZip(USA_WORLDIMG, TestData.class.getResource("usa.zip"), PNG, styleName);
    }

    /** Adds the wcs 1.1 coverages. */
    public void addWcs11Coverages() throws Exception {
        URL style = MockData.class.getResource("raster.sld");
        String styleName = "raster";
        addStyle(styleName, style);

        // wcs 1.1
        addCoverage(TASMANIA_DEM, TestData.class.getResource("tazdem.tiff"), TIFF, styleName);
        addCoverage(TASMANIA_BM, TestData.class.getResource("tazbm.tiff"), TIFF, styleName);
        addCoverage(ROTATED_CAD, TestData.class.getResource("rotated.tiff"), TIFF, styleName);
        addCoverage(WORLD, TestData.class.getResource("world.tiff"), TIFF, styleName);
    }

    /**
     * Adds the specified style to the data directory
     *
     * @param styleId the style id
     * @param style an URL pointing to an SLD file to be copied into the data directory
     */
    public void addStyle(String styleId, URL style) throws IOException {
        layerStyles.put(styleId, styleId + ".sld");
        InputStream styleContents = style.openStream();
        File to = new File(styles, styleId + ".sld");
        IOUtils.copy(styleContents, to);
    }

    /**
     * Adds a property file as a feature type in a property datastore.
     *
     * @param name the fully qualified name of the feature type. The prefix and namespace URI will
     *     be used to create a namespace, the prefix will be used as the datastore name, the local
     *     name will become the feature type name
     * @param properties a URL to the property file backing the feature type. If null, an emtpy
     *     property file will be used
     * @param extraParams a map from extra configurable keys to their values (see for example
     */
    public void addPropertiesType(QName name, URL properties, Map extraParams) throws IOException {
        // sanitize input params
        if (extraParams == null) extraParams = Collections.EMPTY_MAP;

        // setup the type directory if needed
        File directory = new File(data, name.getPrefix());
        if (!directory.exists()) {
            directory.mkdir();
        }

        // create the properties file
        File f = new File(directory, name.getLocalPart() + ".properties");

        // copy over the contents
        InputStream propertiesContents;
        if (properties == null) propertiesContents = new ByteArrayInputStream("-=".getBytes());
        else propertiesContents = properties.openStream();
        IOUtils.copy(propertiesContents, f);

        // write the info file
        info(name, extraParams);

        // setup the meta information to be written in the catalog
        namespaces.put(name.getPrefix(), name.getNamespaceURI());
        dataStoreNamepaces.put(name.getPrefix(), name.getPrefix());
        Map params = new HashMap();
        params.put(PropertyDataStoreFactory.DIRECTORY.key, directory);
        params.put(PropertyDataStoreFactory.NAMESPACE.key, name.getNamespaceURI());
        dataStores.put(name.getPrefix(), params);
    }

    /**
     * Adds a new coverage.
     *
     * <p>Note that callers of this code should call <code>applicationContext.refresh()</code> in
     * order to force the catalog to reload.
     *
     * <p>The <tt>coverage</tt> parameter is an input stream containing a single uncompressed file
     * that's supposed to be a coverage (e.g., a GeoTiff).
     */
    public void addCoverage(QName name, URL coverage, String extension, String styleName)
            throws Exception {
        if (extension == null)
            throw new IllegalArgumentException("Use addCoverageFromZip instead of passing NULL");

        File directory = new File(data, name.getPrefix());
        if (!directory.exists()) {
            directory.mkdir();
        }

        // create the coverage file
        File f = new File(directory, name.getLocalPart() + "." + extension);

        IOUtils.copy(coverage.openStream(), f);

        addCoverageFromPath(
                name,
                f,
                "file:" + name.getPrefix() + "/" + name.getLocalPart() + "." + extension,
                styleName);
    }

    public void addCoverageFromZip(QName name, URL coverage, String extension, String styleName)
            throws Exception {
        File directory = new File(data, name.getPrefix());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File f = new File(directory, name.getLocalPart());
        f.mkdir();

        File compressedFile = new File(f, name.getLocalPart() + ".zip");
        IOUtils.copy(coverage.openStream(), compressedFile);
        IOUtils.decompress(compressedFile, f);
        final File srcDir = new File(f, name.getLocalPart());
        srcDir.mkdir();
        FileUtils.copyDirectory(srcDir, f, true);

        if (extension != null) {
            File coverageFile = new File(srcDir, name.getLocalPart() + "." + extension);
            addCoverageFromPath(
                    name,
                    coverageFile,
                    "file:"
                            + name.getPrefix()
                            + "/"
                            + name.getLocalPart()
                            + "/"
                            + name.getLocalPart()
                            + "."
                            + extension,
                    styleName);
        } else {
            addCoverageFromPath(
                    name, f, "file:" + name.getPrefix() + "/" + name.getLocalPart(), styleName);
        }
    }

    private void addCoverageFromPath(QName name, File coverage, String relpath, String styleName)
            throws Exception {
        coverageInfo(name, coverage, styleName);

        // setup the meta information to be written in the catalog
        AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(coverage);
        namespaces.put(name.getPrefix(), name.getNamespaceURI());
        coverageStoresNamespaces.put(name.getLocalPart(), name.getPrefix());
        Map params = new HashMap();
        params.put(CatalogWriter.COVERAGE_TYPE_KEY, format.getName());
        params.put(CatalogWriter.COVERAGE_URL_KEY, relpath);
        coverageStores.put(name.getLocalPart(), params);
    }

    /** Disables the specificed datastore (it's still configured, but with enabled=false) */
    public void disableDataStore(String datastoreId) {
        disabledDataStores.add(datastoreId);
    }

    /** Disables the specificed coveragestore (it's still configured, but with enabled=false) */
    public void disableCoverageStore(String datastoreId) {
        disabledCoverageStores.add(datastoreId);
    }

    /** Populates a map with prefix to namespace uri mappings for all the mock data namespaces. */
    public void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put(MockData.CITE_PREFIX, MockData.CITE_URI);
        namespaces.put(MockData.CDF_PREFIX, MockData.CDF_URI);
        namespaces.put(MockData.CGF_PREFIX, MockData.CGF_URI);
        namespaces.put(MockData.SF_PREFIX, MockData.SF_URI);
    }

    /** Sets up the catalog in the data directory */
    protected void setUpCatalog() throws IOException {
        // create the catalog.xml
        CatalogWriter writer = new CatalogWriter();
        writer.dataStores(dataStores, dataStoreNamepaces, disabledDataStores);
        writer.coverageStores(coverageStores, coverageStoresNamespaces, disabledCoverageStores);
        writer.namespaces(namespaces);
        writer.styles(layerStyles);
        writer.write(new File(data, "catalog.xml"));
    }

    void properties(QName name) throws IOException {
        // copy over the properties file
        InputStream from = MockData.class.getResourceAsStream(name.getLocalPart() + ".properties");

        File directory = new File(data, name.getPrefix());
        directory.mkdir();

        File to = new File(directory, name.getLocalPart() + ".properties");
        IOUtils.copy(from, to);
    }

    void info(QName name, Map<String, Object> extraParams) throws IOException {
        String type = name.getLocalPart();
        String prefix = name.getPrefix();

        // prepare extra params default
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(KEY_STYLE, "Default");
        params.put(KEY_ALIAS, null);

        Integer srs = SRS.get(name);
        if (srs == null) {
            srs = 4326;
        }
        params.put(KEY_SRS_NUMBER, srs);

        // override with whatever the user provided
        params.putAll(extraParams);

        File featureTypeDir = new File(featureTypes, prefix + "_" + type);
        featureTypeDir.mkdir();

        File info = new File(featureTypeDir, "info.xml");
        info.delete();
        info.createNewFile();

        FileWriter writer = new FileWriter(info);
        writer.write("<featureType datastore=\"" + prefix + "\">");
        writer.write("<name>" + type + "</name>");
        if (params.get(KEY_ALIAS) != null)
            writer.write("<alias>" + params.get(KEY_ALIAS) + "</alias>");
        writer.write("<SRS>" + params.get(KEY_SRS_NUMBER) + "</SRS>");
        // this mock type may have wrong SRS compared to the actual one in the property files...
        // let's configure SRS handling not to alter the original one, and have 4326 used only
        // for capabilities
        int srsHandling = 2;
        Object handling = params.get(KEY_SRS_HANDLINGS);
        if (handling != null) {
            if (handling instanceof ProjectionPolicy) {
                srsHandling = ((ProjectionPolicy) params.get(KEY_SRS_HANDLINGS)).getCode();
            } else if (handling instanceof Number) {
                srsHandling = ((Number) params.get(KEY_SRS_HANDLINGS)).intValue();
            }
        }
        writer.write("<SRSHandling>" + srsHandling + "</SRSHandling>");
        writer.write("<title>" + type + "</title>");
        writer.write("<abstract>abstract about " + type + "</abstract>");
        writer.write("<numDecimals value=\"8\"/>");
        writer.write("<keywords>" + type + "</keywords>");
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
    }

    void coverageInfo(QName name, File coverageFile, String styleName) throws Exception {
        coverageInfo(name, coverageFile, null, styleName);
    }

    void coverageInfo(QName name, Object coverageFile, String gridFormat, String styleName)
            throws Exception {
        String coverage = name.getLocalPart();

        File coverageDir = new File(coverages, coverage);
        coverageDir.mkdir();

        File info = new File(coverageDir, "info.xml");
        info.createNewFile();

        // let's grab the necessary metadata
        AbstractGridFormat format =
                (AbstractGridFormat)
                        (gridFormat != null
                                ? CoverageStoreUtils.acquireFormat(gridFormat)
                                : GridFormatFinder.findFormat(coverageFile));
        GridCoverage2DReader reader;
        try {
            reader = (GridCoverage2DReader) format.getReader(coverageFile);
        } catch (Exception e) {
            String message =
                    "Exception while trying to read "
                            + coverageFile.toString()
                            + " with format"
                            + format.getName();
            throw new RuntimeException(message, e);
        }

        if (reader == null) {
            throw new RuntimeException(
                    "No reader for "
                            + coverageFile.toString()
                            + " with format "
                            + format.getName());
        }
        // basic info
        FileWriter writer = new FileWriter(info);
        writer.write("<coverage format=\"" + coverage + "\">\n");
        writer.write("<name>" + coverage + "</name>\n");
        writer.write("<label>" + coverage + "</label>\n");
        writer.write("<description>" + coverage + " description</description>\n");
        writer.write(
                "<metadataLink about = \"http://www.remotesensing.org:16080/websites/geotiff/geotiff.html\" metadataType = \"other\" />");
        writer.write("<keywords>WCS," + coverage + " </keywords>\n");
        if (styleName == null) styleName = "raster";
        writer.write("<styles default=\"" + styleName + "\"/>\n");

        // envelope
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        GeneralEnvelope wgs84envelope = CoverageStoreUtils.getWGS84LonLatEnvelope(envelope);
        final String nativeCrsName = CRS.lookupIdentifier(crs, false);
        writer.write(
                "<envelope crs=\""
                        + crs.toString().replaceAll("\"", "'")
                        + "\" srsName=\""
                        + nativeCrsName
                        + "\">\n");
        writer.write(
                "<pos>"
                        + wgs84envelope.getMinimum(0)
                        + " "
                        + wgs84envelope.getMinimum(1)
                        + "</pos>\n");
        writer.write(
                "<pos>"
                        + wgs84envelope.getMaximum(0)
                        + " "
                        + wgs84envelope.getMaximum(1)
                        + "</pos>\n");
        writer.write("</envelope>\n");

        /**
         * Now reading a fake small GridCoverage just to retrieve meta information: - calculating a
         * new envelope which is 1/20 of the original one - reading the GridCoverage subset
         */
        final ParameterValueGroup readParams = reader.getFormat().getReadParameters();
        final Map parameters = CoverageUtils.getParametersKVP(readParams);
        double[] minCP = envelope.getLowerCorner().getCoordinate();
        double[] maxCP =
                new double[] {
                    minCP[0] + (envelope.getSpan(0) / 20.0), minCP[1] + (envelope.getSpan(1) / 20.0)
                };
        final GeneralEnvelope subEnvelope = new GeneralEnvelope(minCP, maxCP);
        subEnvelope.setCoordinateReferenceSystem(reader.getCoordinateReferenceSystem());

        parameters.put(
                AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(),
                new GridGeometry2D(reader.getOriginalGridRange(), subEnvelope));
        GridCoverage2D gc =
                (GridCoverage2D)
                        reader.read(CoverageUtils.getParameters(readParams, parameters, true));

        // grid geometry
        final GridGeometry geometry = gc.getGridGeometry();
        final int dimensions = geometry.getGridRange().getDimension();
        String lower = "";
        String upper = "";
        for (int i = 0; i < dimensions; i++) {
            lower = lower + geometry.getGridRange().getLow(i) + " ";
            upper = upper + geometry.getGridRange().getHigh(i) + " ";
        }
        writer.write("<grid dimension = \"" + dimensions + "\">\n");
        writer.write("<low>" + lower + "</low>\n");
        writer.write("<high>" + upper + "</high>\n");
        final CoordinateSystem cs = crs.getCoordinateSystem();
        for (int i = 0; i < cs.getDimension(); i++) {
            writer.write("<axisName>" + cs.getAxis(i).getName().getCode() + "</axisName>\n");
        }
        if (geometry.getGridToCRS() instanceof AffineTransform) {
            AffineTransform aTX = (AffineTransform) geometry.getGridToCRS();
            writer.write("<geoTransform>");
            writer.write("<scaleX>" + aTX.getScaleX() + "</scaleX>\n");
            writer.write("<scaleY>" + aTX.getScaleY() + "</scaleY>\n");
            writer.write("<shearX>" + aTX.getShearX() + "</shearX>\n");
            writer.write("<shearY>" + aTX.getShearY() + "</shearY>\n");
            writer.write("<translateX>" + aTX.getTranslateX() + "</translateX>\n");
            writer.write("<translateY>" + aTX.getTranslateY() + "</translateY>\n");
            writer.write("</geoTransform>\n");
        }
        writer.write("</grid>\n");

        // coverage dimensions
        final GridSampleDimension[] sd = gc.getSampleDimensions();
        for (int i = 0; i < sd.length; i++) {
            writer.write("<CoverageDimension>\n");
            writer.write("<name>" + sd[i].getDescription().toString() + "</name>\n");
            writer.write("<interval>\n");
            writer.write("<min>" + sd[i].getMinimumValue() + "</min>\n");
            writer.write("<max>" + sd[i].getMaximumValue() + "</max>\n");
            writer.write("</interval>\n");
            final List<Category> categories = sd[i].getCategories();
            if (categories != null && categories.size() >= 1) {
                writer.write("<nullValues>\n");
                for (Iterator<Category> it = sd[i].getCategories().iterator(); it.hasNext(); ) {
                    Category cat = (Category) it.next();
                    if ((cat != null) && cat.getName().toString().equalsIgnoreCase("no data")) {
                        double min = cat.getRange().getMinimum();
                        double max = cat.getRange().getMaximum();
                        writer.write("<value>" + min + "</value>\n");
                        if (min != max) writer.write("<value>" + max + "</value>\n");
                    }
                }
                writer.write("</nullValues>\n");
            } else writer.write("<nullValues/>\n");
            writer.write("</CoverageDimension>\n");
        }

        // supported crs
        writer.write("<supportedCRSs>\n");
        writer.write("<requestCRSs>" + nativeCrsName + "</requestCRSs>\n");
        writer.write("<responseCRSs>" + nativeCrsName + "</responseCRSs>\n");
        writer.write("</supportedCRSs>\n");

        // supported formats
        writer.write("<supportedFormats nativeFormat = \"" + format.getName() + "\">\n");
        writer.write("<formats>ARCGRID,ARCGRID-GZIP,GEOTIFF,PNG,GIF,TIFF</formats>\n");
        writer.write("</supportedFormats>\n");

        // supported interpolations
        writer.write("<supportedInterpolations default = \"nearest neighbor\">\n");
        writer.write("<interpolationMethods>nearest neighbor</interpolationMethods>\n");
        writer.write("</supportedInterpolations>\n");

        // the end
        writer.write("</coverage>\n");
        writer.flush();
        writer.close();
    }

    /** Kills the data directory, deleting all the files. */
    public void tearDown() throws IOException {
        //        IOUtils.delete(templates);
        //        IOUtils.delete(validation);
        //        IOUtils.delete(plugIns);
        //        IOUtils.delete(styles);
        //        IOUtils.delete(featureTypes);
        //        IOUtils.delete(coverages);
        IOUtils.delete(data);

        styles = null;
        featureTypes = null;
        data = null;
    }

    public void addCustomType(QName name, Map params) throws IOException {
        // write the info file
        info(name, params);
        // setup the meta information to be written in the catalog
        namespaces.put(name.getPrefix(), name.getNamespaceURI());
        dataStoreNamepaces.put(name.getPrefix(), name.getPrefix());
        dataStores.put(name.getPrefix(), params);
    }

    public void addCustomCoverage(QName name, Map params) throws Exception {
        // write the info file
        coverageInfo(
                name,
                params.get(CatalogWriter.COVERAGE_URL_KEY),
                (String) params.get(CatalogWriter.COVERAGE_TYPE_KEY),
                "generic");
        // setup the meta information to be written in the catalog
        namespaces.put(name.getPrefix(), name.getNamespaceURI());
        coverageStoresNamespaces.put(name.getLocalPart(), name.getPrefix());
        coverageStores.put(name.getLocalPart(), params);
    }
}

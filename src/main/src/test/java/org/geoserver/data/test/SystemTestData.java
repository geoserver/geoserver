/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.URLs;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

/**
 * Test setup uses for GeoServer system tests.
 *
 * <p>This is the default test setup used by {@link GeoServerSystemTestSupport}. During setup this
 * class creates a full GeoServer data directory configuration on disk.
 *
 * <p>Customizing the setup can be done in two ways. Customizations that occur pre system startup
 * and those that happen after. Methods that may be called pre system start with the prefix "setUp".
 * Methods that may be called after system startup are prefixed with "add".
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SystemTestData extends CiteTestData {

    /** Multiband tiff */
    public static final QName MULTIBAND = new QName(WCS_URI, "multiband", WCS_PREFIX);

    static final Logger LOGGER = Logging.getLogger(SystemTestData.class);

    /** Keys for overriding default layer properties */
    public static class LayerProperty<T> {

        T get(Map<LayerProperty, Object> map, T def) {
            return map != null && map.containsKey(this) ? (T) map.get(this) : def;
        }

        public static LayerProperty<String> NAME = new LayerProperty<String>();
        public static LayerProperty<ProjectionPolicy> PROJECTION_POLICY =
                new LayerProperty<ProjectionPolicy>();
        public static LayerProperty<String> STYLE = new LayerProperty<String>();
        public static LayerProperty<ReferencedEnvelope> ENVELOPE =
                new LayerProperty<ReferencedEnvelope>();
        public static LayerProperty<ReferencedEnvelope> LATLON_ENVELOPE =
                new LayerProperty<ReferencedEnvelope>();
        public static LayerProperty<Integer> SRS = new LayerProperty<Integer>();
        public static LayerProperty<String> STORE = new LayerProperty<String>();
    }

    /** Keys for overriding default layer properties */
    public static class StyleProperty<T> {

        T get(Map<StyleProperty, Object> map, T def) {
            return map != null && map.containsKey(this) ? (T) map.get(this) : def;
        }

        public static StyleProperty<String> FORMAT = new StyleProperty<String>();
        public static StyleProperty<Version> FORMAT_VERSION = new StyleProperty<Version>();
        public static StyleProperty<LegendInfo> LEGEND_INFO = new StyleProperty<LegendInfo>();
    }

    /** data directory root */
    protected File data;

    /** internal catalog, used for setup before the real catalog available */
    Catalog catalog;

    public SystemTestData() throws IOException {
        // setup the root
        data = IOUtils.createRandomDirectory("./target", "default", "data");
        data.delete();
        data.mkdir();
    }

    public SystemTestData(File data) {
        this.data = data;
    }

    @Override
    public void setUp() throws Exception {
        createCatalog();
        createConfig();
    }

    public void setUpDefault() throws Exception {
        setUpDefaultLayers();
        setUpSecurity();
    }

    /**
     * Sets up the default set of layers, which is all the vector layers whose names are included in
     * the {@link CiteTestData#TYPENAMES} array.
     */
    public void setUpDefaultLayers() throws IOException {
        for (QName layerName : TYPENAMES) {
            addVectorLayer(layerName, catalog);
        }
    }

    /**
     * Sets up the default set of raster layers.
     *
     * <p>Layer names included in this set include:
     *
     * <ul>
     *   <li>{@link CiteTestData#TASMANIA_BM}
     *   <li>{@link CiteTestData#TASMANIA_DEM}
     *   <li>{@link CiteTestData#ROTATED_CAD}
     *   <li>{@link CiteTestData#WORLD}
     * </ul>
     */
    public void setUpDefaultRasterLayers() throws IOException {
        addWorkspace(WCS_PREFIX, WCS_URI, catalog);
        addDefaultRasterLayer(TASMANIA_DEM, catalog);
        addDefaultRasterLayer(TASMANIA_BM, catalog);
        addDefaultRasterLayer(ROTATED_CAD, catalog);
        addDefaultRasterLayer(WORLD, catalog);
        addDefaultRasterLayer(MULTIBAND, catalog);
    }

    public void setUpWcs10RasterLayers() throws IOException {
        addRasterLayer(USA_WORLDIMG, "usa.zip", PNG, catalog);
    }

    /**
     * Sets up the WCS 11 raster layers.
     *
     * <p>This method is a synonym for {@link #setUpDefaultLayers()}
     */
    public void setUpWcs11RasterLayers() throws IOException {
        setUpDefaultRasterLayers();
    }

    /**
     * Adds a vector layer to the setup with no custom properties.
     *
     * <p>This method should be called during the pre system setup phase, for example from {@link
     * GeoServerSystemTestSupport#setUpTestData(SystemTestData)}.
     *
     * @see {@link #addVectorLayer(QName, Catalog)}
     */
    public void setUpVectorLayer(QName layerName) throws IOException {
        addVectorLayer(layerName, catalog);
    }

    /**
     * Adds a vector layer to the setup.
     *
     * <p>This method should be called during the pre system setup phase, for example from {@link
     * GeoServerSystemTestSupport#setUpTestData(SystemTestData)}.
     *
     * @see {@link #addVectorLayer(QName, Map, Catalog)}
     */
    public void setUpVectorLayer(QName qName, Map<LayerProperty, Object> props) throws IOException {
        addVectorLayer(qName, props, catalog);
    }

    /**
     * Adds a vector layer to the setup.
     *
     * <p>This method should be called during the pre system setup phase, for example from {@link
     * GeoServerSystemTestSupport#setUpTestData(SystemTestData)}.
     *
     * @see {@link #addVectorLayer(QName, Map, Class, Catalog)}
     */
    public void setUpVectorLayer(QName qName, Map<LayerProperty, Object> props, Class scope)
            throws IOException {
        addVectorLayer(qName, props, scope, catalog);
    }

    /**
     * Adds a vector layer to the setup.
     *
     * <p>This method should be called during the pre system setup phase, for example from {@link
     * GeoServerSystemTestSupport#setUpTestData(SystemTestData)}.
     *
     * @see {@link #addVectorLayer(QName, Map, String, Class, Catalog)}
     */
    public void setUpVectorLayer(
            QName qName, Map<LayerProperty, Object> props, String filename, Class scope)
            throws IOException {
        addVectorLayer(qName, props, filename, scope, catalog);
    }

    /**
     * Adds a raster layer to the setup with no custom properties.
     *
     * <p>This method should be called during the pre system setup phase, for example from {@link
     * GeoServerSystemTestSupport#setUpTestData(SystemTestData)}.
     *
     * @see {@link #addRasterLayer(QName, String, String, Catalog)}
     */
    public void setUpRasterLayer(QName qName, String filename, String extension)
            throws IOException {
        addRasterLayer(qName, filename, extension, catalog);
    }

    /**
     * Adds a raster layer to the setup.
     *
     * <p>This method should be called during the pre system setup phase, for example from {@link
     * GeoServerSystemTestSupport#setUpTestData(SystemTestData)}.
     *
     * @see {@link #addRasterLayer(QName, String, String, Map, Catalog)}
     */
    public void setUpRasterLayer(
            QName qName, String filename, String extension, Map<LayerProperty, Object> props)
            throws IOException {
        addRasterLayer(qName, filename, extension, props, catalog);
    }

    /**
     * Adds a raster layer to the setup.
     *
     * <p>This method should be called during the pre system setup phase, for example from {@link
     * GeoServerSystemTestSupport#setUpTestData(SystemTestData)}.
     *
     * @see {@link #addRasterLayer(QName, String, String, Map, Class, Catalog)}
     */
    public void setUpRasterLayer(
            QName qName,
            String filename,
            String extension,
            Map<LayerProperty, Object> props,
            Class scope)
            throws IOException {
        addRasterLayer(qName, filename, extension, props, scope, catalog);
    }

    public void setUpSecurity() throws IOException {
        File secDir = new File(getDataDirectoryRoot(), "security");
        IOUtils.decompress(SystemTestData.class.getResourceAsStream("security.zip"), secDir);
        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor.contains("IBM")) {
            IOUtils.copy(
                    new File(secDir, "geoserver.jceks.ibm"), new File(secDir, "geoserver.jceks"));
        } else {
            IOUtils.copy(
                    new File(secDir, "geoserver.jceks.default"),
                    new File(secDir, "geoserver.jceks"));
        }
    }

    protected void createCatalog() throws IOException {
        CatalogImpl catalog = new CatalogImpl();
        catalog.setExtendedValidation(false);
        catalog.setResourceLoader(new GeoServerResourceLoader(data));

        catalog.addListener(
                new GeoServerConfigPersister(
                        catalog.getResourceLoader(), createXStreamPersister()));
        catalog.addListener(new GeoServerResourcePersister(catalog));

        // workspaces
        addWorkspace(DEFAULT_PREFIX, DEFAULT_URI, catalog);
        addWorkspace(SF_PREFIX, SF_URI, catalog);
        addWorkspace(CITE_PREFIX, CITE_URI, catalog);
        addWorkspace(CDF_PREFIX, CDF_URI, catalog);
        addWorkspace(CGF_PREFIX, CGF_URI, catalog);

        // default style
        addStyle(DEFAULT_VECTOR_STYLE, catalog);
        addStyle(DEFAULT_RASTER_STYLE, catalog);

        this.catalog = catalog;
    }

    protected void createConfig() {
        GeoServerImpl geoServer = new GeoServerImpl();
        geoServer.addListener(
                new GeoServerConfigPersister(
                        new GeoServerResourceLoader(data), createXStreamPersister()));
        catalog.addListener(new GeoServerResourcePersister(catalog));

        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal(global);
        addSettings(null, geoServer);

        LoggingInfo logging = geoServer.getFactory().createLogging();
        geoServer.setLogging(logging);
    }

    XStreamPersister createXStreamPersister() {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        xp.setEncryptPasswordFields(false);
        return xp;
    }

    /**
     * Adds a workspace to the test setup.
     *
     * @param name The name of the workspace.
     * @param uri The namespace uri associated with the workspace.
     */
    public void addWorkspace(String name, String uri, Catalog catalog) {
        addWorkspace(name, uri, false, catalog);
    }

    /**
     * Adds a workspace to the test setup. If the workspace and namespace already exists they will
     * be updated, otherwise they will be created.
     *
     * @param name name of the workspace
     * @param uri the namespace URI associated with the workspace
     * @param isolated TRUE if the workspace and associated namespace are isolated
     * @param catalog the catalog were to store \ update the workspace and associated namespace
     */
    public void addWorkspace(String name, String uri, boolean isolated, Catalog catalog) {

        // let's see if the workspace already exists
        WorkspaceInfo ws = catalog.getWorkspaceByName(name);
        if (ws == null) {
            // new workspace, we need to create a new one
            ws = catalog.getFactory().createWorkspace();
            ws.setName(name);
            ws.setIsolated(isolated);
            catalog.add(ws);
        } else {
            // existing workspace, let's update the isolation state
            ws.setIsolated(isolated);
            catalog.save(ws);
        }

        // let's see if the namespace associated with the workspace already exists
        NamespaceInfo ns = catalog.getNamespaceByPrefix(name);
        if (ns == null) {
            // new namespace, we need to create a new one
            ns = catalog.getFactory().createNamespace();
            ns.setPrefix(name);
            ns.setURI(uri);
            ns.setIsolated(isolated);
            catalog.add(ns);
        } else {
            // existing namespace, let's update the URI and isolation state
            ns.setURI(uri);
            ns.setIsolated(isolated);
            catalog.save(ns);
        }
    }

    /**
     * Adds a style to the test setup.
     *
     * <p>To set up the style a file named <tt>name</tt>.sld is copied from the classpath relative
     * to this class.
     *
     * @param name The name of the style.
     */
    public void addStyle(String name, Catalog catalog) throws IOException {
        addStyle(name, getClass(), catalog);
    }

    /**
     * Adds a style to the test setup.
     *
     * <p>To set up the style a file named <tt>name</tt>.sld is copied from the classpath relative
     * to the <tt>scope</tt> parameter.
     *
     * @param name The name of the style.
     * @param scope Class from which to load sld resource from.
     */
    public void addStyle(String name, Class scope, Catalog catalog) throws IOException {
        addStyle(name, name + ".sld", scope, catalog);
    }

    /**
     * Adds a style to the test setup.
     *
     * <p>To set up the style a file named <tt>filename</tt> is copied from the classpath relative
     * to the <tt>scope</tt> parameter.
     *
     * @param name The name of the style.
     * @param filename The filename to copy from classpath.
     * @param scope Class from which to load sld resource from.
     */
    public void addStyle(String name, String filename, Class scope, Catalog catalog)
            throws IOException {
        addStyle((WorkspaceInfo) null, name, filename, scope, catalog);
    }

    /**
     * Adds a style to the test setup.
     *
     * <p>To set up the style a file named <tt>filename</tt> is copied from the classpath relative
     * to the <tt>scope</tt> parameter. Example: "../temperature.sld" is copied to
     * "styles/temperature.sld".
     *
     * @param ws The workspace to include the style in
     * @param name The name of the style.
     * @param filename The filename to copy from classpath.
     * @param scope Class from which to load sld resource from.
     */
    public void addStyle(
            WorkspaceInfo ws, String name, String filename, Class scope, Catalog catalog)
            throws IOException {
        addStyle(ws, name, filename, scope, catalog, (Map) null);
    }

    /**
     * Adds a style to the test setup.
     *
     * <p>To set up the style a file named <tt>filename</tt> is copied from the classpath relative
     * to the <tt>scope</tt> parameter.
     *
     * @param ws The workspace to include the style in.
     * @param name The name of the style.
     * @param filename The filename to copy from classpath.
     * @param scope Class from which to load sld resource.
     * @param legend The legend for the style.
     */
    public void addStyle(
            WorkspaceInfo ws,
            String name,
            String filename,
            Class scope,
            Catalog catalog,
            LegendInfo legend)
            throws IOException {

        addStyle(
                ws,
                name,
                filename,
                scope,
                catalog,
                Collections.singletonMap(StyleProperty.LEGEND_INFO, legend));
    }

    /**
     * Adds a style to the test setup.
     *
     * <p>To set up the style a file named <tt>filename</tt> is copied from the classpath relative
     * to the <tt>scope</tt> parameter.
     *
     * @param ws The workspace to include the style in.
     * @param name The name of the style.
     * @param filename The filename to copy from classpath.
     * @param scope Class from which to load sld resource.
     * @param properties One of the well known style properties
     */
    public void addStyle(
            WorkspaceInfo ws,
            String name,
            String filename,
            Class scope,
            Catalog catalog,
            Map<StyleProperty, Object> properties)
            throws IOException {

        StyleInfo style = catalog.getStyleByName(ws, name);
        if (style == null) {
            style = catalog.getFactory().createStyle();
            style.setName(name);
            style.setWorkspace(ws);
        }

        GeoServerDataDirectory data = new GeoServerDataDirectory(this.data);
        File styles = data.get(style, "").dir();
        String target = new File(filename).getName();
        catalog.getResourceLoader().copyFromClassPath(filename, new File(styles, target), scope);
        style.setFilename(target);
        style.setFormat(StyleProperty.FORMAT.get(properties, SLDHandler.FORMAT));
        style.setFormatVersion(StyleProperty.FORMAT_VERSION.get(properties, SLDHandler.VERSION_10));
        style.setLegend(StyleProperty.LEGEND_INFO.get(properties, null));
        if (style.getId() == null) {
            catalog.add(style);
        } else {
            catalog.save(style);
        }
    }

    /**
     * Adds a vector layer to the catalog setup.
     *
     * <p>This method calls through to {@link #addVectorLayer(QName, Map, Catalog)} with no custom
     * properties.
     */
    public void addVectorLayer(QName qName, Catalog catalog) throws IOException {
        addVectorLayer(qName, new HashMap(), catalog);
    }

    /**
     * Adds a vector layer to the catalog setup.
     *
     * <p>This method calls through to {@link #addVectorLayer(QName, Map, Class, Catalog)} passing
     * in this class as the scope.
     */
    public void addVectorLayer(QName qName, Map<LayerProperty, Object> props, Catalog catalog)
            throws IOException {
        addVectorLayer(qName, props, getClass(), catalog);
    }

    /**
     * Adds a vector layer to the catalog setup.
     *
     * <p>The layer is created within a store named <code>qName.getPrefix()</code>, creating it if
     * it does not exist. The resulting store is a {@link PropertyDataStore} that points at the
     * directory <code>getDataDirectoryRoot()/qName.getPrefix()</code>. Similarily the layer and
     * store are created within a workspace named <code>qName.getPrefix()</code>, which is created
     * if it does not already exist.
     *
     * <p>The properties data for the layer is copied from the classpath, with a file name of "
     * <code>qName.getLocalPart()</code>.properties". The <tt>scope</tt> parameter is used as the
     * class from which to load the properties file relative to.
     *
     * <p>The <tt>props</tt> parameter is used to define custom properties for the layer. See the
     * {@link LayerProperty} class for supported properties.
     */
    public void addVectorLayer(
            QName qName, Map<LayerProperty, Object> props, Class scope, Catalog catalog)
            throws IOException {
        addVectorLayer(qName, props, qName.getLocalPart() + ".properties", scope, catalog);
    }

    /**
     * Adds a vector layer to the catalog setup.
     *
     * <p>The layer is created within a store named <code>qName.getPrefix()</code>, creating it if
     * it does not exist. The resulting store is a {@link PropertyDataStore} that points at the
     * directory <code>getDataDirectoryRoot()/qName.getPrefix()</code>. Similarily the layer and
     * store are created within a workspace named <code>qName.getPrefix()</code>, which is created
     * if it does not already exist.
     *
     * <p>The properties data for the layer is copied from the classpath, with a file name of "
     * <code>filename</code>.properties". The <tt>scope</tt> parameter is used as the class from
     * which to load the properties file relative to.
     *
     * <p>The <tt>props</tt> parameter is used to define custom properties for the layer. See the
     * {@link LayerProperty} class for supported properties.
     */
    public void addVectorLayer(
            QName qName,
            Map<LayerProperty, Object> props,
            String filename,
            Class scope,
            Catalog catalog)
            throws IOException {
        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();
        String uri = qName.getNamespaceURI();
        String storeName;
        if (LayerProperty.STORE.get(props, null) != null) {
            storeName = LayerProperty.STORE.get(props, null);
        } else {
            storeName = prefix;
        }

        // configure workspace if it doesn;t already exist
        if (catalog.getWorkspaceByName(prefix) == null) {
            addWorkspace(prefix, uri, catalog);
        }

        // configure store if it doesn't already exist

        File storeDir = catalog.getResourceLoader().findOrCreateDirectory(storeName);

        DataStoreInfo store = catalog.getDataStoreByName(storeName);
        if (store == null) {
            store = catalog.getFactory().createDataStore();
            store.setName(storeName);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);

            store.getConnectionParameters().put(PropertyDataStoreFactory.DIRECTORY.key, storeDir);
            store.getConnectionParameters().put(PropertyDataStoreFactory.NAMESPACE.key, uri);
            catalog.add(store);
        }

        // copy the properties file over

        catalog.getResourceLoader()
                .copyFromClassPath(filename, new File(storeDir, filename), scope);

        // configure feature type
        FeatureTypeInfo featureType = catalog.getFactory().createFeatureType();
        featureType.setStore(store);
        featureType.setNamespace(catalog.getNamespaceByPrefix(prefix));
        featureType.setName(LayerProperty.NAME.get(props, name));
        featureType.setNativeName(FilenameUtils.getBaseName(filename));
        featureType.setTitle(name);
        featureType.setAbstract("abstract about " + name);

        Integer srs = LayerProperty.SRS.get(props, SRS.get(qName));
        if (srs == null) {
            srs = 4326;
        }
        featureType.setSRS("EPSG:" + srs);
        try {
            featureType.setNativeCRS(CRS.decode("EPSG:" + srs));
        } catch (Exception e) {
            LOGGER.warning("Failed to decode EPSG:" + srs + ", setting the native SRS to null");
        }
        featureType.setNumDecimals(8);
        featureType.getKeywords().add(new Keyword(name));
        featureType.setEnabled(true);
        featureType.setProjectionPolicy(
                LayerProperty.PROJECTION_POLICY.get(props, ProjectionPolicy.NONE));
        featureType.setLatLonBoundingBox(
                LayerProperty.LATLON_ENVELOPE.get(props, DEFAULT_LATLON_ENVELOPE));
        featureType.setNativeBoundingBox(LayerProperty.ENVELOPE.get(props, null));

        FeatureTypeInfo ft = catalog.getFeatureTypeByDataStore(store, name);
        LayerInfo layer = catalog.getLayerByName(new NameImpl(uri, name));
        if (ft == null) {
            ft = featureType;
            catalog.add(featureType);
        } else {
            if (layer == null) {
                // handles the case of layer removed, but feature type not
                catalog.remove(ft);
                ft = featureType;
                catalog.add(featureType);
            } else {
                new CatalogBuilder(catalog).updateFeatureType(ft, featureType);
                catalog.save(ft);
            }
        }

        if (layer == null
                || !layer.getResource()
                        .getNamespace()
                        .equals(catalog.getNamespaceByPrefix(prefix))) {
            layer = catalog.getFactory().createLayer();
        }

        layer.setResource(ft);

        StyleInfo defaultStyle = null;
        if (LayerProperty.STYLE.get(props, null) != null) {
            defaultStyle = catalog.getStyleByName(LayerProperty.STYLE.get(props, null));
        } else {
            // look for a style matching the layer name
            defaultStyle = catalog.getStyleByName(name);
            if (defaultStyle == null) {
                // see if the resource exists and we just need to create it
                if (getClass().getResource(name + ".sld") != null) {
                    addStyle(name, catalog);
                    defaultStyle = catalog.getStyleByName(name);
                }
            }
        }

        if (defaultStyle == null) {
            defaultStyle = catalog.getStyleByName(DEFAULT_VECTOR_STYLE);
        }

        layer.getStyles().clear();
        layer.setDefaultStyle(defaultStyle);
        layer.setType(PublishedType.VECTOR);
        layer.setEnabled(true);

        if (layer.getId() == null) {
            catalog.add(layer);
        } else {
            catalog.save(layer);
        }
    }

    /**
     * Adds one of the default raster layers.
     *
     * <p>The <tt>name</tt> parameter must be one of:
     *
     * <ul>
     *   <li>{@link CiteTestData#TASMANIA_BM}
     *   <li>{@link CiteTestData#TASMANIA_DEM}
     *   <li>{@link CiteTestData#ROTATED_CAD}
     *   <li>{@link CiteTestData#WORLD}
     * </ul>
     */
    public void addDefaultRasterLayer(QName name, Catalog catalog) throws IOException {
        if (name.equals(TASMANIA_DEM)) {
            addRasterLayer(name, "tazdem.tiff", null, catalog);
        } else if (name.equals(TASMANIA_BM)) {
            addRasterLayer(name, "tazbm.tiff", null, catalog);
        } else if (name.equals(ROTATED_CAD)) {
            addRasterLayer(name, "rotated.tiff", null, catalog);
        } else if (name.equals(WORLD)) {
            addRasterLayer(name, "world.tiff", null, catalog);
        } else if (name.equals(MULTIBAND)) {
            addRasterLayer(name, "multiband.tiff", null, catalog);
        } else {
            throw new IllegalArgumentException("Unknown default raster layer: " + name);
        }
    }

    /**
     * Adds a raster layer to the setup with no custom properties.
     *
     * <p>This method calls through to {@link #addRasterLayer(QName, String, String, Map, Catalog)}
     */
    public void addRasterLayer(QName qName, String filename, String extension, Catalog catalog)
            throws IOException {
        addRasterLayer(qName, filename, extension, new HashMap(), catalog);
    }

    /**
     * Adds a raster layer to the setup.
     *
     * <p>The <tt>filename</tt> parameter defines the raster file to be loaded from the classpath.
     * This method assumes the scope of this class and calls through to {@link
     * #addRasterLayer(QName, String, String, Map, Class, Catalog)}
     */
    public void addRasterLayer(
            QName qName,
            String filename,
            String extension,
            Map<LayerProperty, Object> props,
            Catalog catalog)
            throws IOException {
        addRasterLayer(qName, filename, extension, props, getClass(), catalog);
    }

    /**
     * Adds a raster layer to the setup.
     *
     * <p>This method configures a raster layer with the name <code>qName.getLocalPart()</code>. A
     * coverage store is created (if it doesn't already exist) with the same name. The workspace of
     * the resulting store and layer is determined by <code>qName.getPrefix()</code>.
     *
     * <p>The <tt>filename</tt> parameter defines the raster file to be loaded from the classpath
     * and copied into the data directory. The <tt>scope</tt> is used as the class from which to
     * load the file from.
     *
     * <p>In the case of adding a zipped archive that contains multiple file the <tt>filename</tt>
     * paramter should have a ".zip" extension and the <tt>extension</tt> parameter must define the
     * extension of the main raster file. The parameter is not necessary and may be null if the
     * <tt>filename</tt> does not refer to a zip file.
     *
     * <p>The <tt>props</tt> parameter is used to define custom properties for the layer. See the
     * {@link LayerProperty} class for supported properties.
     *
     * @param qName The name of the raster layer.
     * @param filename The name of the file containing the raster, to be loaded from the classpath.
     * @param extension The file extension (without a ".") of the main raster file. This parameter
     *     my be <code>null</code> only if <tt>filename</tt> does not refer to a zip file.
     * @param props Custom properties to assign to the created raster layer.
     * @param scope The class from which to load the <tt>filename</tt> resource from.
     */
    public void addRasterLayer(
            QName qName,
            String filename,
            String extension,
            Map<LayerProperty, Object> props,
            Class scope,
            Catalog catalog)
            throws IOException {

        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();

        // setup the data
        File dir = new File(data, name);
        FileUtils.deleteQuietly(dir);
        dir.mkdirs();

        File file = new File(dir, filename);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        catalog.getResourceLoader().copyFromClassPath(filename, file, scope);

        String ext = FilenameUtils.getExtension(filename);
        if ("zip".equalsIgnoreCase(ext)) {

            // unpack the archive
            IOUtils.decompress(file, dir);

            // delete archive
            file.delete();

            if (extension == null) {
                // zip with no extension, we just the directory as the file
                file = dir;
            } else {
                // files may have been top level, or one directory level deep
                file = new File(dir, FilenameUtils.getBaseName(filename) + "." + extension);
                if (!file.exists()) {
                    File file2 = new File(new File(dir, dir.getName()), file.getName());
                    if (file2.exists()) {
                        file = file2;
                    }
                }
            }

            if (!file.exists()) {
                throw new FileNotFoundException(file.getPath());
            }
        }

        // load the format/reader
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        if (format == null) {
            throw new RuntimeException("No format for " + file.getCanonicalPath());
        }
        GridCoverage2DReader reader = null;
        try {
            reader = format.getReader(file);
            if (reader == null) {
                throw new RuntimeException(
                        "No reader for "
                                + file.getCanonicalPath()
                                + " with format "
                                + format.getName());
            }

            // configure workspace if it doesn;t already exist
            if (catalog.getWorkspaceByName(prefix) == null) {
                addWorkspace(prefix, qName.getNamespaceURI(), catalog);
            }
            // create the store
            CoverageStoreInfo store = catalog.getCoverageStoreByName(prefix, name);
            if (store == null) {
                store = catalog.getFactory().createCoverageStore();
            }

            store.setName(name);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);
            store.setURL(URLs.fileToUrl(file).toString());
            store.setType(format.getName());

            if (store.getId() == null) {
                catalog.add(store);
            } else {
                catalog.save(store);
            }

            // create the coverage
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(store);

            final String coverageNames[] = reader.getGridCoverageNames();
            if (reader instanceof StructuredGridCoverage2DReader
                    && coverageNames != null
                    && coverageNames.length > 1) {
                for (String coverageName : coverageNames) {
                    addCoverage(
                            store,
                            builder,
                            reader,
                            catalog,
                            format,
                            coverageName,
                            new QName(qName.getPrefix(), coverageName),
                            props,
                            coverageName);
                }
            } else {
                addCoverage(store, builder, reader, catalog, format, name, qName, props, null);
            }
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    private void addCoverage(
            CoverageStoreInfo store,
            CatalogBuilder builder,
            GridCoverage2DReader reader,
            Catalog catalog,
            AbstractGridFormat format,
            String name,
            QName qName,
            Map<LayerProperty, Object> props,
            String coverageName)
            throws IOException {
        CoverageInfo coverage = null;
        try {
            coverage = builder.buildCoverage(reader, coverageName, null);
            // coverage read params
            if (format instanceof ImageMosaicFormat) {
                //  make sure we work in immediate mode
                coverage.getParameters()
                        .put(
                                AbstractGridFormat.USE_JAI_IMAGEREAD.getName().getCode(),
                                Boolean.FALSE);
            }

            coverage.setName(name);
            coverage.setTitle(name);
            coverage.setDescription(name);
            coverage.setEnabled(true);

            CoverageInfo cov = catalog.getCoverageByCoverageStore(store, name);
            if (cov == null) {
                catalog.add(coverage);
            } else {
                builder.updateCoverage(cov, coverage);
                catalog.save(cov);
                coverage = cov;
            }

            LayerInfo layer = catalog.getLayerByName(new NameImpl(qName));
            if (layer == null) {
                layer = catalog.getFactory().createLayer();
            }
            layer.setResource(coverage);

            layer.setDefaultStyle(
                    catalog.getStyleByName(LayerProperty.STYLE.get(props, DEFAULT_RASTER_STYLE)));
            layer.setType(PublishedType.RASTER);
            layer.setEnabled(true);

            if (layer.getId() == null) {
                catalog.add(layer);
            } else {
                catalog.save(layer);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Adds a service configuration to the test setup. If the service object already exists it is
     * simply reverted to its original state.
     *
     * @param serviceClass The class of the service
     * @param workspace The optional workspace for the service, may be <code>null</code>
     * @param geoServer The GeoServer configuration object.
     */
    public <T extends ServiceInfo> void addService(
            Class<T> serviceClass, String workspace, GeoServer geoServer) {

        Catalog catalog = geoServer.getCatalog();

        List<XStreamServiceLoader> loaders =
                GeoServerExtensions.extensions(XStreamServiceLoader.class);
        for (XStreamServiceLoader loader : loaders) {
            if (serviceClass.equals(loader.getServiceClass())) {
                // create a new one
                T created = (T) loader.create(geoServer);

                // grab the old one, if it exists
                T old = null;
                WorkspaceInfo ws = null;
                if (workspace != null) {
                    ws = catalog.getWorkspaceByName(workspace);
                    old = geoServer.getService(ws, serviceClass);
                } else {
                    old = geoServer.getService(serviceClass);
                }

                if (old != null) {
                    // update the old copy
                    OwsUtils.copy(created, old, serviceClass);
                    geoServer.save(old);
                } else {
                    // add the new one
                    created.setWorkspace(ws);
                    geoServer.add(created);
                }

                break;
            }
        }
    }

    /**
     * Adds a settings configuration to the test setup. If the settings object already exists it is
     * simply reverted to its original state.
     *
     * @param workspace The optional workspace for the settings, may be <code>null</code>
     * @param geoServer The GeoServer configuration object.
     */
    public void addSettings(String workspace, GeoServer geoServer) {
        WorkspaceInfo ws =
                workspace != null ? geoServer.getCatalog().getWorkspaceByName(workspace) : null;

        GeoServerInfo global = geoServer.getGlobal();
        SettingsInfo settings = ws != null ? geoServer.getSettings(ws) : global.getSettings();
        if (settings == null) {
            settings = geoServer.getFactory().createSettings();
        }
        settings.setWorkspace(ws);
        settings.getContact().setContactPerson("Andrea Aime");
        settings.getContact().setContactEmail("andrea@geoserver.org");
        settings.getContact()
                .setAddressDeliveryPoint(
                        "1600 Pennsylvania Ave NW, Washington DC 20500, United States");
        settings.setNumDecimals(8);
        settings.setOnlineResource("http://geoserver.org");
        settings.setVerbose(false);
        settings.setVerboseExceptions(false);
        settings.setLocalWorkspaceIncludesPrefix(false);

        if (ws != null) {
            if (settings.getId() != null) {
                geoServer.save(settings);
            } else {
                geoServer.add(settings);
            }
        } else {
            // global
            geoServer.save(global);
        }
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

    @Override
    public void tearDown() throws Exception {
        int MAX_ATTEMPTS = 100;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            try {
                deleteFilesOnExit(data);
                break;
            } catch (IOException e) {
                if (i == MAX_ATTEMPTS && data.exists()) {
                    throw new IOException(
                            "Failed to clean up test data dir after " + MAX_ATTEMPTS + " attempts",
                            e);
                }
                System.err.println(
                        "Error occurred while removing files. "
                                + "Possible transient lock or H2 log race. "
                                + "Sleeping 100ms and retrying. Error message: "
                                + e.getMessage());
                System.gc();
                Thread.sleep(100);
            }
        }
    }

    private void deleteFilesOnExit(File directory) throws IOException {
        try {
            FileUtils.deleteDirectory(data);
        } catch (IOException e) {
            if (!data.exists()) {
                // gone some other way? good...
            } else {
                String tree = printFileTree(data);
                throw new IOException("Failed to delete tree:\n" + tree, e);
            }
        }
    }

    private static String printFileTree(File dir) {
        StringBuilder sb = new StringBuilder();
        sb.append(dir.getPath()).append("\n");
        printFileTree_(sb, "", dir);
        return sb.toString();
    }

    private static void printFileTree_(StringBuilder sb, String prefix, File dir) {
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                boolean last = i == listFile.length - 1;
                File file = listFile[i];
                String firstChar = last ? "└" : "├";
                sb.append(prefix)
                        .append(firstChar)
                        .append("──")
                        .append(file.getName())
                        .append("\n");
                if (file.isDirectory()) {
                    printFileTree_(sb, prefix + (last ? " " : "|") + "  ", file);
                }
            }
        }
    }

    @Override
    public File getDataDirectoryRoot() {
        return data;
    }

    @Override
    public boolean isTestDataAvailable() {
        return true;
    }
}

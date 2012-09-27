package org.geoserver.data.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerPersister;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

/**
 * Test setup uses for GeoServer system tests.
 * <p>
 * This is the default test setup used by {@link GeoServerSystemTestSupport}. During setup this 
 * class creates a full GeoServer data directory configuration on disk. 
 * </p>
 * <p>
 * Customizing the setup, adding layers, etc... is done from 
 * {@link GeoServerSystemTestSupport#setUpTestData}. 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class SystemTestData extends CiteTestData {

    /**
     * Keys for overriding default layer properties
     */
    public static class LayerProperty<T> {

        T get(Map<LayerProperty,Object> map, T def) {
            return map.containsKey(this) ? (T) map.get(this) : def;
        }

        public static LayerProperty<String> NAME = new LayerProperty<String>(); 
        public static LayerProperty<ProjectionPolicy> PROJECTION_POLICY = new LayerProperty<ProjectionPolicy>();
        public static LayerProperty<String> STYLE = new LayerProperty<String>();
        public static LayerProperty<ReferencedEnvelope> ENVELOPE = new LayerProperty<ReferencedEnvelope>();
        public static LayerProperty<ReferencedEnvelope> LATLON_ENVELOPE = new LayerProperty<ReferencedEnvelope>();
        public static LayerProperty<Integer> SRS = new LayerProperty<Integer>();
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

    @Override
    public void setUp() throws Exception {
        createCatalog();
        createConfig();
    }

    /**
     * Sets up the default set of layers, which is all the vector layers whose names are included
     * in the {@link CiteTestData#TYPENAMES} array.
     */
    public void setUpDefaultLayers() throws IOException {
        for (QName layerName : TYPENAMES) {
            addVectorLayer(layerName, catalog);
        }
    }

    /**
     * Sets up the default set of raster layers. 
     * <p>
     * Layer names included in this set include:
     * <ul>
     *  <li>{@link CiteTestData#TASMANIA_BM}
     *  <li>{@link CiteTestData#TASMANIA_DEM}
     *  <li>{@link CiteTestData#ROTATED_CAD}
     *  <li>{@link CiteTestData#WORLD}
     * </ul>
     * </p>
     */
    public void setUpDefaultRasterLayers() throws IOException {
        addWorkspace(WCS_PREFIX, WCS_URI, catalog);
        addDefaultRasterLayer(TASMANIA_DEM, catalog);
        addDefaultRasterLayer(TASMANIA_BM, catalog);
        addDefaultRasterLayer(ROTATED_CAD, catalog);
        addDefaultRasterLayer(WORLD, catalog);
    }

    public void setUpWcs10RasterLayers() throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Sets up the WCS 11 raster layers.
     * <p>
     * This method is a synonym for {@link #setUpDefaultLayers()}
     * </p>
     */
    public void setUpWcs11RasterLayers() throws IOException {
        setUpDefaultRasterLayers();
    }

    protected void createCatalog() throws IOException {
        CatalogImpl catalog = new CatalogImpl();
        catalog.setExtendedValidation(false);
        catalog.setResourceLoader(new GeoServerResourceLoader(data));
        
        catalog.addListener(new GeoServerPersister(catalog.getResourceLoader(), 
            createXStreamPersister()));

        //workspaces
        addWorkspace(DEFAULT_PREFIX, DEFAULT_URI, catalog);
        addWorkspace(SF_PREFIX, SF_URI, catalog);
        addWorkspace(CITE_PREFIX, CITE_URI, catalog);
        addWorkspace(CDF_PREFIX, CDF_URI, catalog);
        addWorkspace(CGF_PREFIX, CGF_URI, catalog);

        //default style
        addStyle(DEFAULT_VECTOR_STYLE, catalog);
        addStyle(DEFAULT_RASTER_STYLE, catalog);

        this.catalog = catalog;
    }

    protected void createConfig() {
        GeoServerImpl geoServer = new GeoServerImpl();
        geoServer.addListener(new GeoServerPersister(new GeoServerResourceLoader(data), 
            createXStreamPersister()));

        GeoServerInfo global = geoServer.getFactory().createGlobal();
        global.getSettings().getContact().setContactPerson("Andrea Aime");
        global.getSettings().setNumDecimals(8);
        global.getSettings().setOnlineResource("http://geoserver.org");
        global.getSettings().setVerbose(false);
        geoServer.setGlobal(global);

        LoggingInfo logging = geoServer.getFactory().createLogging();
        geoServer.setLogging(logging);
    }

    XStreamPersister createXStreamPersister() {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        xp.setEncryptPasswordFields(false);
        xp.setVerbose(false);
        return xp;
    }

    /**
     * Adds a workspace to the test setup.
     * 
     * @param name The name of the workspace.
     * @param uri The namespace uri associated with the workspace.
     */
    public void addWorkspace(String name, String uri, Catalog catalog) {
        
        WorkspaceInfo ws = catalog.getWorkspaceByName(name);
        if (ws == null) {
            ws = catalog.getFactory().createWorkspace();
            ws.setName(name);
            catalog.add(ws);
        }

        NamespaceInfo ns =  catalog.getNamespaceByPrefix(name);
        if (ns == null) {
            ns = catalog.getFactory().createNamespace();
            ns.setPrefix(name);
            ns.setURI(uri);
            catalog.add(ns);
        }
        else {
            ns.setURI(uri);
            catalog.save(ns);
        }
    }

    /**
     * Adds a style to the test setup.
     * <p>
     * To set up the style a file named <tt>name</tt>.sld is copied from the classpath relative
     * to this class.
     * </p>
     * @param name The name of the style.
     */
    public void addStyle(String name, Catalog catalog) throws IOException {
        addStyle(name, getClass(), catalog);
    }

    /**
     * Adds a style to the test setup.
     * <p>
     * To set up the style a file named <tt>name</tt>.sld is copied from the classpath relative
     * to the <tt>scope</tt> parameter.
     * </p>
     * @param name The name of the style.
     * @param scope Class from which to load sld resource from.
     */
    public void addStyle(String name, Class scope, Catalog catalog) throws IOException {
        File styles = catalog.getResourceLoader().findOrCreateDirectory(data, "styles");

        String filename = name + ".sld";
        catalog.getResourceLoader().copyFromClassPath(filename, new File(styles, filename), scope);

        StyleInfo style = catalog. getStyleByName(name);
        if (style == null) {
            style = catalog.getFactory().createStyle();
            style.setName(name);
        }
        style.setFilename(filename);
        if (style.getId() == null) {
            catalog.add(style);
        }
        else {
            catalog.save(style);
        }
    }

    /**
     * Adds a vector layer to the catalog setup.
     * <p>
     * This method calls through to {@link #addVectorLayer(QName, Map, Catalog)} with no custom 
     * properties.
     * </p>
     */
    public void addVectorLayer(QName qName, Catalog catalog) throws IOException {
        addVectorLayer(qName, new HashMap(), catalog);
    }

    /**
     * Adds a vector layer to the catalog setup.
     * <p>
     * This method calls through to {@link #addVectorLayer(QName, Map, Class, Catalog)} passing in
     * this class as the scope.
     * </p> 
     */
    public void addVectorLayer(QName qName, Map<LayerProperty,Object> props, Catalog catalog) 
        throws IOException {
        addVectorLayer(qName, props, getClass(), catalog);
    }

    /**
     * Adds a vector layer to the catalog setup.
     * <p>
     * The layer is created within a store named <code>qName.getPrefix()</code>, creating it 
     * if it does not exist. The resulting store is a {@link PropertyDataStore} that points at the 
     * directory <code>getDataDirectoryRoot()/qName.getPrefix()</code>. Similarily the layer and
     * store are created within a workspace named <code>qName.getPrefix()</code>, which is created
     * if it does not already exist.
     * </p>
     * <p>
     * The properties data for the layer is copied from the classpath, with a file name of 
     * "<code>qName.getLocalPart()</code>.properties". The <tt>scope</tt> parameter is used as the 
     * class from which to load the properties file relative to. 
     * </p>
     * <p>
     * The <tt>props</tt> parameter is used to define custom properties for the layer. See the 
     * {@link LayerProperty} class for supported properties. 
     * </p>
     */
    public void addVectorLayer(QName qName, Map<LayerProperty,Object> props, Class scope, 
        Catalog catalog) throws IOException {
        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();
        String uri = qName.getNamespaceURI();

        //configure workspace if it doesn;t already exist
        if (catalog.getWorkspaceByName(prefix) == null) {
            addWorkspace(prefix, uri, catalog);
        }
        
        //configure store if it doesn't already exist

        File storeDir = catalog.getResourceLoader().findOrCreateDirectory(prefix);

        DataStoreInfo store = catalog.getDataStoreByName(prefix);
        if (store == null) {
            store = catalog.getFactory().createDataStore();
            store.setName(prefix);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);

            store.getConnectionParameters().put(PropertyDataStoreFactory.DIRECTORY.key, storeDir);
            store.getConnectionParameters().put(PropertyDataStoreFactory.NAMESPACE.key, uri);
            catalog.add(store);
        }

        //copy the properties file over
        String filename = name + ".properties";
        catalog.getResourceLoader().copyFromClassPath(filename, new File(storeDir, filename), scope);

        //configure feature type
        FeatureTypeInfo featureType = catalog.getFactory().createFeatureType();
        featureType.setStore(store);
        featureType.setNamespace(catalog.getNamespaceByPrefix(prefix));
        featureType.setName(LayerProperty.NAME.get(props, name));
        featureType.setNativeName(name);
        featureType.setTitle(name);
        featureType.setAbstract("abstract about " + name);

        Integer srs = LayerProperty.SRS.get(props, SRS.get(qName));
        if ( srs == null ) {
            srs = 4326;
        }
        featureType.setSRS("EPSG:" + srs);
        try {
            featureType.setNativeCRS(CRS.decode("EPSG:" + srs));
        } catch (Exception e) {
            throw new IOException(e);
        }
        featureType.setNumDecimals(8);
        featureType.getKeywords().add(new Keyword(name));
        featureType.setEnabled(true);
        featureType.setProjectionPolicy(LayerProperty.PROJECTION_POLICY.get(props, ProjectionPolicy.NONE));
        featureType.setLatLonBoundingBox(LayerProperty.LATLON_ENVELOPE.get(props, DEFAULT_LATLON_ENVELOPE));
        featureType.setNativeBoundingBox(LayerProperty.ENVELOPE.get(props, null));

        FeatureTypeInfo ft = catalog.getFeatureTypeByDataStore(store, name);
        if (ft == null) {
            ft = featureType;
            catalog.add(featureType);
        }
        else {
            new CatalogBuilder(catalog).updateFeatureType(ft, featureType);
            catalog.save(ft);
        }

        LayerInfo layer = catalog.getLayerByName(new NameImpl(prefix, name));
        if (layer == null) {
            layer = catalog.getFactory().createLayer();    
        }

        layer.setResource(ft);

        StyleInfo defaultStyle = null;
        if (props.containsKey(LayerProperty.STYLE)) {
            defaultStyle = catalog.getStyleByName(LayerProperty.STYLE.get(props, null));
        }
        else {
            //look for a style matching the layer name
            defaultStyle = catalog.getStyleByName(name);
            if (defaultStyle == null) {
                //see if the resource exists and we just need to create it
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
        layer.setType(LayerInfo.Type.VECTOR);
        layer.setEnabled(true);

        if (layer.getId() == null) {
            catalog.add(layer);
        }
        else {
            catalog.save(layer);
        }
    }

    /**
     * Adds one of the default raster layers.
     * <p>
     *  The <tt>name</tt> parameter must be one of:
     *  <ul>
     *  <li>{@link CiteTestData#TASMANIA_BM}
     *  <li>{@link CiteTestData#TASMANIA_DEM}
     *  <li>{@link CiteTestData#ROTATED_CAD}
     *  <li>{@link CiteTestData#WORLD}
     * </ul>
     * </p>
     */
    public void addDefaultRasterLayer(QName name, Catalog catalog) throws IOException {
        if (name.equals(TASMANIA_DEM)) {
            addRasterLayer(name,  "tazdem.tiff", null, catalog);
        }
        else if (name.equals(TASMANIA_BM)) {
            addRasterLayer(name, "tazbm.tiff", null, catalog);
        }
        else if (name.equals(ROTATED_CAD)) {
            addRasterLayer(name, "rotated.tiff", null, catalog);
        }
        else if (name.equals(WORLD)) {
            addRasterLayer(name, "world.tiff", null, catalog);
        }
        else {
            throw new IllegalArgumentException("Unknown default raster layer: " + name);
        }
    }

    /**
     * Adds a raster layer to the setup with no custom properties.
     * <p>
     * This method calls through to {@link #addRasterLayer(QName, String, String, Map, Catalog)}
     * </p> 
     */
    public void addRasterLayer(QName qName, String filename, String extension, Catalog catalog) 
        throws IOException {
        addRasterLayer(qName, filename, extension, new HashMap(), catalog);
    }

    /**
     * Adds a raster layer to the setup.
     * <p>
     * The <tt>filename</tt> parameter defines the raster file to be loaded from the classpath.
     * This method assumes the scope of this class and calls through to 
     * {@link #addRasterLayer(QName, String, String, Map, Class, Catalog)}
     * </p>
     */
    public void addRasterLayer(QName qName, String filename, String extension, 
        Map<LayerProperty,Object> props, Catalog catalog) throws IOException {
        addRasterLayer(qName, filename, extension, props, getClass(), catalog);
    }

    /**
     * Adds a raster layer to the setup.
     * <p>
     * This method configures a raster layer with the name <code>qName.getLocalPart()</code>. A 
     * coverage store is created (if it doesn't already exist) with the same name. The workspace
     * of the resulting store and layer is determined by <code>qName.getPrefix()</code>.
     * </p>
     * <p>
     * The <tt>filename</tt> parameter defines the raster file to be loaded from the classpath 
     * and copied into the data directory. The <tt>scope</tt> is used as the class from which to 
     * load the file from.
     * </p>
     * <p>
     * In the case of adding a zipped archive that contains multiple file the <tt>filename</tt> 
     * paramter should have a ".zip" extension and the <tt>extension</tt> parameter must define the 
     * extension of the main raster file. The parameter is not necessary and may be null if the 
     * <tt>filename</tt> does not refer to a zip file.
     * </p>
     * <p>
     * The <tt>props</tt> parameter is used to define custom properties for the layer. See the 
     * {@link LayerProperty} class for supported properties. 
     * </p>
     * @param qName The name of the raster layer.
     * @param filename The name of the file containing the raster, to be loaded from the classpath.
     * @param extension The file extension (without a ".") of the main raster file. This parameter
     *   my be <code>null</code> only if <tt>filename</tt> does not refer to a zip file.
     * @param props Custom properties to assign to the created raster layer.
     * @param scope The class from which to load the <tt>filename</tt> resource from. 
     *
     */
    public void addRasterLayer(QName qName, String filename, String extension, 
        Map<LayerProperty,Object> props, Class scope, Catalog catalog) throws IOException {

        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();

        //setup the data
        File dir = new File(data, name);
        dir.mkdirs();

        File file = new File(dir, filename);
        catalog.getResourceLoader().copyFromClassPath(filename, file, getClass());

        String ext = FilenameUtils.getExtension(filename);
        if ("zip".equalsIgnoreCase(ext)) {
            if (extension == null) {
                throw new IllegalArgumentException("Raster data specified as archive but no " + 
                    "extension of coverage was specified");
            }

            //unpack the archive
            IOUtils.decompress(file, dir);

            //delete archive
            file.delete();

            file = new File(dir, FilenameUtils.getBaseName(filename) + "." + ext);
            if (!file.exists()) {
                throw new FileNotFoundException(file.getPath());
            }
        }

        //load the format/reader
        AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(file);
        if (format == null) {
            throw new RuntimeException("No format for " + file.getCanonicalPath());
        }
        AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) format.getReader(file);
        if (reader == null) {
            throw new RuntimeException("No reader for " + file.getCanonicalPath() + " with format " + format.getName());
        }

        //create the store
        CoverageStoreInfo store = catalog.getCoverageStoreByName(prefix, name);
        if (store == null) {
            store = catalog.getFactory().createCoverageStore();
            store.setName(name);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);
            store.setURL(DataUtilities.fileToURL(file).toString());
            store.setType(format.getName());
            catalog.add(store);
        }

        //create the coverage
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(store);

        CoverageInfo coverage = null;
        
        try {
            coverage = builder.buildCoverage(reader, null);
        } catch (Exception e) {
            throw new IOException(e);
        }

        coverage.setName(name);
        coverage.setTitle(name);
        coverage.setDescription(name);
        coverage.setEnabled(true);

        CoverageInfo cov = catalog.getCoverageByCoverageStore(store, name);
        if (cov == null) {
            catalog.add(coverage);
        }
        else {
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
        layer.setType(LayerInfo.Type.RASTER);
        layer.setEnabled(true);

        if (layer.getId() == null) {
            catalog.add(layer);
        }
        else {
            catalog.save(layer);
        }
    }

    @Override
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(data);
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

/* (c) 2014-2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.eclipse.imagen.PlanarImage;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.util.ReaderUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.PostGISTestResource;
import org.geoserver.test.RunTestSetup;
import org.geoserver.test.SystemTest;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataAccessFinder;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureLocking;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.Mark;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDirectoryFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.image.util.ImageUtilities;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.URLs;
import org.geotools.util.Version;
import org.geotools.util.factory.FactoryRegistry;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.locationtech.jts.geom.Point;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Tests for {@link ResourcePool}.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
@Category(SystemTest.class)
public class ResourcePoolTest extends GeoServerSystemTestSupport {

    @ClassRule
    public static final PostGISTestResource postgis = new PostGISTestResource();

    private static final String SQLVIEW_DATASTORE = "sqlviews";

    private static final String VT_NAME = "pgeo_view";

    private static final String HUMANS = "humans";

    private static final String BAD_CONN_DATASTORE = "bad_conn_data_store";
    public static final DataStoreFactorySpi TEST_DIRECTORY_STORE_FACTORY_SPI = new TestDirectoryStoreFactorySpi();

    static {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
    }

    private static File rockFillSymbolFile;

    protected static QName TIMERANGES = new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);

    private static final String EXTERNAL_ENTITIES = "externalEntities";

    @BeforeClass
    public static void registerTestDirectoryStore() {
        // a "catch-all" datastore that will use any File without requiring a filetype/dbtype
        DataStoreFinder.registerFactory(TEST_DIRECTORY_STORE_FACTORY_SPI);
    }

    @AfterClass
    public static void deregisterTestDirectoryStore() {
        DataStoreFinder.deregisterFactory(TEST_DIRECTORY_STORE_FACTORY_SPI);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle("relative", "se_relativepath.sld", ResourcePoolTest.class, catalog);
        testData.addStyle("relative_protocol", "se_relativepath_protocol.sld", ResourcePoolTest.class, catalog);
        testData.addStyle(HUMANS, "humans.sld", ResourcePoolTest.class, catalog);
        testData.addStyle(EXTERNAL_ENTITIES, "externalEntities.sld", TestData.class, catalog);
        StyleInfo style = catalog.getStyleByName("relative");
        style.setFormatVersion(new Version("1.1.0"));
        catalog.save(style);
        style = catalog.getStyleByName(HUMANS);
        style.setFormatVersion(new Version("1.1.0"));
        catalog.save(style);
        File images = new File(testData.getDataDirectoryRoot(), "styles/images");
        assertTrue(images.mkdir());
        File image = new File("./src/test/resources/org/geoserver/catalog/rockFillSymbol.png");
        assertTrue(image.exists());
        FileUtils.copyFileToDirectory(image, images);
        rockFillSymbolFile = new File(images, image.getName()).getCanonicalFile();

        testData.addRasterLayer(TIMERANGES, "timeranges.zip", null, null, SystemTestData.class, catalog);

        FileUtils.copyFileToDirectory(
                new File("./src/test/resources/geoserver-environment.properties"), testData.getDataDirectoryRoot());

        // add the states shapefile with odd CRS definition from prj file (non EPSG)
        try (InputStream is = getClass().getResourceAsStream("mini-states.zip")) {
            File dir = getDataDirectory().get("data/mini-states").dir();
            dir.mkdirs();
            org.geoserver.util.IOUtils.decompress(is, dir);
        }
        CatalogBuilder cb = new CatalogBuilder(catalog);
        DataStoreInfo dataStoreInfo = cb.buildDataStore("mini-states");
        dataStoreInfo.setType("Shapefile");
        dataStoreInfo.getConnectionParameters().put("url", "file:data/mini-states/mini-states.shp");
        catalog.add(dataStoreInfo);
        DataStore ds = (DataStore) dataStoreInfo.getDataStore(null);
        cb.setStore(dataStoreInfo);
        FeatureTypeInfo typeInfo = cb.buildFeatureType(ds.getFeatureSource(ds.getTypeNames()[0]));
        List<AttributeTypeInfo> attributes = catalog.getResourcePool().getAttributes(typeInfo);
        typeInfo.getAttributes().addAll(attributes);
        catalog.add(typeInfo);
        LayerInfo layerInfo = cb.buildLayer(typeInfo);
        catalog.add(layerInfo);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs11RasterLayers();
    }

    /**
     * Test that the {@link FeatureType} cache returns the same instance every time. This is assumed by some nasty code
     * in other places that tampers with the CRS. If a new {@link FeatureType} is constructed for the same
     * {@link FeatureTypeInfo}, Bad Things Happen (TM).
     */
    @Test
    public void testFeatureTypeCacheInstance() throws Exception {
        ResourcePool pool = ResourcePool.create(getCatalog());
        FeatureTypeInfo info =
                getCatalog().getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        FeatureType ft1 = pool.getFeatureType(info);
        FeatureType ft2 = pool.getFeatureType(info);
        FeatureType ft3 = pool.getFeatureType(info);
        assertSame(ft1, ft2);
        assertSame(ft1, ft3);
    }

    @Test
    public void testAttributeCache() throws Exception {
        final Catalog catalog = getCatalog();
        ResourcePool pool = ResourcePool.create(catalog);

        // clean up the lakes type
        FeatureTypeInfo oldInfo =
                catalog.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        List<LayerInfo> layers = catalog.getLayers(oldInfo);
        for (LayerInfo layerInfo : layers) {
            catalog.remove(layerInfo);
        }
        catalog.remove(oldInfo);

        // rebuild as new
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(catalog.getStoreByName(MockData.CITE_PREFIX, MockData.CITE_PREFIX, DataStoreInfo.class));
        FeatureTypeInfo info =
                builder.buildFeatureType(new NameImpl(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart()));

        // non persisted state, caching should not occurr
        List<AttributeTypeInfo> att1 = pool.getAttributes(info);
        List<AttributeTypeInfo> att2 = pool.getAttributes(info);
        assertNotSame(att1, att2);
        assertEquals(att1, att2);

        // save it, making it persistent
        catalog.add(info);

        // first check caching actually works against persisted type infos
        List<AttributeTypeInfo> att3 = pool.getAttributes(info);
        List<AttributeTypeInfo> att4 = pool.getAttributes(info);
        assertSame(att3, att4);
        assertNotSame(att1, att3);
        assertEquals(att1, att3);
    }

    boolean cleared = false;

    @Test
    public void testCacheClearing() throws IOException {
        cleared = false;
        ResourcePool pool = new ResourcePool(getCatalog()) {
            @Override
            public void clear(FeatureTypeInfo info) {
                cleared = true;
                super.clear(info);
            }
        };
        FeatureTypeInfo info =
                getCatalog().getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());

        assertNotNull(pool.getFeatureType(info));
        info.setTitle("changed");

        assertFalse(cleared);
        getCatalog().save(info);
        assertTrue(cleared);

        cleared = false;
        assertNotNull(pool.getFeatureType(info));

        for (LayerInfo l : getCatalog().getLayers(info)) {
            getCatalog().remove(l);
        }
        getCatalog().remove(info);
        assertTrue(cleared);
    }

    boolean disposeCalled;

    /**
     * Make sure {@link ResourcePool#clear(DataStoreInfo)} and {@link ResourcePool#dispose()} call
     * {@link DataAccess#dispose()}
     */
    @Test
    public void testDispose() throws IOException {
        disposeCalled = false;
        class ResourcePool2 extends ResourcePool {
            public ResourcePool2(Catalog catalog) {
                super(catalog);
                dataStoreCache = new DataStoreCache() {
                    @Override
                    protected void dispose(String name, DataAccess dataStore) {
                        disposeCalled = true;
                        super.dispose(name, dataStore);
                    }
                };
            }
        }

        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool2(catalog);
        catalog.setResourcePool(pool);

        DataStoreInfo info = catalog.getDataStores().get(0);
        // force the datastore to be created
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = pool.getDataStore(info);
        assertNotNull(dataStore);
        assertFalse(disposeCalled);
        pool.clear(info);
        assertTrue(disposeCalled);

        // force the datastore to be created
        dataStore = pool.getDataStore(info);
        assertNotNull(dataStore);
        disposeCalled = false;
        pool.dispose();
        assertTrue(disposeCalled);
    }

    @Test
    public void testConfigureFeatureTypeCacheSize() {
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        global.setFeatureTypeCacheSize(200);
        gs.save(global);

        Catalog catalog = getCatalog();
        // we actually keep two versions of the feature type in the cache, so we need it
        // twice as big
        assertEquals(
                400, ((SoftValueHashMap) catalog.getResourcePool().getFeatureTypeCache()).getHardReferencesCount());
    }

    @Test
    public void testDropCoverageStore() throws Exception {
        // build the store
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        CoverageStoreInfo store = cb.buildCoverageStore("dem");
        store.setURL(MockData.class.getResource("tazdem.tiff").toExternalForm());
        store.setType("GeoTIFF");
        cat.add(store);

        // build the coverage
        cb.setStore(store);
        CoverageInfo ci = cb.buildCoverage();
        cat.add(ci);

        // build the layer
        LayerInfo layer = cb.buildLayer(ci);
        cat.add(layer);

        // grab a reader just to inizialize the code
        ci.getGridCoverage(null, null);
        ci.getGridCoverageReader(null, GeoTools.getDefaultHints());

        // now drop the store
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(cat);
        visitor.visit(store);

        // and reload (GEOS-4782 -> BOOM!)
        getGeoServer().reload();
    }

    @Test
    public void testNativeCoverageName() throws Exception { // GEOS-9236
        // build the store
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        CoverageStoreInfo store = cb.buildCoverageStore("geotiff");

        store.setURL(MockData.class
                .getResource("/org/geoserver/data/test/tazdem.tiff")
                .toExternalForm());
        store.setType("GeoTIFF");
        cat.add(store);

        // build the coverage
        cb.setStore(store);
        CoverageInfo ci = cb.buildCoverage();
        ci.setNativeCoverageName("geotiff_coverage");
        cat.add(ci);

        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, GeoTools.getDefaultHints());

        Bounds envelop = reader.getOriginalEnvelope();

        assertNotNull(ci);
        assertTrue(reader.getFormat() instanceof GeoTiffFormat);
        assertNotNull(envelop);
        assertEquals("tazdem", ci.getNativeCoverageName());
    }

    @RunTestSetup
    @Test
    public void testGeoServerReload() throws Exception {
        Catalog cat = getCatalog();
        FeatureTypeInfo lakes =
                cat.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        assertNotEquals("foo", lakes.getTitle());

        GeoServerDataDirectory dd = new GeoServerDataDirectory(getResourceLoader());
        File info = dd.config(lakes).file();
        // File info = getResourceLoader().find("featureTypes", "cite_Lakes", "info.xml");

        try (FileReader in = new FileReader(info)) {
            Element dom = ReaderUtils.parse(in);
            Element title = ReaderUtils.getChildElement(dom, "title");
            title.getFirstChild().setNodeValue("foo");

            try (OutputStream output = new FileOutputStream(info)) {
                TransformerFactory.newInstance()
                        .newTransformer()
                        .transform(new DOMSource(dom), new StreamResult(output));
            }

            getGeoServer().reload();
            lakes = cat.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
            assertEquals("foo", lakes.getTitle());
        }
    }

    @Test
    public void testSEStyleWithRelativePath() throws IOException {
        StyleInfo si = getCatalog().getStyleByName("relative");

        assertNotNull(si);
        Style style = si.getStyle();
        PolygonSymbolizer ps = (PolygonSymbolizer)
                style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        ExternalGraphic eg = (ExternalGraphic)
                ps.getFill().getGraphicFill().graphicalSymbols().get(0);
        URI uri = eg.getOnlineResource().getLinkage();
        assertNotNull(uri);
        File actual = URLs.urlToFile(uri.toURL()).getCanonicalFile();
        assertEquals(rockFillSymbolFile, actual);
    }

    @Test
    public void testSEStyleWithRelativePathProtocol() throws IOException {
        StyleInfo si = getCatalog().getStyleByName("relative_protocol");

        assertNotNull(si);
        Style style = si.getStyle();
        PolygonSymbolizer ps = (PolygonSymbolizer)
                style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        ExternalGraphic eg = (ExternalGraphic)
                ps.getFill().getGraphicFill().graphicalSymbols().get(0);
        URI uri = eg.getOnlineResource().getLinkage();
        assertNotNull(uri);
        File actual = URLs.urlToFile(uri.toURL()).getCanonicalFile();
        assertEquals(rockFillSymbolFile, actual);
    }

    @Test
    public void testPreserveStructuredReader() throws IOException {
        // we have to make sure time ranges native name is set to trigger the bug in question
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(TIMERANGES));
        assertTrue(ci.getGridCoverageReader(null, null) instanceof StructuredGridCoverage2DReader);
        String name = ci.getGridCoverageReader(null, null).getGridCoverageNames()[0];
        ci.setNativeCoverageName(name);
        getCatalog().save(ci);

        ci = getCatalog().getCoverageByName(getLayerId(TIMERANGES));
        assertTrue(ci.getGridCoverageReader(null, null) instanceof StructuredGridCoverage2DReader);
    }

    @Test
    public void testMissingNullValuesInCoverageDimensions() throws IOException {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(MockData.TASMANIA_DEM));
        List<CoverageDimensionInfo> dimensions = ci.getDimensions();
        // legacy layers have no null value list
        dimensions.get(0).getNullValues().clear();
        getCatalog().save(ci);

        // and now go back and ask for the reader
        ci = getCatalog().getCoverageByName(getLayerId(MockData.TASMANIA_DEM));
        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        GridCoverage2D gc = null;
        try {
            // check that we maintain the native info if we don't have any
            gc = (GridCoverage2D) reader.read();
            assertEquals(-9999d, CoverageUtilities.getNoDataProperty(gc).getAsSingleValue(), 0d);
        } finally {
            if (gc != null) {
                RenderedImage ri = gc.getRenderedImage();
                if (gc instanceof GridCoverage2D) {
                    gc.dispose(true);
                }
                if (ri instanceof PlanarImage image) {
                    ImageUtilities.disposePlanarImageChain(image);
                }
            }
        }
    }

    @RunTestSetup
    @Test
    public void testEnvParametrizationValues() throws Exception {

        final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

        DataStoreInfo ds = getCatalog().getFactory().createDataStore();
        ds.getConnectionParameters().put("host", "${jdbc.host}");
        ds.getConnectionParameters().put("port", "${jdbc.port}");

        try {
            final String dsName = "GS-ENV-TEST-DS";
            ds.setName(dsName);

            getCatalog().save(ds);

            ds = getCatalog().getDataStoreByName(dsName);

            DataStoreInfo expandedDs = getCatalog().getResourcePool().clone(ds, true);

            assertEquals("${jdbc.host}", ds.getConnectionParameters().get("host"));
            assertEquals("${jdbc.port}", ds.getConnectionParameters().get("port"));

            if (GeoServerEnvironment.allowEnvParametrization()) {
                assertEquals(
                        expandedDs.getConnectionParameters().get("host"), gsEnvironment.resolveValue("${jdbc.host}"));
                assertEquals(
                        expandedDs.getConnectionParameters().get("port"), gsEnvironment.resolveValue("${jdbc.port}"));
            } else {
                assertEquals(
                        "${jdbc.host}", expandedDs.getConnectionParameters().get("host"));
                assertEquals(
                        "${jdbc.port}", expandedDs.getConnectionParameters().get("port"));
            }
        } finally {
            getCatalog().remove(ds);
        }
    }

    @Test
    public void testCloneStoreInfo() throws Exception {
        Catalog catalog = getCatalog();

        DataStoreInfo source1 = catalog.getDataStores().get(0);
        DataStoreInfo clonedDs = catalog.getResourcePool().clone(source1, false);
        assertNotNull(source1);
        assertNotNull(clonedDs);

        assertEquals(source1, clonedDs);

        CoverageStoreInfo source2 = catalog.getCoverageStores().get(0);
        CoverageStoreInfo clonedCs = catalog.getResourcePool().clone(source2, false);
        assertNotNull(source2);
        assertNotNull(clonedCs);

        assertEquals(source2, clonedCs);
    }

    @Test
    public void testAddFilePathWithSpaces() throws Exception {
        // Other tests mess with or reset the resourcePool, so lets make it is initialised properly
        GeoServerExtensions.extensions(ResourcePoolInitializer.class).get(0).initialize(getGeoServer());

        ResourcePool rp = getCatalog().getResourcePool();

        CoverageStoreInfo info = getCatalog().getFactory().createCoverageStore();
        info.setName("spaces");
        info.setType("ImagePyramid");
        info.setEnabled(true);
        info.setURL("file://./src/test/resources/data_dir/nested_layer_groups/data/pyramid with space");
        try {
            rp.getGridCoverageReader(info, null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            fail("Unable to add an imagepyramid with a space in it's name");
        }
        rp.dispose();
    }

    @Test
    public void testWmsCascadeEntityExpansion() throws Exception {
        // Other tests mess with or reset the resourcePool, so lets make it is initialized properly
        GeoServerExtensions.extensions(ResourcePoolInitializer.class).get(0).initialize(getGeoServer());

        ResourcePool rp = getCatalog().getResourcePool();

        WMSStoreInfo info = getCatalog().getFactory().createWebMapServer();
        ((WMSStoreInfoImpl) info).setId(UUID.randomUUID().toString());
        URL url = getClass().getResource("1.3.0Capabilities-xxe.xml");
        info.setCapabilitiesURL(url.toExternalForm());
        info.setEnabled(true);
        // the connection pooling client does not support file references, disable it
        info.setUseConnectionPooling(false);
        try {
            rp.getWebMapServer(info);
            fail("WebMapServer instantiation should fail");
        } catch (IOException e) {
            assertThat(e.getCause(), instanceOf(ServiceException.class));
            ServiceException serviceException = (ServiceException) e.getCause();
            assertThat(serviceException.getMessage(), containsString("Error while parsing XML"));

            SAXException saxException = (SAXException) serviceException.getCause();
            Exception cause = saxException.getException();
            assertFalse("Expect external entity cause", cause != null && cause instanceof FileNotFoundException);
        }
        // make sure clearing the catalog does not clear the EntityResolver
        getGeoServer().reload();
        rp = getCatalog().getResourcePool();

        try {
            rp.getWebMapServer(info);
            fail("WebMapServer instantiation should fail");
        } catch (IOException e) {
            assertThat(e.getCause(), instanceOf(ServiceException.class));
            ServiceException serviceException = (ServiceException) e.getCause();
            assertThat(serviceException.getMessage(), containsString("Error while parsing XML"));

            SAXException saxException = (SAXException) serviceException.getCause();
            Exception cause = saxException.getException();
            assertFalse("Expect external entity cause", cause != null && cause instanceof FileNotFoundException);
        }
    }

    @Test
    public void testWfsCascadeEntityExpansion() throws Exception {
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        DataStoreInfo ds = cb.buildDataStore("wfs-xxe");
        URL url = getClass().getResource("wfs1.1.0Capabilities-xxe.xml");
        ds.getConnectionParameters().put(WFSDataStoreFactory.URL.key, url);
        // required or the store won't fetch caps from a file
        ds.getConnectionParameters().put("TESTING", Boolean.TRUE);
        final ResourcePool rp = getCatalog().getResourcePool();
        try {
            rp.getDataStore(ds);
            fail("Store creation should have failed to to XXE attack");
        } catch (Exception e) {
            String message = e.getMessage();
            assertThat(message, containsString("Entity resolution disallowed"));
            assertThat(message, containsString("file:///file/not/there"));
        }
    }

    @Test
    public void testStyleWithExternalEntities() throws Exception {
        StyleInfo si = getCatalog().getStyleByName(EXTERNAL_ENTITIES);
        try {
            si.getStyle();
            fail("Should have failed with a parse error");
        } catch (Exception e) {
            String message = e.getMessage();
            assertThat(message, containsString("Entity resolution disallowed"));
            assertThat(message, containsString("/this/file/does/not/exist"));
        }
    }

    /**
     * Triggers an exception to check that the file in data catalog is deleted. The exception occurs because the format
     * is set to null.
     *
     * @throws Exception
     */
    @Test
    public void testWriteStyleThatFails() throws Exception {
        StyleInfo style = getCatalog().getFactory().createStyle();
        style.setName("foo");
        style.setFilename("foo.sld");
        style.setFormat(null);
        ((StyleInfoImpl) style).setId(UUID.randomUUID().toString());
        File sldFile = new File(getTestData().getDataDirectoryRoot().getAbsolutePath(), "styles/foo.sld");

        final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();

        try {
            ResourcePool pool = new ResourcePool(getCatalog());
            pool.writeSLD(style, sld);
            Assert.fail("Should fail with IOException");
        } catch (IOException e) {
            // writeStyleFile throws an IOException
        }
        Assert.assertFalse("foo.sld should not exist on disk after a failure.", sldFile.exists());
    }

    /**
     * Since GEOS-10743 readStyle() must throw a FileNotFoundException if the style file does not exist. (Before an
     * empty file was returned.)
     *
     * @throws IOException
     */
    @Test(expected = FileNotFoundException.class)
    public void testMissingStyleThrowsException() throws IOException {
        Catalog catalog = getCatalog();
        StyleInfo missing = catalog.getFactory().createStyle();
        missing.setName("missing");
        missing.setFilename("missing.sld");

        ResourcePool pool = new ResourcePool(catalog);
        try (BufferedReader ignored = pool.readStyle(missing)) {
            fail("FileNotFoundException expected for missing style files");
        }
    }

    @Test
    public void testParseExternalMark() throws Exception {
        StyleInfo si = getCatalog().getStyleByName(HUMANS);
        // used to blow here with an NPE
        Style s = si.getStyle();
        s.accept(new AbstractStyleVisitor() {
            @Override
            public void visit(Mark mark) {
                assertEquals(
                        "ttf://Webdings",
                        mark.getExternalMark().getOnlineResource().getLinkage().toASCIIString());
            }
        });
    }

    /** This checks that the resource pool does NOT wrap the FeatureSource so that it prevents locking */
    @Test
    public void testSourceIsNotIncorrectlyWrappedAndCanLock() throws IOException {
        Catalog cat = getCatalog();
        ResourcePool resourcePool = cat.getResourcePool();

        FeatureTypeInfo featureTypeInfo = cat.getFeatureTypeByName("Lines");
        featureTypeInfo.getAttributes().addAll(featureTypeInfo.attributes());
        FeatureSource source = resourcePool.getFeatureSource(featureTypeInfo, null);

        assertTrue(source instanceof SimpleFeatureLocking);
    }

    @Test
    public void testDataStoreScan() throws Exception {
        final Catalog catalog = getCatalog();

        // prepare a store that supports sql views
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName(SQLVIEW_DATASTORE);
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        ds.setWorkspace(ws);
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.putAll(postgis.getConnectionParameters());
        cat.add(ds);

        SimpleFeatureSource fsp = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init(fsp.getSchema());
        tb.remove("surfaceProperty"); // the store cannot create multi-geom tables it seems
        tb.remove("curveProperty"); // the store cannot create multi-geom tables it seems
        tb.remove("uriProperty"); // this would render the store read only
        tb.setName("pgeo");
        SimpleFeatureType schema = tb.buildFeatureType();
        store.createSchema(schema);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) store.getFeatureSource("pgeo");
        featureStore.addFeatures(fsp.getFeatures());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);
        FeatureTypeInfo tft = cb.buildFeatureType(featureStore);
        cat.add(tft);

        // create the sql view
        JDBCDataStore jds = (JDBCDataStore) ds.getDataStore(null);
        VirtualTable vt = new VirtualTable(
                VT_NAME,
                "select \"name\", \"pointProperty\" from \"pgeo\" where \"booleanProperty\" = %bool% and \"name\" = '%name%'");
        vt.addParameter(new VirtualTableParameter("bool", "true"));
        vt.addParameter(new VirtualTableParameter("name", "name-f001"));
        vt.addGeometryMetadatata("pointProperty", Point.class, 4326);
        jds.createVirtualTable(vt);

        FeatureTypeInfo vft = cb.buildFeatureType(jds.getFeatureSource(vt.getName()));
        vft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
        cat.add(vft);

        AtomicInteger counter = new AtomicInteger();
        ResourcePool testPool = new ResourcePool() {

            /*
             * This is the method making the expensive call to the data store (especially if the store is an Oracle one without a schema specified).
             * Make sure it's not being called unless the feature type is really not cacheable.
             */
            @Override
            protected Name getTemporaryName(
                    FeatureTypeInfo info,
                    DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
                    FeatureTypeCallback initializer)
                    throws IOException {
                if (VT_NAME.equals(info.getNativeName())) {
                    counter.incrementAndGet();
                }
                return super.getTemporaryName(info, dataAccess, initializer);
            }
        };
        testPool.setCatalog(catalog);
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(VT_NAME);
        testPool.getFeatureSource(ft, null);
        assertEquals(0, counter.get());
        // now try with a dirty feature type, the call should be made
        ft.setName("foobar");
        testPool.getFeatureSource(ft, null);
        assertThat(counter.get(), greaterThan(0));
    }

    @Test
    public void testRepositoryHints() throws Exception {
        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool(catalog) {
            // cannot clone the mock objects
            @Override
            public CoverageStoreInfo clone(CoverageStoreInfo source, boolean allowEnvParametrization) {
                return source;
            }
        };

        // setup all the mocks
        final String url = "http://www.geoserver.org/mock/format";
        AbstractGridCoverage2DReader reader = createNiceMock("theReader", AbstractGridCoverage2DReader.class);
        replay(reader);
        AbstractGridFormat format = createNiceMock("theFormat", AbstractGridFormat.class);
        Capture<Hints> capturedHints = Capture.newInstance(CaptureType.LAST);
        expect(format.getReader(EasyMock.eq(url), capture(capturedHints)))
                .andReturn(reader)
                .anyTimes();
        replay(format);
        CoverageStoreInfo storeInfo = createNiceMock("storeInfo", CoverageStoreInfo.class);
        expect(storeInfo.getURL()).andReturn(url).anyTimes();
        expect(storeInfo.getFormat()).andReturn(format).anyTimes();
        replay(storeInfo);

        // pass no hints
        GridCoverageReader returnedReader = pool.getGridCoverageReader(storeInfo, null);
        assertThat(reader, equalTo(returnedReader));
        final Hints hints1 = capturedHints.getValue();
        assertThat(hints1, notNullValue());
        assertThat(hints1, hasEntry(Hints.REPOSITORY, pool.repository));

        // pass some hints
        capturedHints.reset();
        GridCoverageReader returnedReader2 =
                pool.getGridCoverageReader(storeInfo, new Hints(Hints.KEY_ANTIALIASING, Hints.VALUE_ANTIALIAS_ON));
        assertThat(reader, equalTo(returnedReader2));
        final Hints hints2 = capturedHints.getValue();
        assertThat(hints2, notNullValue());
        assertThat(hints2, hasEntry(Hints.REPOSITORY, pool.repository));
        assertThat(hints2, hasEntry(Hints.KEY_ANTIALIASING, Hints.VALUE_ANTIALIAS_ON));
    }

    @Test
    public void testGetParamsFixesDatabaseFilePath() {
        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool(catalog);
        DataStoreInfo ds = getCatalog().getFactory().createDataStore();
        ds.getConnectionParameters().put("database", "file:data/test.gpkg");
        Map newParams = pool.getParams(ds.getConnectionParameters(), getResourceLoader());
        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(getResourceLoader());
        String absolutePath = dataDir.get("data/test.gpkg").dir().getAbsolutePath();
        assertNotEquals(newParams.get("database"), "file:data/test.gpkg");
        assertTrue(((String) newParams.get("database")).contains(absolutePath));
    }

    @Test
    public void testGetParamsFixesCSVFilePath() {
        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool(catalog);
        DataStoreInfo ds = getCatalog().getFactory().createDataStore();
        ds.getConnectionParameters().put("file", "file:data/locations.csv");
        Map newParams = pool.getParams(ds.getConnectionParameters(), getResourceLoader());
        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(getResourceLoader());
        String absolutePath = dataDir.get("data/locations.csv").file().getAbsolutePath();
        assertNotEquals(newParams.get("file"), "file:locations.csv");
        assertTrue(((String) newParams.get("file")).contains(absolutePath));
    }

    @Test
    public void testEmptySort() throws IOException, IllegalAccessException {
        SimpleFeatureSource fsp = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);
        Query q = new Query();
        q.setSortBy(SortBy.UNSORTED);
        SimpleFeatureCollection fc = fsp.getFeatures(q);

        // the above call used to add a SortedSimpleFeatureCollection wrapper that in turn killed
        // some visit optimizations
        while (fc instanceof DecoratingFeatureCollection) {
            assertThat(fc, not(instanceOf(SortedSimpleFeatureCollection.class)));
            Field field = FieldUtils.getDeclaredField(SortedSimpleFeatureCollection.class, "delegate");
            field.setAccessible(true);
            Object delegate = field.get(fc);
            fc = (SimpleFeatureCollection) delegate;
        }
    }

    @Test
    public void testDefaultGeometry() throws IOException {
        FeatureTypeInfo featureType = getCatalog().getResourceByName("cdf", "Nulls", FeatureTypeInfo.class);
        GeometryDescriptor schemaDefaultGeometry = featureType.getFeatureType().getGeometryDescriptor();

        try (FeatureIterator i =
                featureType.getFeatureSource(null, null).getFeatures().features()) {
            GeometryDescriptor featureDefaultGeometry =
                    i.next().getDefaultGeometryProperty().getDescriptor();

            assertNotNull(schemaDefaultGeometry);
            assertNotNull(featureDefaultGeometry);
            assertEquals("pointProperty", schemaDefaultGeometry.getLocalName());
            assertEquals(schemaDefaultGeometry, featureDefaultGeometry);
        }
    }

    /**
     * Tests the ability for the ResourcePool to convert input objects for getting a specific GridCoverageReader using
     * the CoverageReaderInputObjectConverter extension point.
     *
     * @throws IOException
     */
    @Test
    public void testCoverageReaderInputConverter() throws IOException {
        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool(catalog);

        CoverageStoreInfo info = catalog.getFactory().createCoverageStore();
        info.setType("ImagePyramid");
        info.setURL("file://./src/test/resources/data_dir/nested_layer_groups/data/pyramid%20with%20space");

        GridCoverageReader reader = pool.getGridCoverageReader(info, null);

        // the CoverageReaderFileConverter should have successfully converted the URL string to a
        // File object
        assertTrue(reader.getSource() instanceof File);
    }

    /**
     * Tests that even if the input string cannot be parsed as a valid URI reference, but the it is nontheless a valid
     * file URL, the CoverageReaderFileConverter is able to do the conversion.
     *
     * @throws IOException
     */
    @Test
    public void testCoverageReaderInputConverterInvalidURI() throws IOException {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        String fileURL = MockData.class.getResource("tazdem.tiff").getFile().replace("/", "\\");
        fileURL = "file://" + fileURL;
        // the file URL is not now a valid URI but the converter should be able to convert it
        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool(catalog);

        CoverageStoreInfo info = catalog.getFactory().createCoverageStore();
        info.setType("GeoTIFF");
        info.setURL(fileURL);

        GridCoverageReader reader = pool.getGridCoverageReader(info, null);
        // the CoverageReaderFileConverter should have successfully converted the URL string to a
        // File object
        assertTrue(reader.getSource() instanceof File);
    }

    /**
     * Tests that even if the input string is a valid file path, the CoverageReaderFileConverter is able to do the
     * conversion.
     *
     * @throws IOException
     */
    @Test
    public void testCoverageReaderInputConverterFilePath() throws IOException {
        String fileURL = MockData.class.getResource("tazdem.tiff").getFile();
        // the file URL is not now a valid URI but the converter should be able to convert it
        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool(catalog);

        CoverageStoreInfo info = catalog.getFactory().createCoverageStore();
        info.setType("GeoTIFF");
        info.setURL(fileURL);

        GridCoverageReader reader = pool.getGridCoverageReader(info, null);
        // the CoverageReaderFileConverter should have successfully converted the URL string to a
        // File object
        assertTrue(reader.getSource() instanceof File);
    }

    @Test
    public void testGetDataStoreConcurrency() throws Exception {
        ResourcePool pool = null;
        List<ResourcePoolLatchedThread<DataStoreInfo, DataAccess>> threads = null;
        try {
            final Catalog catalog = getCatalog();
            DataStoreInfo ds = storeInfo(catalog, "concurrencyTest1");
            pool = new ResourcePool(getCatalog());

            ResourcePoolLatchedThread.PoolBiFunction<DataStoreInfo, DataAccess> function =
                    (resPool, info) -> resPool.getDataStore(info);
            CountDownLatch taskLatch = new CountDownLatch(1);
            int numberOfThreads = 5;
            CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);
            threads = getLatchedThreads(taskLatch, completeLatch, numberOfThreads, pool, ds, function);
            threads.forEach(t -> t.start());
            taskLatch.countDown();
            completeLatch.await(60, TimeUnit.SECONDS);

            DataAccess dA = pool.dataStoreCache.get(ds.getId());
            assertNotNull(dA);
            for (ResourcePoolLatchedThread<DataStoreInfo, DataAccess> t : threads) {
                assertSame(dA, t.getResult());
                assertTrue(t.getErrors().isEmpty());
            }
        } finally {
            if (pool != null) pool.dispose();
            killThreads(threads);
        }
    }

    @Test
    public void testNonBlockingThreadOnGetStore() throws Exception {
        // test that if a thread is accessing a store in the cache
        // and is blocked, other threads can retrieve other instances from the cache.
        ResourcePool resPool = null;
        ResourcePoolLatchedThread<DataStoreInfo, DataAccess> latchedThread1 = null;
        ResourcePoolLatchedThread<DataStoreInfo, DataAccess> latchedThread2 = null;
        try {
            final Catalog catalog = getCatalog();
            DataStoreInfo ds = storeInfo(catalog, "concurrencyTest2");
            DataStoreInfo ds2 = storeInfo(catalog, "concurrencyTest3");
            CountDownLatch taskLatch = new CountDownLatch(1);
            CountDownLatch taskLatch2 = new CountDownLatch(1);
            resPool = new ResourcePool(catalog) {
                @Override
                protected DataAccess<? extends FeatureType, ? extends Feature> createDataAccess(
                        DataStoreInfo info, DataStoreInfo expandedStore) throws IOException {
                    if (ds.getId().equalsIgnoreCase(info.getId())) {
                        Thread thread = Thread.currentThread();
                        synchronized (thread) {
                            try {
                                // lets the other thread start and try retrieve
                                // another store.
                                taskLatch2.countDown();
                                thread.wait(60 * 1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    return super.createDataAccess(info, expandedStore);
                }
            };
            ResourcePoolLatchedThread.PoolBiFunction<DataStoreInfo, DataAccess> function =
                    (pool, info) -> pool.getDataStore(info);
            CountDownLatch completeLatch = new CountDownLatch(2);
            latchedThread1 = new ResourcePoolLatchedThread<>(taskLatch, completeLatch, resPool, ds, function);
            latchedThread2 = new ResourcePoolLatchedThread<>(taskLatch2, completeLatch, resPool, ds2, function);
            latchedThread1.start();
            latchedThread2.start();
            // let's start just the first thread
            // when it will arrive at store creation time will unblock
            // the second thread.
            taskLatch.countDown();
            latchedThread2.join(60 * 1000);
            // thread2 completed and retrieved the data Access.
            DataAccess access = resPool.dataStoreCache.get(ds2.getId());
            assertSame(access, latchedThread2.getResult());
            assertNull(latchedThread1.getResult());

            synchronized (latchedThread1) {
                latchedThread1.notify();
            }
            completeLatch.await(60, TimeUnit.SECONDS);

            DataAccess dA = resPool.dataStoreCache.get(ds.getId());
            assertNotNull(dA);
            assertSame(dA, latchedThread1.getResult());
        } finally {
            if (resPool != null) resPool.dispose();
            killThread(latchedThread1);
            killThread(latchedThread2);
        }
    }

    @Test
    public void testConcurrencyOnFeatureTypeCache() throws Exception {
        ResourcePool pool = null;
        List<ResourcePoolLatchedThread<FeatureTypeInfo, FeatureType>> threads = null;
        try {
            pool = ResourcePool.create(getCatalog());
            FeatureTypeInfo info =
                    getCatalog().getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
            CountDownLatch taskLatch = new CountDownLatch(1);
            int numberOfThreads = 5;
            CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);
            ResourcePoolLatchedThread.PoolBiFunction<FeatureTypeInfo, FeatureType> function =
                    (rl, fti) -> rl.getFeatureType(info, false);
            threads = getLatchedThreads(taskLatch, completeLatch, numberOfThreads, pool, info, function);
            threads.forEach(t -> t.start());
            taskLatch.countDown();
            completeLatch.await(60, TimeUnit.SECONDS);
            FeatureType featureType = pool.getFeatureType(info, false);
            for (ResourcePoolLatchedThread<FeatureTypeInfo, FeatureType> t : threads) {
                assertSame(featureType, t.getResult());
                assertTrue(t.getErrors().isEmpty());
            }
        } finally {
            if (pool != null) pool.dispose();
            killThreads(threads);
        }
    }

    @Test
    public void testConcurrencyOnCRSCache() throws Exception {
        ResourcePool pool = null;
        List<ResourcePoolLatchedThread<String, CoordinateReferenceSystem>> threads = null;
        try {
            pool = ResourcePool.create(getCatalog());
            String srs = "EPSG:4326";
            CountDownLatch taskLatch = new CountDownLatch(1);
            int numberOfThreads = 5;
            CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);
            ResourcePoolLatchedThread.PoolBiFunction<String, CoordinateReferenceSystem> function =
                    (rl, srsName) -> rl.getCRS(srsName);
            threads = getLatchedThreads(taskLatch, completeLatch, numberOfThreads, pool, srs, function);
            threads.forEach(t -> t.start());
            taskLatch.countDown();
            completeLatch.await(60, TimeUnit.SECONDS);
            CoordinateReferenceSystem crs = pool.getCrsCache().get(srs);
            for (ResourcePoolLatchedThread<String, CoordinateReferenceSystem> t : threads) {
                assertSame(crs, t.getResult());
                assertTrue(t.getErrors().isEmpty());
            }
        } finally {
            if (pool != null) pool.dispose();
            killThreads(threads);
        }
    }

    @Test
    public void testConcurrencyOnStyleCache() throws Exception {
        ResourcePool pool = null;
        List<ResourcePoolLatchedThread<StyleInfo, Style>> threads = null;
        try {
            pool = ResourcePool.create(getCatalog());
            StyleInfo info = getCatalog().getStyleByName(HUMANS);
            CountDownLatch taskLatch = new CountDownLatch(1);
            int numberOfThreads = 5;
            CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);
            ResourcePoolLatchedThread.PoolBiFunction<StyleInfo, Style> function =
                    (rl, styleInfo) -> rl.getStyle(styleInfo);
            threads = getLatchedThreads(taskLatch, completeLatch, numberOfThreads, pool, info, function);
            threads.forEach(t -> t.start());
            taskLatch.countDown();
            completeLatch.await();
            Style style = pool.getStyleCache().get(info.getId());
            for (ResourcePoolLatchedThread<StyleInfo, Style> t : threads) {
                assertSame(style, t.getResult());
                assertTrue(t.getErrors().isEmpty());
            }
        } finally {
            if (pool != null) pool.dispose();
            killThreads(threads);
        }
    }

    @Test
    public void testConcurrencyOnSLDCache() throws Exception {
        ResourcePool pool = null;
        List<ResourcePoolLatchedThread<StyleInfo, StyledLayerDescriptor>> threads = null;
        try {
            pool = ResourcePool.create(getCatalog());
            StyleInfo info = getCatalog().getStyleByName(HUMANS);
            CountDownLatch taskLatch = new CountDownLatch(1);
            int numberOfThreads = 5;
            CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);
            ResourcePoolLatchedThread.PoolBiFunction<StyleInfo, StyledLayerDescriptor> function =
                    (rl, styleInfo) -> rl.getSld(styleInfo);
            threads = getLatchedThreads(taskLatch, completeLatch, numberOfThreads, pool, info, function);
            threads.forEach(t -> t.start());
            taskLatch.countDown();
            completeLatch.await();
            StyledLayerDescriptor sld = pool.getSldCache().get(info.getId());
            for (ResourcePoolLatchedThread<StyleInfo, StyledLayerDescriptor> t : threads) {
                assertSame(sld, t.getResult());
                assertTrue(t.getErrors().isEmpty());
            }
        } finally {
            if (pool != null) pool.dispose();
            killThreads(threads);
        }
    }

    @Test
    public void testConcurrencyOnFeatureTypeAttrsCache() throws Exception {
        ResourcePool pool = null;
        List<ResourcePoolLatchedThread<FeatureTypeInfo, List<AttributeTypeInfo>>> threads = null;
        try {
            pool = ResourcePool.create(getCatalog());
            FeatureTypeInfo info =
                    getCatalog().getFeatureTypeByName(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
            CountDownLatch taskLatch = new CountDownLatch(1);
            int numberOfThreads = 5;
            CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);
            ResourcePoolLatchedThread.PoolBiFunction<FeatureTypeInfo, List<AttributeTypeInfo>> function =
                    (rl, fti) -> rl.getAttributes(info);
            threads = getLatchedThreads(taskLatch, completeLatch, numberOfThreads, pool, info, function);
            threads.forEach(t -> t.start());
            taskLatch.countDown();
            completeLatch.await(60, TimeUnit.SECONDS);
            List<AttributeTypeInfo> list = pool.getAttributes(info);
            for (ResourcePoolLatchedThread<FeatureTypeInfo, List<AttributeTypeInfo>> t : threads) {
                assertSame(list, t.getResult());
                assertTrue(t.getErrors().isEmpty());
            }
        } finally {
            if (pool != null) pool.dispose();
            killThreads(threads);
        }
    }

    private DataStoreInfo storeInfo(Catalog catalog, String name) {
        // prepare a store that supports sql views
        DataStoreInfoImpl ds = new DataStoreInfoImpl(catalog);
        ds.setId(UUID.randomUUID().toString());
        ds.setName(name);
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        ds.setWorkspace(ws);
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.putAll(postgis.getConnectionParameters());
        catalog.add(ds);
        return ds;
    }

    private <P, R> List<ResourcePoolLatchedThread<P, R>> getLatchedThreads(
            CountDownLatch taskLatch,
            CountDownLatch completeLatch,
            int numberOfThreads,
            ResourcePool resourcePool,
            P funParam,
            ResourcePoolLatchedThread.PoolBiFunction<P, R> function) {
        List<ResourcePoolLatchedThread<P, R>> threads = new ArrayList<>(numberOfThreads);
        int i = 0;
        while (i < numberOfThreads) {
            threads.add(new ResourcePoolLatchedThread<>(taskLatch, completeLatch, resourcePool, funParam, function));
            i++;
        }
        return threads;
    }

    private void killThreads(List threads) {
        if (threads != null && !threads.isEmpty()) {
            for (Object thread : threads) {
                if (thread instanceof Thread thread1) {
                    killThread(thread1);
                }
            }
        }
    }

    private void killThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    @Test
    public void testAutodisableOnConnfailure() {

        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName(BAD_CONN_DATASTORE);
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        ds.setWorkspace(ws);
        ds.setEnabled(true);
        ds.setDisableOnConnFailure(true);
        ds.setType("PostGIS");
        Map<String, Serializable> params = ds.getConnectionParameters();
        params.putAll(postgis.getConnectionParameters());
        // Make the connection invalid to test error handling
        params.put("database", "nonexistent_database");
        cat.add(ds);

        DataStoreInfo dsi = cat.getDataStoreByName(ds.getName());
        assertTrue(dsi.isEnabled());

        ResourcePool resourcePool = ResourcePool.create(cat);
        DataAccess<?, ?> access = null;
        try {
            access = resourcePool.getDataStore(dsi);
        } catch (IOException e) {

        }
        assertNull(access);
        DataStoreInfo storeInfo = cat.getDataStoreByName(dsi.getName());
        assertFalse(storeInfo.isEnabled());
    }

    @Test
    public void testWmsCascadeAutoDisable() throws Exception {
        GeoServerExtensions.extensions(ResourcePoolInitializer.class).get(0).initialize(getGeoServer());

        ResourcePool rp = getCatalog().getResourcePool();

        WMSStoreInfo info = getCatalog().getFactory().createWebMapServer();
        info.setName("TestAutoDisableWMSStore");
        URL url = getClass().getResource("1.3.0Capabilities-xxe.xml");
        info.setCapabilitiesURL(url.toExternalForm());
        info.setEnabled(true);
        info.setDisableOnConnFailure(true);
        info.setUseConnectionPooling(false);
        getCatalog().add(info);
        WMSStoreInfo wmsStore = getCatalog().getWMSStoreByName(info.getName());
        assertTrue(wmsStore.isEnabled());
        try {
            rp.getWebMapServer(wmsStore);
        } catch (IOException e) {
            wmsStore = getCatalog().getWMSStoreByName(info.getName());
        }
        assertFalse(wmsStore.isEnabled());
    }

    @Test
    public void testWmtsCascadeAutoDisable() throws Exception {
        GeoServerExtensions.extensions(ResourcePoolInitializer.class).get(0).initialize(getGeoServer());

        ResourcePool rp = getCatalog().getResourcePool();

        WMTSStoreInfo info = getCatalog().getFactory().createWebMapTileServer();
        info.setName("TestAutoDisableWMTSStore");
        URL url = getClass().getResource("1.3.0Capabilities-xxe.xml");
        info.setCapabilitiesURL(url.toExternalForm());
        info.setEnabled(true);
        info.setDisableOnConnFailure(true);
        info.setUseConnectionPooling(false);
        getCatalog().add(info);
        WMTSStoreInfo wmtsStore = getCatalog().getWMTSStoreByName(info.getName());
        assertTrue(wmtsStore.isEnabled());
        try {
            rp.getWebMapTileServer(wmtsStore);
        } catch (IOException e) {
            wmtsStore = getCatalog().getWMTSStoreByName(info.getName());
        }
        assertFalse(wmtsStore.isEnabled());
    }

    @Test
    public void testCoverageStoreInfoAutodisable() throws Exception {
        GeoServerExtensions.extensions(ResourcePoolInitializer.class).get(0).initialize(getGeoServer());

        ResourcePool rp = getCatalog().getResourcePool();

        CoverageStoreInfo info = getCatalog().getFactory().createCoverageStore();
        info.setName("TestCoverageAutoDisable");
        info.setType("ImagePyramid");
        info.setEnabled(true);
        info.setDisableOnConnFailure(true);
        info.setURL("file://./src/test/resources/not-existing.tif");
        getCatalog().add(info);
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName(info.getName());
        assertTrue(storeInfo.isEnabled());
        try {
            rp.getGridCoverageReader(storeInfo, null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            storeInfo = getCatalog().getCoverageStoreByName(info.getName());
        }
        assertFalse(storeInfo.isEnabled());
        rp.dispose();
    }

    @Test
    public void testEPSGLookup() throws Exception {
        // UTM 32 North, without EPSG codes
        String wkt =
                """
                PROJCS["WGS 84 / UTM zone 32N",
                    GEOGCS["WGS 84",
                        DATUM["WGS_1984",
                            SPHEROID["WGS 84",6378137,298.257223563,
                                AUTHORITY["EPSG","7030"]]],
                        PRIMEM["Greenwich",0,
                            AUTHORITY["EPSG","8901"]],
                        UNIT["degree",0.0174532925199433,
                            AUTHORITY["EPSG","9122"]]],
                    PROJECTION["Transverse_Mercator"],
                    PARAMETER["latitude_of_origin",0],
                    PARAMETER["central_meridian",9],
                    PARAMETER["scale_factor",0.9996],
                    PARAMETER["false_easting",500000],
                    PARAMETER["false_northing",0],
                    UNIT["metre",1,
                        AUTHORITY["EPSG","9001"]],
                    AXIS["Easting",EAST],
                    AXIS["Northing",NORTH]]""";
        CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
        assertEquals("EPSG:32632", ResourcePool.lookupIdentifier(crs, true));
    }

    @Test
    public void testIAULookup() throws Exception {
        // Sun CRS, without authority and code
        String wkt =
                """
                GEOGCS["Sun (2015) - Sphere / Ocentric",
                    DATUM["Sun (2015) - Sphere",
                        SPHEROID["Sun (2015) - Sphere",695700000,0,
                            AUTHORITY["IAU","1000"]],
                        AUTHORITY["IAU","1000"]],
                    PRIMEM["Reference Meridian",0,
                        AUTHORITY["IAU","1000"]],
                    UNIT["degree",0.0174532925199433,
                        AUTHORITY["EPSG","9122"]]]""";
        CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
        assertEquals("IAU:1000", ResourcePool.lookupIdentifier(crs, true));
    }

    @Test
    public void testCustomizeAttributesCRS() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("mini-states");
        SimpleFeatureType schema = (SimpleFeatureType) fti.getFeatureType();
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        assertEquals(CRS.decode("EPSG:4326", true), crs);
    }

    @Test
    public void testAcceptAllStore() throws Exception {
        // check we have both the shapefile directory and test store accepting URL without a dbtype
        ShapefileDirectoryFactory shapeDirectorFactory = null;
        TestDirectoryStoreFactorySpi testDirectoryFactory = null;
        Iterator<DataStoreFactorySpi> factoryIterator = DataStoreFinder.getAllDataStores();
        while (factoryIterator.hasNext()) {
            DataStoreFactorySpi spi = factoryIterator.next();
            if (spi instanceof TestDirectoryStoreFactorySpi factorySpi) testDirectoryFactory = factorySpi;
            else if (spi instanceof ShapefileDirectoryFactory factory) shapeDirectorFactory = factory;
        }
        assertNotNull(shapeDirectorFactory);
        assertNotNull(testDirectoryFactory);

        // Using reflection to grab the registry. The synchronization is there because the method
        // is using assertions to check it's being called in a synchronized block (assertions
        // are enabled when running tests with Maven)
        FactoryRegistry registry;
        synchronized (DataAccessFinder.class) {
            registry = ReflectionTestUtils.invokeMethod(DataStoreFinder.class, "getServiceRegistry");
        }
        registry.setOrdering(DataStoreFactorySpi.class, testDirectoryFactory, shapeDirectorFactory);

        // now create a store that would be caught by the test factory, if it wasn't for the type
        // being used to lookup the shape factory
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        DataStoreInfo dataStoreInfo = cb.buildDataStore("mini-states-dir");
        dataStoreInfo.setType("Directory of spatial files (shapefiles)");
        dataStoreInfo.getConnectionParameters().put("url", "file:data/mini-states");
        catalog.add(dataStoreInfo);

        // check this is the right type of store
        DataStore ds = (DataStore) dataStoreInfo.getDataStore(null);
        assertThat(ds, Matchers.instanceOf(DirectoryDataStore.class));
    }
}

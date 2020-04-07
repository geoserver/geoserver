/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeatureType;

/** @author Alessio Fabiani, GeoSolutions */
public class BackupRestoreTestSupport extends GeoServerSystemTestSupport {

    protected static Catalog catalog;

    protected static XpathEngine xp;

    protected static Backup backupFacade;

    static File root;

    protected GeoServerDataDirectory createDataDirectoryMock() {
        GeoServerDataDirectory dd = createNiceMock(GeoServerDataDirectory.class);
        expect(dd.root()).andReturn(root).anyTimes();
        return dd;
    }

    public static final Set<String> DEFAULT_STYLEs =
            new HashSet<String>() {
                {
                    add(StyleInfo.DEFAULT_POINT);
                    add(StyleInfo.DEFAULT_LINE);
                    add(StyleInfo.DEFAULT_GENERIC);
                    add(StyleInfo.DEFAULT_POLYGON);
                    add(StyleInfo.DEFAULT_RASTER);
                }
            };

    @Before
    public void beforeTest() throws InterruptedException {
        // reset invocations counter of continuable handler
        ContinuableHandler.resetInvocationsCount();
        // reset invocation of generic listener
        GenericListener.reset();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        /** Dispose Services */
        this.testData = new SystemTestData();

        cleanCatalog();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();

        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");

        testData.registerNamespaces(namespaces);
        CiteTestData.registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        backupFacade = (Backup) applicationContext.getBean("backupFacade");

        catalog = getCatalog();

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("global");
        lg.getLayers().add(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getLayers().add(catalog.getLayerByName("sf:AggregateGeoFeature"));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        lg = catalog.getFactory().createLayerGroup();
        lg.setName("local");
        lg.setWorkspace(catalog.getWorkspaceByName("sf"));
        lg.getLayers().add(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getLayers().add(catalog.getLayerByName("sf:AggregateGeoFeature"));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        // add two workspace specific styles
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("sf_style");
        s.setWorkspace(catalog.getWorkspaceByName("sf"));
        s.setFilename("sf.sld");
        catalog.add(s);

        s = catalog.getFactory().createStyle();
        s.setName("cite_style");
        s.setWorkspace(catalog.getWorkspaceByName("cite"));
        s.setFilename("cite.sld");
        catalog.add(s);

        setUpInternal(testData);
    }

    protected void setUpInternal(SystemTestData data) throws Exception {
        root = File.createTempFile("template", "tmp", new File("target"));
        root.delete();
        root.mkdir();

        // setup an H2 datastore for the purpose of doing joins
        // run all the tests against a store that can do native paging (h2) and one that
        // can't (property)
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(catalog.getDefaultWorkspace());

        Map params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/foo");
        catalog.add(ds);

        FeatureSource fs1 = getFeatureSource(SystemTestData.FORESTS);
        FeatureSource fs2 = getFeatureSource(SystemTestData.LAKES);
        FeatureSource fs3 = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init((SimpleFeatureType) fs1.getSchema());
        // tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());

        tb.init((SimpleFeatureType) fs2.getSchema());
        // tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());

        tb.init((SimpleFeatureType) fs3.getSchema());
        tb.remove("surfaceProperty");
        tb.remove("curveProperty");
        tb.remove("uriProperty");
        store.createSchema(tb.buildFeatureType());

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(ds);

        FeatureStore fs = (FeatureStore) store.getFeatureSource("Forests");
        fs.addFeatures(fs1.getFeatures());
        addFeature(
                fs,
                "MULTIPOLYGON (((0.008151604330777 -0.0023208963631571, 0.0086527358638763 -0.0012374917185382, 0.0097553137885805 -0.0004505798694767, 0.0156132468328575 0.001226912691216, 0.0164282119026783 0.0012863836826631, 0.0171241513076058 0.0011195104764988, 0.0181763809803841 0.0003258121477801, 0.018663180519973 -0.0007914339515293, 0.0187 -0.0054, 0.0185427596344991 -0.0062643098258021, 0.0178950534559435 -0.0072336706251426, 0.0166538015456463 -0.0078538015456464, 0.0160336706251426 -0.0090950534559435, 0.0150643098258021 -0.0097427596344991, 0.0142 -0.0099, 0.0086 -0.0099, 0.0077356901741979 -0.0097427596344991, 0.0067663293748574 -0.0090950534559435, 0.0062572403655009 -0.0082643098258021, 0.0061 -0.0074, 0.0061055767515099 -0.0046945371967831, 0.0062818025956546 -0.0038730531083409, 0.0066527358638763 -0.0032374917185382, 0.0072813143786463 -0.0026800146279973, 0.008151604330777 -0.0023208963631571)))",
                "110",
                "Foo Forest");
        addFeature(
                fs,
                "MULTIPOLYGON (((-0.0023852705061082 -0.005664537521815, -0.0026781637249217 -0.0063716443030016, -0.0033852705061082 -0.006664537521815, -0.0040923772872948 -0.0063716443030016, -0.0043852705061082 -0.005664537521815, -0.0040923772872947 -0.0049574307406285, -0.0033852705061082 -0.004664537521815, -0.0026781637249217 -0.0049574307406285, -0.0023852705061082 -0.005664537521815)))",
                "111",
                "Bar Forest");
        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        catalog.add(ft);

        fs = (FeatureStore) store.getFeatureSource("Lakes");
        fs.addFeatures(fs2.getFeatures());
        addFeature(
                fs,
                "POLYGON ((0.0049784771992108 -0.0035817570010558, 0.0046394552911414 -0.0030781256232061, 0.0046513167019495 -0.0024837722339832, 0.0051238379318686 -0.0011179833712748, 0.0057730295670053 -0.0006191988155468, 0.0065631962428717 -0.0022312008226987, 0.0065546368796182 -0.0027977724434409, 0.0060815583363558 -0.0033764140395305, 0.0049784771992108 -0.0035817570010558))",
                "102",
                "Red Lake");
        addFeature(
                fs,
                "POLYGON ((0.0057191452206184 -0.0077928768384869, 0.0051345315543621 -0.0076850644756826, 0.0046394552911414 -0.0070781256232061, 0.0046513167019495 -0.0064837722339832, 0.0051238379318686 -0.0051179833712748, 0.0054994549090862 -0.0047342895334108, 0.0070636636030018 -0.0041582580884052, 0.0078667798947931 -0.0042156264760765, 0.0082944271909999 -0.0046527864045, 0.0089944271909999 -0.0060527864045, 0.0090938616646936 -0.0066106299753791, 0.0089805097233498 -0.0069740280868118, 0.0084059445811345 -0.007452049322921, 0.0057191452206184 -0.0077928768384869))",
                "103",
                "Green Lake");
        addFeature(
                fs,
                "POLYGON ((0.0007938800267961 -0.0056175636045986, 0.0011573084862925 -0.0051229419555271, 0.0017412204815544 -0.0049337922722299, 0.0023617041415903 -0.0050976945961703, 0.0029728059060882 -0.0055503031602247, 0.0034289873678372 -0.0063805324543033, 0.0035801692478343 -0.0074485059825999, 0.0034823709081135 -0.008013559804892, 0.0032473247836666 -0.008318888359415, 0.0029142821960289 -0.0085126790755088, 0.0023413406005588 -0.0085369332611115, 0.0011766812981572 -0.0078593563537122, 0.0006397573417165 -0.0067622385244755, 0.0007938800267961 -0.0056175636045986))",
                "110",
                "Black Lake");
        ft = cb.buildFeatureType(fs);
        catalog.add(ft);

        fs = (FeatureStore) store.getFeatureSource("PrimitiveGeoFeature");
        fs.addFeatures(fs3.getFeatures());
        ft = cb.buildFeatureType(fs);
        catalog.add(ft);

        tb = new SimpleFeatureTypeBuilder();
        tb.setName("TimeFeature");
        tb.add("name", String.class);
        tb.add("dateTime", Date.class);

        SimpleFeatureType timeFeatureType = tb.buildFeatureType();
        store.createSchema(timeFeatureType);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, null);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(timeFeatureType);
        fb.add("one");
        fb.add(dateFormat.parseObject("2006-04-04 22:00:00"));
        features.add(fb.buildFeature(null));

        fb.add("two");
        fb.add(dateFormat.parseObject("2006-05-05 20:00:00"));
        features.add(fb.buildFeature(null));

        fb.add("three");
        fb.add(dateFormat.parseObject("2006-06-28 18:00:00"));
        features.add(fb.buildFeature(null));

        fs = (FeatureStore) store.getFeatureSource("TimeFeature");
        fs.addFeatures(features);
        ft = cb.buildFeatureType(fs);
        catalog.add(ft);

        // add three joinable types with same code, but different type names
        SimpleFeatureType ft1 =
                DataUtilities.createType(
                        SystemTestData.CITE_URI, "t1", "g1:Point:srid=4326,code1:int,name1:String");
        store.createSchema(ft1);
        fs = (FeatureStore) store.getFeatureSource("t1");
        addFeature(fs, "POINT(1 1)", Integer.valueOf(1), "First");
        ft = cb.buildFeatureType(fs);
        catalog.add(ft);

        SimpleFeatureType ft2 =
                DataUtilities.createType(
                        SystemTestData.CITE_URI, "t2", "g2:Point:srid=4326,code2:int,name2:String");
        store.createSchema(ft2);
        fs = (FeatureStore) store.getFeatureSource("t2");
        addFeature(fs, "POINT(2 2)", Integer.valueOf(1), "Second");
        ft = cb.buildFeatureType(fs);
        catalog.add(ft);

        SimpleFeatureType ft3 =
                DataUtilities.createType(
                        SystemTestData.CITE_URI, "t3", "g3:Point:srid=4326,code3:int,name3:String");
        store.createSchema(ft3);
        fs = (FeatureStore) store.getFeatureSource("t3");
        addFeature(fs, "POINT(3 3)", Integer.valueOf(1), "Third");
        ft = cb.buildFeatureType(fs);
        catalog.add(ft);

        DataStoreInfo peDatastore = catalog.getFactory().createDataStore();
        peDatastore.setName("foo_pe");
        peDatastore.setWorkspace(catalog.getDefaultWorkspace());

        Map pedsParams = peDatastore.getConnectionParameters();
        pedsParams.put("dbtype", "h2");
        pedsParams.put(
                "database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/foo_pe");
        pedsParams.put("passwd", "foo");
        catalog.add(peDatastore);

        data.setUp();
    }

    void addFeature(FeatureStore store, String wkt, Object... atts) throws Exception {
        SimpleFeatureBuilder b = new SimpleFeatureBuilder((SimpleFeatureType) store.getSchema());
        b.add(new WKTReader().read(wkt));
        for (Object att : atts) {
            b.add(att);
        }

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, null);
        features.add(b.buildFeature(null));
        store.addFeatures(features);
    }

    public static Resource file(String path) throws Exception {
        Resource dir = BackupUtils.tmpDir();

        if (dir.dir().exists()) {
            FileUtils.forceDelete(dir.dir());
        }

        return file(path, dir);
    }

    public static Resource file(String path, Resource dir) throws IOException {
        String filename = new File(path).getName();
        InputStream in = BackupRestoreTestSupport.class.getResourceAsStream("test-data/" + path);

        File file = new File(dir.dir(), filename);

        if (file.exists()) {
            FileUtils.forceDelete(file);
        }

        FileOutputStream out = new FileOutputStream(file);
        try {
            IOUtils.copy(in, out);
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                in.close();
            }

            if (out != null) {
                out.flush();
                out.close();
            }
        }

        return org.geoserver.platform.resource.Files.asResource(file);
    }

    public void cleanCatalog() {
        try {
            for (StoreInfo s : catalog.getStores(StoreInfo.class)) {
                removeStore(s.getWorkspace().getName(), s.getName());
            }
            for (StyleInfo s : catalog.getStyles()) {
                String styleName = s.getName();
                if (!DEFAULT_STYLEs.contains(styleName)) {
                    removeStyle(null, styleName);
                }
            }

            int cnt = 0;
            do {
                try {
                    root.delete();
                    FileUtils.forceDelete(root);
                } catch (Exception e) {
                    cnt++;
                }
            } while (root.exists() && cnt < 30);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Please, ensure the temp folder have been correctly cleaned out!",
                    e);
        }

        catalog.dispose();
    }

    /** @throws InterruptedException */
    protected void ensureCleanedQueues() throws InterruptedException {
        int cnt = 0;
        while (!(backupFacade.getRestoreRunningExecutions().isEmpty()
                && backupFacade.getBackupRunningExecutions().isEmpty())) {
            if (cnt > 30) {
                LOGGER.log(Level.SEVERE, "Could not cleanup Running Executions Queues!");
                break;
            }
            // Wait a bit
            Thread.sleep(10);
            cnt++;
        }
    }
}

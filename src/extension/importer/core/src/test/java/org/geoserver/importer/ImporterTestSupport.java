/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public abstract class ImporterTestSupport extends GeoServerSystemTestSupport {

    static Logger LOGGER = Logging.getLogger(ImporterTestSupport.class);

    static final Set<String> DEFAULT_STYLEs =
            new HashSet<String>() {
                {
                    add(StyleInfo.DEFAULT_POINT);
                    add(StyleInfo.DEFAULT_LINE);
                    add(StyleInfo.DEFAULT_GENERIC);
                    add(StyleInfo.DEFAULT_POLYGON);
                    add(StyleInfo.DEFAULT_RASTER);
                }
            };

    protected Importer importer;

    GeoServerDataDirectory dd;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        ImporterTestUtils.setComparisonTolerance();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        // add test importer properties file to the temporary data dir. For testing purposes only
        dd = new GeoServerDataDirectory(testData.getDataDirectoryRoot());
        GeoServerExtensionsHelper.singleton("dataDirectory", dd, GeoServerDataDirectory.class);

        // write out a simple shell script in the data dir and make it executable
        File importerConfigLocation = dd.findOrCreateDir("importer");
        File importerConfigProps = new File(importerConfigLocation, "importer.properties");
        IOUtils.copy(
                ImporterTestSupport.class
                        .getClassLoader()
                        .getResourceAsStream("importer.properties"),
                importerConfigProps);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no pre-existing test data needed for the importer
        testData.setUpSecurity();
    }

    @After
    public void cleanCatalog() throws IOException {
        for (StoreInfo s : getCatalog().getStores(StoreInfo.class)) {
            removeStore(s.getWorkspace().getName(), s.getName());
        }
        for (StyleInfo s : getCatalog().getStyles()) {
            String styleName = s.getName();
            if (!DEFAULT_STYLEs.contains(styleName)) {
                removeStyle(null, styleName);
            }
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);

        File dir = dd.getRoot().dir();
        FileUtils.deleteQuietly(dir);

        try {
            if (System.getProperty(ImporterInfoDAO.UPLOAD_ROOT_KEY) != null) {
                System.clearProperty(ImporterInfoDAO.UPLOAD_ROOT_KEY);
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not remove System ENV variable {"
                            + ImporterInfoDAO.UPLOAD_ROOT_KEY
                            + "}",
                    e);
        }
    }

    @Before
    public void setupImporterField() {
        setupImporterFieldInternal();
    }

    /**
     * Override this method in case a subclass has other @Before that need to run after {@link
     * #setupImporterField()}
     */
    protected void setupImporterFieldInternal() {
        importer = (Importer) applicationContext.getBean("importer");
        // clean up the import history (to isolate tests from each other)
        if (importer.getStore() instanceof MemoryImportStore) {
            MemoryImportStore store = (MemoryImportStore) importer.getStore();
            store.destroy();
        } else {
            importer.getStore().removeAll();
        }
    }

    protected File tmpDir() throws Exception {
        return ImporterTestUtils.tmpDir();
    }

    protected File unpack(String path) throws Exception {
        return ImporterTestUtils.unpack(path);
    }

    protected File unpack(String path, File dir) throws Exception {
        return ImporterTestUtils.unpack(path, dir);
    }

    protected File file(String path) throws Exception {
        return ImporterTestUtils.file(path);
    }

    protected File file(String path, File dir) throws IOException {
        return ImporterTestUtils.file(path, dir);
    }

    protected void runChecks(String layerName) throws Exception {
        LayerInfo layer = getCatalog().getLayerByName(layerName);
        assertNotNull(layer);
        assertNotNull(layer.getDefaultStyle());
        assertNotNull(layer.getResource().getProjectionPolicy());

        if (layer.getType() == PublishedType.VECTOR) {
            FeatureTypeInfo featureType = (FeatureTypeInfo) layer.getResource();
            FeatureSource source = featureType.getFeatureSource(null, null);
            assertTrue(source.getCount(Query.ALL) > 0);

            // do a wfs request
            Document dom =
                    getAsDOM("wfs?request=getFeature&typename=" + featureType.prefixedName());
            assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
            assertEquals(
                    source.getCount(Query.ALL),
                    dom.getElementsByTagName(featureType.prefixedName()).getLength());
        }

        // do a wms request
        MockHttpServletResponse response =
                getAsServletResponse("wms/reflect?layers=" + layer.getResource().prefixedName());
        assertEquals("image/png", response.getContentType());
    }

    protected DataStoreInfo createH2DataStore(String wsName, String dsName) {
        // create a datastore to import into
        Catalog cat = getCatalog();

        WorkspaceInfo ws =
                wsName != null ? cat.getWorkspaceByName(wsName) : cat.getDefaultWorkspace();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setWorkspace(ws);
        ds.setName(dsName);
        ds.setType("H2");

        Map params = new HashMap();
        params.put("database", getTestData().getDataDirectoryRoot().getPath() + "/" + dsName);
        params.put("dbtype", "h2");
        ds.getConnectionParameters().putAll(params);
        ds.setEnabled(true);
        cat.add(ds);

        return ds;
    }

    /** Adding special treatment for H2 databases, we want to also kill the db itself */
    @Override
    protected void removeStore(String workspaceName, String name) {
        Catalog cat = getCatalog();
        StoreInfo store = cat.getStoreByName(workspaceName, name, StoreInfo.class);
        if (store == null) {
            return;
        }

        // store the database location in case it's a H2 store
        Map<String, Serializable> params = store.getConnectionParameters();
        String databaseLocation = null;
        if ("h2".equals(params.get("dbtype"))) {
            databaseLocation = (String) params.get("database");
        }

        CascadeDeleteVisitor v = new CascadeDeleteVisitor(getCatalog());
        store.accept(v);

        // clean up the database files if needed
        if (databaseLocation != null) {
            final File dbFile = new File(databaseLocation);
            File container = dbFile.getParentFile();
            File[] dbFiles =
                    container.listFiles(
                            new FilenameFilter() {

                                @Override
                                public boolean accept(File dir, String name) {
                                    return name.startsWith(dbFile.getName());
                                }
                            });
            for (File f : dbFiles) {
                assertTrue("Failed to remove file " + f.getPath(), FileUtils.deleteQuietly(f));
            }
        }
    }

    private String createSRSJSON(String srs) {
        return "{" + "\"layer\":   {" + "\"srs\": \"" + srs + "\"" + "}" + "}";
        /*return "{" +
          "\"resource\": {" +
            "\"featureType\":   {" +
               "\"srs\": \"" + srs + "\"" +
             "}" +
           "}" +
        "}";*/
    }

    protected MockHttpServletResponse setSRSRequest(String url, String srs) throws Exception {
        String srsRequest = createSRSJSON(srs);
        return putAsServletResponse(url, srsRequest, "application/json");
    }

    protected void assertErrorResponse(MockHttpServletResponse resp, String... errs)
            throws UnsupportedEncodingException {
        assertEquals(400, resp.getStatus());
        // TODO: Implement JSON error response
        /*
        JSONObject json = JSONObject.fromObject(resp.getContentAsString());
        JSONArray errors = json.getJSONArray("errors");
        assertNotNull("Expected error array", errors);
        assertEquals(errs.length, errors.size());
        for (int i = 0; i < errs.length; i++) {
            assertEquals(errors.get(i), errs[i]);
        }
        */
    }

    protected int lastId() {
        Iterator<ImportContext> ctx = importer.getAllContexts();
        long max = 0;
        while (ctx.hasNext()) {
            max = Math.max(ctx.next().getId(), max);
        }
        return (int) max;
    }

    public static class JSONObjectBuilder extends JSONBuilder {

        public JSONObjectBuilder() {
            super(new StringWriter());
        }

        public JSONObject buildObject() {
            return JSONObject.fromObject(((StringWriter) writer).toString());
        }
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return SystemUtils.IS_OS_WINDOWS;
    }
}

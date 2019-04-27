/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.*;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.servlet.Filter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.filters.LoggingFilter;
import org.geoserver.platform.resource.Files;
import org.geotools.util.URLs;
import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DataStoreFileUploadWFSTest extends CatalogRESTTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        NamespaceInfo gsmlNamespace = new NamespaceInfoImpl();
        gsmlNamespace.setPrefix("gsml");
        gsmlNamespace.setURI("http://www.cgi-iugs.org/xml/GeoSciML/2");

        WorkspaceInfo gsmlWorkspace = new WorkspaceInfoImpl();
        gsmlWorkspace.setName("gsml");

        getCatalog().add(gsmlNamespace);
        getCatalog().add(gsmlWorkspace);
    }

    @Override
    protected List<Filter> getFilters() {
        LoggingFilter filter = new LoggingFilter();
        filter.setEnabled(true);
        filter.setLogBodies(true);
        return Collections.singletonList(filter);
    }

    @Before
    public void removePdsDataStore() {
        removeStore("gs", "pds");
        removeStore("gs", "store with spaces");
    }

    @After
    public void cleanUpDbFiles() throws Exception {
        DeleteDbFiles.execute("target", "foo", true);
        DeleteDbFiles.execute("target", "pds", true);
        DeleteDbFiles.execute("target", "chinese_poly", true);
    }

    @Test
    public void testPropertyFileUpload() throws Exception {
        /*
        Properties p = new Properties();
        p.put( "_", "name:String,pointProperty:Point");
        p.put( "pds.0", "'zero'|POINT(0 0)");
        p.put( "pds.1", "'one'|POINT(1 1)");
        */
        byte[] bytes = propertyFile();
        // p.store( output, null );

        put(ROOT_PATH + "/workspaces/gs/datastores/pds/file.properties", bytes, "text/plain");
        Document dom = getAsDOM("wfs?request=getfeature&typename=gs:pds");
        assertFeatures(dom);
    }

    @Test
    public void testPropertyFileUploadWithWorkspace() throws Exception {
        byte[] bytes = propertyFile();

        put(ROOT_PATH + "/workspaces/sf/datastores/pds/file.properties", bytes, "text/plain");
        Document dom = getAsDOM("wfs?request=getfeature&typename=sf:pds");
        assertFeatures(dom, "sf");
    }

    @Test
    public void testPropertyFileUploadZipped() throws Exception {
        byte[] bytes = propertyFile();

        // compress
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(out);
        zout.putNextEntry(new ZipEntry("pds.properties"));
        zout.write(bytes);
        zout.flush();
        zout.close();

        put(
                ROOT_PATH + "/workspaces/gs/datastores/pds/file.properties",
                out.toByteArray(),
                "application/zip");

        Document dom = getAsDOM("wfs?request=getfeature&typename=gs:pds");
        assertFeatures(dom);
    }

    byte[] propertyFile() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
        writer.write("_=name:String,pointProperty:Point\n");
        writer.write("ds.0='zero'|POINT(0 0)\n");
        writer.write("ds.1='one'|POINT(1 1)\n");
        writer.flush();
        return output.toByteArray();
    }

    void assertFeatures(Document dom) throws Exception {
        assertFeatures(dom, "gs");
    }

    void assertFeatures(Document dom, String ns) throws Exception {
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(2, dom.getElementsByTagName(ns + ":pds").getLength());
    }

    @Test
    public void testShapeFileUpload() throws Exception {
        byte[] bytes = shpZipAsBytes();
        put(ROOT_PATH + "/workspaces/gs/datastores/pds/file.shp", bytes, "application/zip");
        Document dom = getAsDOM("wfs?request=getfeature&typename=gs:pds");
        assertFeatures(dom);
    }

    @Test
    public void testShapeFileUploadWithCharset() throws Exception {
        /* Requires that a zipped shapefile (chinese_poly.zip) be in test-data directory */
        byte[] bytes = shpChineseZipAsBytes();
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/datastores/chinese_poly/file.shp?charset=UTF-8",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());

        MockHttpServletResponse response2 =
                getAsServletResponse("wfs?request=getfeature&typename=gs:chinese_poly", "GB18030");
        assertTrue(response2.getContentAsString().contains("\u951f\u65a4\u62f7"));
    }

    byte[] shpZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream("test-data/pds.zip"));
    }

    byte[] shpChineseZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream("test-data/chinese_poly.zip"));
    }

    byte[] shpMultiZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream("test-data/pdst.zip"));
    }

    byte[] toBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int c;
        while ((c = in.read()) != -1) {
            out.write(c);
        }
        return out.toByteArray();
    }

    @Test
    public void testShapeFileUploadExternal() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&typename=gs:pds");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        File target = new File("target");
        File f = File.createTempFile("rest", "dir", target);
        try {
            f.delete();
            f.mkdir();

            File zip = new File(f, "pds.zip");
            IOUtils.copy(
                    getClass().getResourceAsStream("test-data/pds.zip"), new FileOutputStream(zip));
            org.geoserver.rest.util.IOUtils.inflate(new ZipFile(zip), Files.asResource(f), null);

            MockHttpServletResponse resp =
                    putAsServletResponse(
                            ROOT_PATH + "/workspaces/gs/datastores/pds/external.shp",
                            URLs.fileToUrl(new File(f, "pds.shp")).toString(),
                            "text/plain");
            assertEquals(201, resp.getStatus());

            dom = getAsDOM("wfs?request=getfeature&typename=gs:pds");
            assertFeatures(dom);

            // try to download it again after a full reload from disk (GEOS-4616)
            getGeoServer().reload();

            resp = getAsServletResponse(ROOT_PATH + "/workspaces/gs/datastores/pds/file.shp");
            assertEquals(200, resp.getStatus());
            assertEquals("application/zip", resp.getContentType());

            Set<String> entryNames = new HashSet<>();
            try (ByteArrayInputStream bin = getBinaryInputStream(resp);
                    ZipInputStream zin = new ZipInputStream(bin)) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    entryNames.add(entry.getName());
                }
            }
            assertTrue(entryNames.contains("pds.shp"));
            assertTrue(entryNames.contains("pds.shx"));
            assertTrue(entryNames.contains("pds.dbf"));
        } finally {
            FileUtils.deleteQuietly(f);
        }
    }

    @Test
    public void testShapeFileUploadIntoExisting() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "foo_h2"));

        String xml =
                "<dataStore>"
                        + " <name>foo_h2</name>"
                        + " <type>H2</type>"
                        + " <connectionParameters>"
                        + "<namespace>"
                        + MockData.DEFAULT_URI
                        + "</namespace>"
                        + "<database>target/foo</database>"
                        + "<dbtype>h2</dbtype>"
                        + " </connectionParameters>"
                        + "<workspace>gs</workspace>"
                        + "</dataStore>";

        post(ROOT_PATH + "/workspaces/gs/datastores", xml);

        DataStoreInfo ds = cat.getDataStoreByName("gs", "foo_h2");
        assertNotNull(ds);

        assertTrue(cat.getFeatureTypesByDataStore(ds).isEmpty());

        byte[] bytes = shpZipAsBytes();
        put(ROOT_PATH + "/workspaces/gs/datastores/foo_h2/file.shp", bytes, "application/zip");

        assertFalse(cat.getFeatureTypesByDataStore(ds).isEmpty());

        Document dom = getAsDOM("wfs?request=getfeature&typename=gs:pds");
        assertFeatures(dom);
    }

    @Test
    public void testShapeFileUploadWithTarget() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "pds"));

        byte[] bytes = shpZipAsBytes();
        put(
                ROOT_PATH + "/workspaces/gs/datastores/pds/file.shp?target=h2",
                bytes,
                "application/zip");

        DataStoreInfo ds = cat.getDataStoreByName("gs", "pds");
        assertNotNull(ds);
        assertFalse(cat.getFeatureTypesByDataStore(ds).isEmpty());

        Document dom = getAsDOM("wfs?request=getfeature&typename=gs:pds");
        assertFeatures(dom);
    }

    @Test
    @Ignore
    // fixing https://osgeo-org.atlassian.net/browse/GEOS-6845, re-enable when a proper fix for
    // spaces in
    // name has been made
    public void testShapeFileUploadWithSpaces() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "store with spaces"));

        byte[] bytes = shpZipAsBytes();
        put(
                ROOT_PATH + "/workspaces/gs/datastores/store%20with%20spaces/file.shp",
                bytes,
                "application/zip");

        DataStoreInfo ds = cat.getDataStoreByName("gs", "store with spaces");
        assertNull(ds);
    }
}

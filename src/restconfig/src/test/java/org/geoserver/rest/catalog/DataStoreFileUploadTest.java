/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.Filter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.filters.LoggingFilter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.URLs;
import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataStoreFileUploadTest extends CatalogRESTTestSupport {

    @ClassRule public static TemporaryFolder temp = new TemporaryFolder();

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
        LoggingFilter filter = new LoggingFilter(null);
        filter.setEnabled(true);
        filter.setLogBodies(true);
        return Collections.singletonList(filter);
    }

    @Before
    public void removePdsDataStore() {
        removeStore("gs", "pds");
        removeStore("gs", "store with spaces");
        removeStore("gs", "san_andres_y_providencia");
    }

    @After
    public void cleanUpDbFiles() throws Exception {
        DeleteDbFiles.execute("target", "foo", true);
        DeleteDbFiles.execute("target", "pds", true);
        DeleteDbFiles.execute("target", "chinese_poly", true);
        DeleteDbFiles.execute("target", "san_andres_y_providencia", true);
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

    byte[] shpZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream("test-data/pds.zip"));
    }

    byte[] shpChineseZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream("test-data/chinese_poly.zip"));
    }

    byte[] shpSanAndresShapefilesZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream("test-data/san_andres_y_providencia.zip"));
    }

    byte[] shpMultiZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream("test-data/pdst.zip"));
    }

    byte[] shpSameNameZipAsBytes(String path) throws IOException {
        return toBytes(getClass().getResourceAsStream(path));
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
    public void testShapeFileUploadNotExisting() throws Exception {
        File file = new File("./target/notThere.tiff");
        if (file.exists()) {
            assertTrue(file.delete());
        }

        URL url = URLs.fileToUrl(file.getCanonicalFile());
        String body = url.toExternalForm();
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/datastores/pds/external.shp",
                        body,
                        "text/plain");
        assertEquals(400, response.getStatus());
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

    @Test
    public void testShapefileUploadMultiple() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "pdst"));

        put(
                ROOT_PATH + "/workspaces/gs/datastores/pdst/file.shp?configure=all",
                shpMultiZipAsBytes(),
                "application/zip");

        DataStoreInfo ds = cat.getDataStoreByName("gs", "pdst");
        assertNotNull(ds);

        assertEquals(2, cat.getFeatureTypesByDataStore(ds).size());
    }

    @Test
    public void testRenamingFeatureTypeAlreadyInCatalog() throws Exception {
        Catalog cat = getCatalog();

        put(
                ROOT_PATH + "/workspaces/gs/datastores/10bt/file.shp?configure=all",
                shpSameNameZipAsBytes("test-data/10bt.zip"),
                "application/zip");

        List<String> ftNames = new ArrayList<>(10);
        DataStoreInfo ds = cat.getDataStoreByName("gs", "10bt");
        cat.getFeatureTypesByDataStore(ds).forEach((ft) -> ftNames.add(ft.getName()));

        assertEquals(10, ftNames.size());

        put(
                ROOT_PATH + "/workspaces/gs/datastores/bt/file.shp?configure=all",
                shpSameNameZipAsBytes("test-data/bt.zip"),
                "application/zip");

        DataStoreInfo ds2 = cat.getDataStoreByName("gs", "bt");

        assertEquals("bt10", cat.getFeatureTypesByDataStore(ds2).get(0).getName());
    }

    @Test
    public void testShapefileUploadZip() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "san_andres_y_providencia"));

        put(
                ROOT_PATH + "/workspaces/gs/datastores/san_andres_y_providencia/file.shp",
                shpSanAndresShapefilesZipAsBytes(),
                "application/zip");

        DataStoreInfo ds = cat.getDataStoreByName("gs", "san_andres_y_providencia");
        assertNotNull(ds);

        assertEquals(1, cat.getFeatureTypesByDataStore(ds).size());
    }

    @Test
    public void testGetProperties() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(ROOT_PATH + "/workspaces/gs/datastores/pds/file.properties");
        assertEquals(404, resp.getStatus());

        byte[] bytes = propertyFile();
        put(ROOT_PATH + "/workspaces/gs/datastores/pds/file.properties", bytes, "text/plain");

        resp = getAsServletResponse(ROOT_PATH + "/workspaces/gs/datastores/pds/file.properties");
        assertEquals(200, resp.getStatus());
        assertEquals("application/zip", resp.getContentType());

        ByteArrayInputStream bin = getBinaryInputStream(resp);
        ZipInputStream zin = new ZipInputStream(bin);

        ZipEntry entry = zin.getNextEntry();
        assertNotNull(entry);
        assertEquals("pds.properties", entry.getName());
    }

    @Test
    public void testAppSchemaMappingFileUpload() throws Exception {
        byte[] bytes = appSchemaMappingAsBytes();
        if (bytes == null) {
            // skip test
            LOGGER.warning("app-schema test data not available: skipping test");
            return;
        }

        // copy necessary .properties files from classpath
        loadAppSchemaTestData();

        // upload mapping file (datastore is created implicitly)
        put(
                ROOT_PATH + "/workspaces/gsml/datastores/mappedPolygons/file.appschema",
                bytes,
                "text/xml");
        Document dom = getAsDOM("wfs?request=getfeature&typename=gsml:MappedFeature");

        // print(dom);

        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        NodeList mappedFeatureNodes =
                dom.getDocumentElement()
                        .getElementsByTagNameNS(
                                "http://www.cgi-iugs.org/xml/GeoSciML/2", "MappedFeature");
        assertNotNull(mappedFeatureNodes);
        assertEquals(2, mappedFeatureNodes.getLength());

        int namesCount = countNameAttributes(mappedFeatureNodes.item(0));
        assertEquals(2, namesCount);

        // upload alternative mapping file
        bytes = appSchemaAlternativeMappingAsBytes();
        put(
                ROOT_PATH
                        + "/workspaces/gsml/datastores/mappedPolygons/file.appschema?configure=none",
                bytes,
                "text/xml");
        dom = getAsDOM("wfs?request=getfeature&typename=gsml:MappedFeature");

        // print(dom);

        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        mappedFeatureNodes =
                dom.getDocumentElement()
                        .getElementsByTagNameNS(
                                "http://www.cgi-iugs.org/xml/GeoSciML/2", "MappedFeature");
        assertNotNull(mappedFeatureNodes);
        assertEquals(2, mappedFeatureNodes.getLength());

        namesCount = countNameAttributes(mappedFeatureNodes.item(0));
        // just one name should be found
        assertEquals(1, namesCount);
    }

    private int countNameAttributes(Node mappedFeatureNode) {
        NodeList attrNodes = mappedFeatureNode.getChildNodes();
        int namesCount = 0;
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Node attribute = attrNodes.item(i);
            if ("name".equals(attribute.getLocalName())) {
                namesCount++;
            }
        }

        return namesCount;
    }

    private void loadAppSchemaTestData() throws IOException {
        GeoServerResourceLoader loader =
                new GeoServerResourceLoader(getTestData().getDataDirectoryRoot());
        loader.copyFromClassPath(
                "test-data/mappedPolygons.properties", "data/gsml/mappedPolygons.properties");
        loader.copyFromClassPath(
                "test-data/mappedPolygons.oasis.xml", "data/gsml/mappedPolygons.oasis.xml");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/CGI_basicTypes.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/CGI_basicTypes.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/CGI_Value.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/CGI_Value.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/earthMaterial.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/earthMaterial.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/fossil.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/fossil.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/geologicStructure.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/geologicStructure.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/geologicUnit.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/geologicUnit.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/geosciml.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/geosciml.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/Gsml.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/Gsml.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/metadata.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/metadata.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/ObsAndMeas.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/ObsAndMeas.xsd");
        loader.copyFromClassPath(
                "test-data/commonSchemas_new/GeoSciML/vocabulary.xsd",
                "data/gsml/commonSchemas_new/GeoSciML/vocabulary.xsd");
    }

    @SuppressWarnings("PMD.CloseResource")
    private byte[] appSchemaMappingAsBytes() throws IOException {
        InputStream in = getClass().getResourceAsStream("/test-data/mappedPolygons.xml");
        if (in != null) {
            byte[] original = toBytes(in);

            String originalAsString = new String(original, StandardCharsets.UTF_8);
            // modify paths in the original mapping file
            String modifiedAsString =
                    originalAsString
                            .replace("file:./", "file:../")
                            .replace("commonSchemas_new/", "../commonSchemas_new/")
                            .replace("mappedPolygons.oasis", "../mappedPolygons.oasis");

            byte[] modified = modifiedAsString.getBytes(StandardCharsets.UTF_8);

            return modified;
        } else {
            return null;
        }
    }

    private byte[] appSchemaAlternativeMappingAsBytes() throws Exception {
        byte[] mapping = appSchemaMappingAsBytes();
        if (mapping != null) {
            Document mappingDom = dom(new ByteArrayInputStream(mapping));

            // remove mapping for MappedFeature/gml:name[2] attribute
            NodeList attrMappingNodes =
                    mappingDom.getDocumentElement().getElementsByTagName("AttributeMapping");
            for (int i = 0; i < attrMappingNodes.getLength(); i++) {
                Node attrMapping = attrMappingNodes.item(i);
                NodeList children = attrMapping.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if ("MappedFeature/gml:name[2]".equals(children.item(j).getTextContent())) {
                        attrMapping.getParentNode().removeChild(attrMapping);
                        break;
                    }
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            print(mappingDom, output);

            return output.toByteArray();
        } else {
            return null;
        }
    }

    @Test
    public void testShapefileUploadExternalZipDirectory() throws Exception {
        // get the path to a directory
        File file = temp.getRoot();
        String body = file.getAbsolutePath();
        // the request will fail since it won't attempt to copy a directory
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/foo/datastores/bar/external.shp",
                        body,
                        "application/zip");
        assertEquals(500, response.getStatus());
        assertThat(response.getContentAsString(), startsWith("Error renaming zip file from "));
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file.exists());
    }

    @Test
    public void testShapefileUploadExternalZipExistingDirectory() throws Exception {
        // create a file to copy and get its path
        File file1 = temp.newFile("test1.zip");
        String body = file1.getAbsolutePath();
        // create the file in the data directory
        File file2 = getResourceLoader().createDirectory("data/foo/bar1/test1.zip");
        // the request will fail since it won't overwrite an existing zip file
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/foo/datastores/bar1/external.shp",
                        body,
                        "application/zip");
        assertEquals(500, response.getStatus());
        assertThat(response.getContentAsString(), startsWith("Error renaming zip file from "));
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file1.exists());
        // verify that the file in the data directory was not deleted
        assertTrue("The file in the data directory was unexpectedly deleted", file2.isDirectory());
    }

    @Test
    public void testShapefileUploadExternalZipBadFile() throws Exception {
        // create a file that is not a valid zip file and get its path
        File file = temp.newFile("test2.zip");
        String body = file.getAbsolutePath();
        // the request will fail unzipping since it is not a valid zip fail
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/foo/datastores/bar2/external.shp",
                        body,
                        "application/zip");
        assertEquals(500, response.getStatus());
        assertEquals("Error occured unzipping file", response.getContentAsString());
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file.exists());
        // verify that the zip file was deleted from the data directory
        assertEquals(
                "The data directory file was not deleted",
                Resource.Type.UNDEFINED,
                getResourceLoader().get("data/foo/bar2/test2.zip").getType());
    }

    @Test
    public void testShapefileUploadExternalZipValid() throws Exception {
        // create a valid zip file and get its path
        File file = temp.newFile("test3.zip");
        Files.write(file.toPath(), shpSanAndresShapefilesZipAsBytes());
        String body = file.getAbsolutePath();
        // verify that the datastore does not already exist
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "san_andres_y_providencia"));
        // the request should succeed
        put(
                ROOT_PATH + "/workspaces/gs/datastores/san_andres_y_providencia/external.shp",
                body,
                "application/zip");
        // verify that the datastore was created successfully
        DataStoreInfo ds = cat.getDataStoreByName("gs", "san_andres_y_providencia");
        assertNotNull(ds);
        assertEquals(1, cat.getFeatureTypesByDataStore(ds).size());
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file.exists());
        // verify that the zip file was deleted from the data directory
        assertEquals(
                "The data directory file was not deleted",
                Resource.Type.UNDEFINED,
                getResourceLoader().get("data/gs/san_andres_y_providencia/test3.zip").getType());
    }
}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestBaseController;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CoverageStoreControllerTest extends CatalogRESTTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void addBlueMarbleCoverage() throws Exception {
        getTestData().addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
    }

    @Test
    public void testGetAllOnMissingWorkspace() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/abcde/coveragestores.xml");
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(), containsString("abcde"));
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores.xml");
        assertEquals(
                catalog.getStoresByWorkspace("wcs", CoverageStoreInfo.class).size(),
                dom.getElementsByTagName("coverageStore").getLength());
    }

    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores.json");
        assertTrue(json instanceof JSONObject);

        Object coveragestores =
                ((JSONObject) json).getJSONObject("coverageStores").get("coverageStore");
        assertNotNull(coveragestores);

        if (coveragestores instanceof JSONArray) {
            assertEquals(
                    catalog.getCoverageStoresByWorkspace("wcs").size(),
                    ((JSONArray) coveragestores).size());
        } else {
            assertEquals(1, catalog.getCoverageStoresByWorkspace("wcs").size());
        }
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores.html");
        List<CoverageStoreInfo> coveragestores = catalog.getCoverageStoresByWorkspace("wcs");

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(coveragestores.size(), links.getLength());

        for (int i = 0; i < coveragestores.size(); i++) {
            CoverageStoreInfo cs = coveragestores.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(cs.getName() + ".html"));
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(
                                RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores")
                        .getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores")
                        .getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble.xml");
        // print(dom);
        assertEquals("coverageStore", dom.getDocumentElement().getNodeName());
        assertEquals("BlueMarble", xp.evaluate("/coverageStore/name", dom));
        assertEquals("wcs", xp.evaluate("/coverageStore/workspace/name", dom));
        assertEquals(
                "http://localhost:8080/geoserver"
                        + RestBaseController.ROOT_PATH
                        + "/workspaces/wcs.xml",
                xp.evaluate("/coverageStore/workspace/atom:link/@href", dom));
        assertEquals(
                "http://localhost:8080/geoserver"
                        + RestBaseController.ROOT_PATH
                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages.xml",
                xp.evaluate("/coverageStore/coverages/atom:link/@href", dom));
    }

    @Test
    public void testGetAsHTML() throws Exception {
        // rename to test
        Catalog catalog = getCatalog();
        CoverageInfo coverage = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_BM));
        String oldName = coverage.getName();
        coverage.setName("fooBar");
        catalog.save(coverage);

        try {
            Document dom =
                    getAsDOM(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/wcs/coveragestores/BlueMarble.html");

            CoverageStoreInfo cs =
                    CatalogRESTTestSupport.catalog.getCoverageStoreByName("wcs", "BlueMarble");
            List<CoverageInfo> coverages =
                    CatalogRESTTestSupport.catalog.getCoveragesByCoverageStore(cs);

            NodeList links = xp.getMatchingNodes("//html:a", dom);
            assertEquals(coverages.size(), links.getLength());

            for (int i = 0; i < coverages.size(); i++) {
                CoverageInfo cov = coverages.get(i);
                Element link = (Element) links.item(i);
                assertTrue(
                        link.getAttribute("href").endsWith("coverages/" + cov.getName() + ".html"));
            }
        } finally {
            // revert change
            coverage.setName(oldName);
            catalog.save(coverage);
        }
    }

    @Test
    public void testGetWrongCoverageStore() throws Exception {
        // Parameters for the request
        String ws = "wcs";
        String cs = "BlueMarblesssss";
        // Request path
        String requestPath =
                RestBaseController.ROOT_PATH
                        + "/workspaces/"
                        + ws
                        + "/coveragestores/"
                        + cs
                        + ".html";
        // Exception path
        String exception = "No such coverage store: " + ws + "," + cs;
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    File setupNewCoverageStore() throws Exception {
        File dir = new File("./target/usa");
        dir.mkdir();
        dir.deleteOnExit();

        File f = new File(dir, "usa.prj");
        f.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(f);
        IOUtils.copy(getClass().getResourceAsStream("test-data/usa.prj"), fout);
        fout.flush();
        fout.close();

        f = new File(dir, "usa.meta");
        f.deleteOnExit();
        fout = new FileOutputStream(f);
        IOUtils.copy(getClass().getResourceAsStream("test-data/usa.meta"), fout);
        fout.flush();
        fout.close();

        f = new File(dir, "usa.png");
        f.deleteOnExit();

        fout = new FileOutputStream(f);
        IOUtils.copy(getClass().getResourceAsStream("test-data/usa.png"), fout);
        fout.flush();
        fout.close();

        return f;
    }

    @Test
    public void testPostAsXML() throws Exception {
        removeStore("wcs", "newCoverageStore");

        File f = setupNewCoverageStore();
        String xml =
                "<coverageStore>"
                        + "<name>newCoverageStore</name>"
                        + "<type>WorldImage</type>"
                        + "<url>file://"
                        + f.getAbsolutePath()
                        + "</url>"
                        + "<workspace>wcs</workspace>"
                        + "</coverageStore>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores",
                        xml,
                        "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/wcs/coveragestores/newCoverageStore"));

        CoverageStoreInfo newCoverageStore = catalog.getCoverageStoreByName("newCoverageStore");
        assertNotNull(newCoverageStore);

        assertNotNull(newCoverageStore.getFormat());
    }

    @Test
    public void testRoundTripCoverageStoreXML() throws Exception {
        CoverageInfo before = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_BM));

        // get and re-write, does not go boom
        String xml =
                getAsString(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble.xml");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        // check nothing actually changed
        CoverageInfo after = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_BM));
        assertEquals(before, after);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble.json");
        // print(json);
        JSONObject coverageStore = ((JSONObject) json).getJSONObject("coverageStore");
        assertNotNull(coverageStore);

        assertEquals("BlueMarble", coverageStore.get("name"));
        assertEquals("wcs", coverageStore.getJSONObject("workspace").get("name"));
        assertNotNull(coverageStore.get("type"));
        assertNotNull(coverageStore.get("url"));
        assertEquals(
                "http://localhost:8080/geoserver"
                        + RestBaseController.ROOT_PATH
                        + "/workspaces/wcs.json",
                coverageStore.getJSONObject("workspace").get("href"));
        assertEquals(
                "http://localhost:8080/geoserver"
                        + RestBaseController.ROOT_PATH
                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages.json",
                coverageStore.get("coverages"));
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeStore("wcs", "newCoverageStore");
        File f = setupNewCoverageStore();
        String json =
                "{'coverageStore':{"
                        + "'name':'newCoverageStore',"
                        + "'type': 'WorldImage',"
                        + "'url':'"
                        + f.getAbsolutePath().replace('\\', '/')
                        + "',"
                        + "'workspace':'wcs',"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores",
                        json,
                        "text/json");

        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/wcs/coveragestores/newCoverageStore"));

        CoverageStoreInfo newCoverageStore = catalog.getCoverageStoreByName("newCoverageStore");
        assertNotNull(newCoverageStore);
        assertNotNull(newCoverageStore.getFormat());
    }

    @Test
    public void testRoundTripCoverageStoreJSON() throws Exception {
        CoverageInfo before = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_BM));

        // get and re-write, does not go boom
        String xml =
                getAsString(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble.json");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble",
                        xml,
                        "application/json");
        assertEquals(200, response.getStatus());

        // check nothing actually changed
        CoverageInfo after = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_BM));
        assertEquals(before, after);
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml =
                "<coverageStore>"
                        + "<name>BlueMarble</name>"
                        + "<enabled>false</enabled>"
                        + "</coverageStore>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testPut() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble.xml");
        assertXpathEvaluatesTo("true", "/coverageStore/enabled", dom);

        String xml =
                "<coverageStore>"
                        + "<name>BlueMarble</name>"
                        + "<enabled>false</enabled>"
                        + "</coverageStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble.xml");
        assertXpathEvaluatesTo("false", "/coverageStore/enabled", dom);

        assertFalse(catalog.getCoverageStoreByName("wcs", "BlueMarble").isEnabled());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        CoverageStoreInfo cs = catalog.getCoverageStoreByName("wcs", "BlueMarble");

        assertTrue(cs.isEnabled());

        String xml = "<coverageStore>" + "<name>BlueMarble</name>" + "</coverageStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        cs = catalog.getCoverageStoreByName("wcs", "BlueMarble");
        assertTrue(cs.isEnabled());
    }

    @Test
    @Ignore
    public void testPutEmptyAndHarvest() throws Exception {
        File dir = new File("./target/empty");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File f = new File(dir, "empty.zip");
        f.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(f);
        IOUtils.copy(getClass().getResourceAsStream("test-data/empty.zip"), fout);
        fout.flush();
        fout.close();

        final int length = (int) f.length();
        byte[] zipData = new byte[length];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(zipData);
        }

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/empty/file.imagemosaic?configure=none",
                        zipData,
                        "application/zip");
        // Store is created
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());

        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/empty.xml");
        assertXpathEvaluatesTo("true", "/coverageStore/enabled", dom);

        // Harvesting
        f = new File(dir, "NCOM_wattemp_020_20081031T0000000_12.tiff");
        f.deleteOnExit();
        fout = new FileOutputStream(f);
        IOUtils.copy(
                getClass()
                        .getResourceAsStream("test-data/NCOM_wattemp_020_20081031T0000000_12.tiff"),
                fout);
        fout.flush();
        fout.close();

        final String path = "file://" + f.getCanonicalPath();
        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/empty/external.imagemosaic",
                        path,
                        "text/plain");
        assertEquals(202, response.getStatus());

        // Getting the list of available coverages
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/empty/coverages.xml?list=all");
        assertXpathEvaluatesTo("index", "/list/coverageName", dom);
        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/empty?recurse=true&purge=all")
                        .getStatus());
    }

    private void purgeRequest(final String purge, final int expectedFiles) throws Exception {
        File dir = new File("./target/mosaicfordelete");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File f = new File(dir, "mosaic.zip");
        f.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(f);
        IOUtils.copy(getClass().getResourceAsStream("test-data/mosaic.zip"), fout);
        fout.flush();
        fout.close();

        final int length = (int) f.length();
        byte[] zipData = new byte[length];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(zipData);
        }

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/mosaicfordelete/file.imagemosaic",
                        zipData,
                        "application/zip");
        // Store is created
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/mosaicfordelete.xml");
        assertXpathEvaluatesTo("true", "/coverageStore/enabled", dom);

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        final File storeDir =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()),
                                "data/wcs/mosaicfordelete"),
                        true);
        File[] content = storeDir.listFiles();
        assertThat(content.length, anyOf(equalTo(9), equalTo(10), equalTo(11)));

        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/mosaicfordelete?recurse=true&purge="
                                        + purge)
                        .getStatus());
        content = storeDir.listFiles();

        // purge all: no files remaining; purge metadata: only 1 Granule remaining; purge none: all
        // files (11) remaining
        assertEquals(expectedFiles, content.length);

        assertNull(catalog.getCoverageStoreByName("wcs", "mosaicfordelete"));
    }

    @Test
    public void testDeletePurgeMetadataAfterConfigure() throws Exception {
        purgeRequest("metadata", 1);
    }

    @Test
    public void testDeletePurgeAllAfterConfigure() throws Exception {
        purgeRequest("all", 0);
    }

    @Test
    public void testPut2() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble.xml");
        assertXpathEvaluatesTo("GeoTIFF", "/coverageStore/type", dom);

        String xml =
                "<coverageStore>"
                        + "<name>BlueMarble</name>"
                        + "<type>WorldImage</type>"
                        + "</coverageStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        CoverageStoreInfo cs = catalog.getCoverageStoreByName("wcs", "BlueMarble");
        assertEquals("WorldImage", cs.getType());
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<coverageStore>" + "<name>changed</name>" + "</coverageStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/nonExistant",
                        xml,
                        "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/nonExistant")
                        .getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        CoverageStoreInfo cs = catalog.getCoverageStoreByName("wcs", "BlueMarble");
        List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore(cs);
        for (CoverageInfo c : coverages) {
            for (LayerInfo l : catalog.getLayers(c)) {
                catalog.remove(l);
            }
            catalog.remove(c);
        }

        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble")
                        .getStatus());
        assertNull(catalog.getCoverageStoreByName("wcs", "BlueMarble"));
    }

    @Test
    public void testDeleteNonEmpty() throws Exception {
        assertEquals(
                401,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble")
                        .getStatus());
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getCoverageStoreByName("wcs", "BlueMarble"));
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble?recurse=true");
        assertEquals(200, response.getStatus());

        assertNull(catalog.getCoverageStoreByName("wcs", "BlueMarble"));

        for (CoverageInfo c : catalog.getCoverages()) {
            if (c.getStore().getName().equals("BlueMarble")) {
                fail();
            }
        }
    }
}

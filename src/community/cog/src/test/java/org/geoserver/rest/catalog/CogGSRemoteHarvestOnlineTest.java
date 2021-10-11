/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Similar to the {@link CogRemoteHarvestOnlineTest} but testing Google Storage */
public class CogGSRemoteHarvestOnlineTest extends CatalogRESTTestSupport {

    private static String GS_COG_URI =
            "gs://gcp-public-data-landsat/LC08/01/044/034"
                    + "/LC08_L1GT_044034_20130330_20170310_01_T2"
                    + "/LC08_L1GT_044034_20130330_20170310_01_T2_B11.TIF";

    private static String HTTPS_COG_URI =
            "https://storage.googleapis"
                    + ".com/gcp-public-data-landsat/LC08/01/044/034"
                    + "/LC08_L1GT_044034_20130330_20170310_01_T2"
                    + "/LC08_L1GT_044034_20130330_20170310_01_T2_B11.TIF";

    protected boolean isOnline() {
        try {
            URL u = new URL(HTTPS_COG_URI);
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("HEAD");
            huc.connect();
            return huc.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // setup the namespace context for this test
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("gf", "http://www.geoserver.org/rest/granules");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testHarvestHTTPS() throws Exception {
        assumeTrue(isOnline());
        File dir = new File("./target/gs_https_empty");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File f = new File(dir, "gsempty.zip");
        f.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(f)) {
            IOUtils.copy(getClass().getResourceAsStream("test-data/gsempty.zip"), fout);
            fout.flush();
        }

        final int length = (int) f.length();
        byte[] zipData = new byte[length];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(zipData);
        }

        // setup store
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/file.imagemosaic?configure=none",
                        zipData,
                        "application/zip");
        assertEquals(201, response.getStatus());

        // Harvesting
        response =
                postAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/coveragestores/empty/remote.imagemosaic",
                        HTTPS_COG_URI,
                        "text/plain");
        assertEquals(202, response.getStatus());

        // Getting the list of available coverages
        Document dom =
                getAsDOM(ROOT_PATH + "/workspaces/gs/coveragestores/empty/coverages.xml?list=all");
        XMLAssert.assertXpathEvaluatesTo("emptycog", "/list/coverageName", dom);

        // Configuring the coverage
        response =
                postAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/coveragestores/empty/coverages",
                        "<coverage><name>emptyhttpscog</name><nativeName>emptycog</nativeName></coverage>",
                        "text/xml");
        assertEquals(201, response.getStatus());

        // Checking granules
        dom =
                getAsDOM(
                        ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/coverages/emptyhttpscog/index/granules");
        XMLAssert.assertXpathEvaluatesTo(HTTPS_COG_URI, "//gf:emptycog/gf:location", dom);
    }

    @Test
    public void testHarvestURI() throws Exception {
        assumeTrue(isOnline());
        File dir = new File("./target/gs_uri_empty");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File f = new File(dir, "gsempty.zip");
        f.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(f)) {
            IOUtils.copy(getClass().getResourceAsStream("test-data/gsempty.zip"), fout);
            fout.flush();
        }

        final int length = (int) f.length();
        byte[] zipData = new byte[length];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(zipData);
        }

        // setup store
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/file.imagemosaic?configure=none",
                        zipData,
                        "application/zip");
        assertEquals(201, response.getStatus());

        // Harvesting
        response =
                postAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/coveragestores/empty/remote.imagemosaic",
                        GS_COG_URI,
                        "text/plain");
        assertEquals(202, response.getStatus());

        // Getting the list of available coverages
        Document dom =
                getAsDOM(ROOT_PATH + "/workspaces/gs/coveragestores/empty/coverages.xml?list=all");
        XMLAssert.assertXpathEvaluatesTo("emptycog", "/list/coverageName", dom);

        // Configuring the coverage
        response =
                postAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/coveragestores/empty/coverages",
                        "<coverage><name>emptyhttpscog</name><nativeName>emptycog</nativeName></coverage>",
                        "text/xml");
        assertEquals(201, response.getStatus());

        // Checking granules
        dom =
                getAsDOM(
                        ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/coverages/emptyhttpscog/index/granules");
        XMLAssert.assertXpathEvaluatesTo(GS_COG_URI, "//gf:emptycog/gf:location", dom);
    }

    @Override
    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
}

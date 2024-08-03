/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
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
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CogRemoteHarvestOnlineTest extends CatalogRESTTestSupport {

    static final String MAIN_TEST_URL =
            "https://s3-us-west-2.amazonaws.com/landsat-pds/c1/L8/153/075/LC08_L1TP_153075_20190515_20190515_01_RT/LC08_L1TP_153075_20190515_20190515_01_RT_B3.TIF";

    protected boolean isOnline() {
        try {
            URL u = new URL(MAIN_TEST_URL);
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
    public void testHarvestRemoteURLInImageMosaic() throws Exception {
        assumeTrue(isOnline());
        File dir = new File("./target/empty");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File f = new File(dir, "empty.zip");
        f.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(f);
        org.apache.commons.io.IOUtils.copy(
                getClass().getResourceAsStream("test-data/empty.zip"), fout);
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
                                + "/workspaces/gs/coveragestores/empty/file.imagemosaic?configure=none",
                        zipData,
                        "application/zip");
        // Store is created
        assertEquals(201, response.getStatus());

        // Harvesting
        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/remote.imagemosaic",
                        MAIN_TEST_URL,
                        "text/plain");
        assertEquals(202, response.getStatus());

        // Getting the list of available coverages
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/coverages.xml?list=all");
        XMLAssert.assertXpathEvaluatesTo("emptycog", "/list/coverageName", dom);

        // Configuring the coverage
        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/coverages",
                        "<coverage><name>emptycog</name><nativeName>emptycog</nativeName></coverage>",
                        "text/xml");
        assertEquals(201, response.getStatus());

        // Harvesting another granule
        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/remote.imagemosaic",
                        "https://s3-us-west-2.amazonaws.com/landsat-pds/c1/L8/153/075/LC08_L1TP_153075_20190429_20190429_01_RT/LC08_L1TP_153075_20190429_20190429_01_RT_B1.TIF",
                        "text/plain");
        assertEquals(202, response.getStatus());

        // Check we have 2 granules
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/empty/coverages/emptycog/index/granules.xml");
        assertXpathEvaluatesTo("2", "count(//gf:emptycog)", dom);
    }

    @Override
    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
}

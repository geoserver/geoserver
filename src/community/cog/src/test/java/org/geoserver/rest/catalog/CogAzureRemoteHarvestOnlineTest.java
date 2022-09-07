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

/** Similar to the {@link CogRemoteHarvestOnlineTest} but testing Azure Blobs */
public class CogAzureRemoteHarvestOnlineTest extends CatalogRESTTestSupport {

    private static String HTTPS_COG_URI =
            "https://cogtestdata.blob.core.windows.net/cogtestdata/land_topo_cog_jpeg_1024.tif";

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
        File dir = new File("./target/azure_https_empty");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File f = new File(dir, "azureempty.zip");
        f.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(f)) {
            IOUtils.copy(getClass().getResourceAsStream("test-data/azureempty.zip"), fout);
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

    @Override
    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
}

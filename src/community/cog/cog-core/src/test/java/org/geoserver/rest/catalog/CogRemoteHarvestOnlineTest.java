/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;

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
import org.geoserver.rest.RestBaseController;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Similar to the {@link CogRemoteHarvestOnlineTest} but testing Google Storage */
public abstract class CogRemoteHarvestOnlineTest extends CatalogRESTTestSupport {

    private static final String COVERAGE_DEFINITION =
            "<coverage><name>$CN</name><nativeName>$CN</nativeName></coverage>";
    public static final String WS_COVERAGESTORES = "/workspaces/gs/coveragestores/";

    @BeforeClass
    public static void setSkipTrue() {
        System.setProperty("it.geosolutions.skip.external.files.lookup", "true");
    }

    @AfterClass
    public static void restoreSkip() {
        System.setProperty("it.geosolutions.skip.external.files.lookup", "false");
    }

    protected abstract String getCogURL();

    protected void configureCoverage(String coverageName) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(
                ROOT_PATH + "/workspaces/gs/coveragestores/" + coverageName + "/coverages",
                COVERAGE_DEFINITION.replace("$CN", coverageName),
                "text/xml");
        assertEquals(201, response.getStatus());
    }

    protected void harvestGranule(String storeName, String url) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(
                ROOT_PATH + WS_COVERAGESTORES + storeName + "/remote.imagemosaic", url, "text/plain");
        assertEquals(202, response.getStatus());
    }

    protected void createImageMosaicStore(String storeName, byte[] zipData) throws Exception {
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/" + storeName
                        + "/file.imagemosaic?configure=none",
                zipData,
                "application/zip");
        assertEquals(201, response.getStatus());
    }

    protected void checkGranuleExists(String storeName, String coverageName, String expectedUrl) throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/gs/coveragestores/" + storeName + "/coverages/" + coverageName
                + "/index/granules");
        XMLAssert.assertXpathEvaluatesTo(expectedUrl, "//gf:" + coverageName + "/gf:location", dom);
    }

    protected void checkExistingCoverages(String storeName, String expectedCoverageName) throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/gs/coveragestores/" + storeName + "/coverages.xml?list=all");
        XMLAssert.assertXpathEvaluatesTo(expectedCoverageName, "/list/coverageName", dom);
    }

    protected boolean isOnline() {
        try {
            URL u = new URL(getCogURL());
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("HEAD");
            huc.connect();
            return huc.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    protected byte[] prepareZipData(String folder, String zipArchiveName) throws IOException {
        File dir = new File("./target/" + folder);
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        String zipFileName = zipArchiveName + ".zip";
        File f = new File(dir, zipFileName);
        f.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(f)) {
            IOUtils.copy(getClass().getResourceAsStream("test-data/" + zipFileName), fout);
            fout.flush();
        }

        final int length = (int) f.length();
        byte[] zipData = new byte[length];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(zipData);
        }
        return zipData;
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
        // Do nothing on setup
    }

    @Override
    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
}

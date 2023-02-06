package org.geoserver.ogcapi.v1.stac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.util.IOUtils;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * This test class is created separately due to copying collection.ftl file to the data directory in
 * the tests. If it is not done in another class, it fails the tests that uses collection.ftl
 */
public class LoadJSONTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        replacePathAndCopy("/collections.ftl");
        copyTemplate("/collections-SENTINEL2.json", "templates/ogc/stac/v1/");
        copyTemplate("/collection.ftl", "templates/ogc/stac/v1/");

        GeoServerDataDirectory dd =
                (GeoServerDataDirectory) applicationContext.getBean("dataDirectory");

        File file = dd.getResourceLoader().createFile("/readAndEval.json");
        dd.getResourceLoader().copyFromClassPath("/readAndEval.json", file, getClass());
    }

    // it replaces the path with machine's local path
    private void replacePathAndCopy(String template) throws IOException, URISyntaxException {
        String nestedJsonPath =
                new File(
                                LoadJSONTest.class
                                        .getClassLoader()
                                        .getResource("readAndEval.json")
                                        .toURI())
                        .getCanonicalPath();
        // makes sure forward slash is used otherwise freemarker will complain
        if (nestedJsonPath.indexOf("\\") != -1)
            nestedJsonPath = nestedJsonPath.replaceAll("\\\\", "/");
        assertTrue(new File(nestedJsonPath).exists());
        String oldContent = IOUtils.toString(getClass().getResourceAsStream(template));
        String newContent = oldContent.replace("willBeReplaced", nestedJsonPath).trim();
        try (ByteArrayInputStream bais =
                new ByteArrayInputStream(newContent.getBytes(Charset.forName("UTF-8")))) {
            copyTemplate(template, bais);
        }
    }

    @Test
    public void testReadAndEvalJSON() throws Exception {
        Document dom = getAsJSoup("ogc/stac/v1/collections/SENTINEL2?f=html");

        assertEquals("attribute1 => 1 attribute2 => 2", dom.select("h2").text());
    }

    @Test
    public void testReadFileOutsideOfDataDir() throws Exception {
        MockHttpServletResponse response =
                getAsMockHttpServletResponse("ogc/stac/v1/collections?f=html", 200);
        assertTrue(response.getContentAsString().contains("FreeMarker template error"));
    }
}

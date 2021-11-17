package org.geoserver.ogcapi.stac;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.jsoup.nodes.Document;
import org.junit.Test;

/**
 * This test class is created separately due to copying collection.ftl file to the data directory in
 * the tests. If it is not done in another class, it fails the tests that uses collection.ftl
 */
public class LoadJSONTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyAbsolutePathToCollectionsFtl();

        copyTemplate("/collections-SENTINEL2.json");
        copyTemplate("/collection.ftl");
        copyTemplate("/collections.ftl");

        GeoServerDataDirectory dd =
                (GeoServerDataDirectory) applicationContext.getBean("dataDirectory");

        File file = dd.getResourceLoader().createFile("/readAndEval.json");
        dd.getResourceLoader().copyFromClassPath("/readAndEval.json", file, getClass());
    }

    // it replaces the path with machine's local path
    private void copyAbsolutePathToCollectionsFtl() throws IOException {
        String collectionsPath =
                LoadJSONTest.class.getClassLoader().getResource("collections.ftl").getPath();

        String nestedJsonPath =
                LoadJSONTest.class.getClassLoader().getResource("readAndEval.json").getPath();
        File fileToBeModified = new File(collectionsPath);

        FileWriter writer = null;

        try {
            String oldContent = new String(Files.readAllBytes(Paths.get(collectionsPath)));
            String newContent = oldContent.replaceAll("willBeReplaced", nestedJsonPath);
            writer = new FileWriter(fileToBeModified);
            writer.write(newContent);
        } catch (Exception e) {
            System.out.println();
        } finally {
            writer.close();
        }
    }

    @Test
    public void testReadAndEvalJSON() throws Exception {
        Document dom = getAsJSoup("ogc/stac/collections/SENTINEL2?f=html");

        assertEquals("attribute1 => 1 attribute2 => 2", dom.select("h2").text());
    }

    @Test
    public void testReadFileOutsideOfDataDir() throws Exception {
        Document dom = getAsJSoup("ogc/stac/collections?f=html");

        // should return an empty element since collections.ftl loadJSON will not evaluate the file
        // outside of data dir
        assertEquals("", dom.select("h2").text());
    }
}

/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.importer.transform.PostScriptTransform;
import org.geoserver.platform.GeoServerExtensionsHelper;

public class PostScriptTransformTest extends TransformTestSupport {

    GeoServerDataDirectory dd;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        File tempDirectory = Files.createTempDirectory("postScriptTest").toFile();
        dd = new GeoServerDataDirectory(tempDirectory);
        GeoServerExtensionsHelper.singleton("dataDirectory", dd, GeoServerDataDirectory.class);

        // write out a simple shell script in the data dir and make it executable
        File scripts = dd.findOrCreateDir("importer", "scripts");
        File script = new File(scripts, "test.sh");
        FileUtils.writeStringToFile(script, "touch test.properties\n", "UTF-8");
        script.setExecutable(true, true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        File dir = dd.getRoot().dir();
        FileUtils.deleteQuietly(dir);
    }

    public void testJSON() throws Exception {
        doJSONTest(new PostScriptTransform("test.sh", null));
        doJSONTest(new PostScriptTransform("test.sh", Arrays.asList("abcd")));
    }
}

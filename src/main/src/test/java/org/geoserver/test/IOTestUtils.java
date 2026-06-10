/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** IOUtils to aid in staging data for test cases. */
public class IOTestUtils {

    /**
     * Creates a directory as a child of baseDir. The directory name will be preceded by prefix and followed by suffix
     */
    public static File createRandomDirectory(String baseDir, String prefix) throws IOException {
        Path tmpPath = java.nio.file.Files.createTempDirectory(Paths.get(baseDir), prefix);

        return tmpPath.toFile();
    }
}

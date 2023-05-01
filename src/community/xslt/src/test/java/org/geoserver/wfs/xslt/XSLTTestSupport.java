/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.geoserver.wfs.WFSTestSupport;

public class XSLTTestSupport extends WFSTestSupport {

    public static void deleteDirectory(File directory) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            // silly I know, but under Windows not even commons can detect if a directory
            // was really removed...
            long start = System.currentTimeMillis();
            while (directory.exists() && (System.currentTimeMillis() - start) < 10000) {
                // we do a quiet remove because during the deletion a file can be reported
                // as existing, and then disappear before Java can try to remove it...
                FileUtils.deleteQuietly(directory);
            }
            if (directory.exists()) {
                throw new IOException(
                        "Could not remove directory "
                                + directory.getPath()
                                + " after repeated attempts");
            }
        } else {
            // aaah, sanity
            FileUtils.deleteDirectory(directory);
        }
    }
}

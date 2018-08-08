/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** An aid for ZIP related testing. */
public class ZipTestUtil {

    private ZipTestUtil() {}

    public static File initZipSlipFile(File file) throws IOException {
        Files.copy(getZipSlipInput(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return file;
    }

    public static ByteArrayInputStream getZipSlipInput() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("good.txt"));
            zip.write("good file".getBytes());
            zip.closeEntry();
            zip.putNextEntry(
                    new ZipEntry(
                            "../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../tmp/evil.txt"));
            zip.write("evil file".getBytes());
            zip.closeEntry();
        }
        return new ByteArrayInputStream(output.toByteArray());
    }
}

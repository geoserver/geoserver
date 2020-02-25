/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * This class returns a zip encoded results of the users's query.
 *
 * <p>Currently supported type of values are instances of {@link File} or {@link List} of {@link
 * File}.
 */
public class ZipOutputFormat extends Response {

    private static final Logger LOGGER = Logging.getLogger(ZipOutputFormat.class);

    public ZipOutputFormat() {
        super(List.class);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/zip";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        File tempDir = IOUtils.createTempDirectory("ziptemp");
        if (value == null) {
            throw new ServiceException("No value to be written has been provided");
        }
        try {
            List<File> files = null;
            if (value instanceof List) {
                files = (List<File>) value;
            } else if (value instanceof File) {
                files = Collections.singletonList((File) value);
            } else {
                throw new IllegalArgumentException(value.getClass() + " type isn't supported yet");
            }

            // Copying files to the temp folder
            for (File file : files) {
                FileUtils.copyFile(file, new File(tempDir, file.getName()));
            }
            ZipOutputStream zipOut = new ZipOutputStream(output);
            IOUtils.zipDirectory(tempDir, zipOut, null);
            zipOut.finish();
        } finally {
            // make sure we remove the temp directory and its contents completely now
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                LOGGER.warning(
                        "Could not delete temp directory: "
                                + tempDir.getAbsolutePath()
                                + " due to: "
                                + e.getMessage());
            }
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        File file = ((List<File>) value).get(0);
        // Use the first file as reference. That should be the main file name
        String filename = FilenameUtils.getBaseName(file.getAbsolutePath());
        return filename + (filename.endsWith(".zip") ? "" : ".zip");
    }
}

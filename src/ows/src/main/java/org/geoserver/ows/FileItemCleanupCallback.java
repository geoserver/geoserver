/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.fileupload.FileItem;
import org.geotools.util.logging.Logging;

/**
 * Dispatcher callback to ensure that all uploaded files are deleted at the end of a
 * multipart/form-data request instead of relying on the garbage collector to delete the files
 * through the {@link org.apache.commons.fileupload.disk.DiskFileItem#finalize()} method.
 */
public class FileItemCleanupCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(FileItemCleanupCallback.class);

    private static final ThreadLocal<List<FileItem>> FILE_ITEMS =
            ThreadLocal.withInitial(Collections::emptyList);

    public static void setFileItems(List<FileItem> fileItems) {
        FILE_ITEMS.set(fileItems);
    }

    @Override
    public void finished(Request request) {
        List<FileItem> items = FILE_ITEMS.get();
        FILE_ITEMS.remove();
        if (!items.isEmpty()) {
            try (Reader r = request.getInput()) {
                // just closing the input which may be pointing to a temp file
            } catch (Exception e) {
                LOGGER.log(Level.FINEST, "Unable to close request input", e);
            }
            // delete all of the temp file uploads for this request
            items.forEach(FileItem::delete);
        }
    }
}

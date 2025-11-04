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
import org.apache.commons.fileupload2.core.FileItem;
import org.geotools.util.logging.Logging;

/**
 * Dispatcher callback to ensure that all uploaded files are deleted at the end of a multipart/form-data request instead
 * of relying on GC.
 */
public class FileItemCleanupCallback extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(FileItemCleanupCallback.class);

    // Hold any concrete List<? extends FileItem<?>>, e.g., List<DiskFileItem>
    private static final ThreadLocal<List<? extends FileItem<?>>> FILE_ITEMS =
            ThreadLocal.withInitial(Collections::<FileItem<?>>emptyList);

    // Accept any concrete list of FileItem implementations
    public static void setFileItems(List<? extends FileItem<?>> fileItems) {
        FILE_ITEMS.set(fileItems);
    }

    @Override
    public void finished(Request request) {
        final List<? extends FileItem<?>> items = FILE_ITEMS.get();
        FILE_ITEMS.remove();

        if (!items.isEmpty()) {
            //noinspection EmptyTryBlock
            try (Reader ignored = request.getInput()) {
                // just closing the input which may be pointing to a temp file
            } catch (Exception e) {
                LOGGER.log(Level.FINEST, "Unable to close request input", e);
            }
            // delete all the temp file uploads for this request
            for (FileItem<?> item : items) {
                try {
                    item.delete();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Unable to delete uploaded file item: " + item.getName(), e);
                }
            }
        }
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;

/**
 * Utility class for XStream related utilities
 *
 * @author Andrea Aime - TOPP
 */
public final class XStreamUtils {
    /**
     * Performs serialization with an {@link XStreamPersister} in a safe manner in which a temp file
     * is used for the serialization so that the true destination file is not partially written in
     * the case of an error.
     *
     * @param f The file to write to, only modified if the temp file serialization was error free.
     * @param obj The object to serialize.
     * @param xp The persister.
     */
    public static void xStreamPersist(File f, Object obj, XStreamPersister xp) throws IOException {
        // first save to a temp file
        final File temp = File.createTempFile(f.getName(), null, f.getParentFile());

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp))) {
            xp.save(obj, out);
            out.flush();
        }

        // no errors, overwrite the original file
        try {
            IOUtils.rename(temp, f);
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    /**
     * Performs serialization with an {@link XStreamPersister} in a safe manner in which a temp file
     * is used for the serialization so that the true destination file is not partially written in
     * the case of an error.
     *
     * @param r The resource to write to, only modified if the temp file serialization was error
     *     free.
     * @param obj The object to serialize.
     * @param xp The persister.
     */
    public static void xStreamPersist(Resource r, Object obj, XStreamPersister xp)
            throws IOException {

        try (OutputStream out = r.out()) {
            xp.save(obj, out);
            out.flush();
        }
    }
}

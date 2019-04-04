/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.util;

import java.io.File;
import java.io.IOException;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamUtils;
import org.geoserver.platform.resource.Resource;

/**
 * Utility class for IO related utilities
 *
 * @author Andrea Aime - TOPP
 * @deprecated use {@link org.geoserver.util.IOUtils} instead
 */
public final class IOUtils extends org.geoserver.util.IOUtils {
    private IOUtils() {
        super();
    }

    /**
     * Performs serialization with an {@link XStreamPersister} in a safe manner in which a temp file
     * is used for the serialization so that the true destination file is not partially written in
     * the case of an error.
     *
     * @deprecated use {@link org.geoserver.config.util.XStreamPersist(File f, Object obj,
     *     XStreamPersister xp)} instead
     * @param f The file to write to, only modified if the temp file serialization was error free.
     * @param obj The object to serialize.
     * @param xp The persister.
     */
    public static void xStreamPersist(File f, Object obj, XStreamPersister xp) throws IOException {
        XStreamUtils.xStreamPersist(f, obj, xp);
    }

    /**
     * Performs serialization with an {@link XStreamPersister} in a safe manner in which a temp file
     * is used for the serialization so that the true destination file is not partially written in
     * the case of an error.
     *
     * @deprecated use {@link org.geoserver.config.util.XStreamPersist(Resource r, Object obj,
     *     XStreamPersister xp)} instead
     * @param r The resource to write to, only modified if the temp file serialization was error
     *     free.
     * @param obj The object to serialize.
     * @param xp The persister.
     */
    public static void xStreamPersist(Resource r, Object obj, XStreamPersister xp)
            throws IOException {
        XStreamUtils.xStreamPersist(r, obj, xp);
    }
}

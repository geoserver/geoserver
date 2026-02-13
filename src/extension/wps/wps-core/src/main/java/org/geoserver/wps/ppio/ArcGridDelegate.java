/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.resource.GridCoverageReaderResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.parameter.Parameter;

/** Isolated from the PPIO so the JVM doesn't load these imports until this specific class is initialized. */
class ArcGridDelegate {
    public static Object decode(InputStream input, WPSResourceManager resources) throws Exception {
        // in order to read a grid coverage we need to first store it on disk
        File root = new File(System.getProperty("java.io.tmpdir", "."));
        File f = File.createTempFile("wps", "asc", root);
        GridCoverageReaderResource resource = null;
        try {
            FileUtils.copyInputStreamToFile(input, f);
            ArcGridFormat format = new ArcGridFormat();
            if (!format.accepts(f)) {
                throw new WPSException("Could not read application/arcgrid coverage");
            }
            ArcGridReader reader = format.getReader(f);
            resource = new GridCoverageReaderResource(reader, f);
            return reader.read();
        } finally {
            if (resource != null) {
                resources.addResource(resource);
            } else {
                f.delete();
            }
        }
    }

    public static void encode(Object value, OutputStream os) throws IOException {
        Parameter<Boolean> forceSquareCells = new Parameter<>(ArcGridFormat.FORCE_CELLSIZE, Boolean.TRUE);
        new ArcGridFormat().getWriter(os).write((GridCoverage2D) value, forceSquareCells);
    }
}

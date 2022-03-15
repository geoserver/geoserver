/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.ByteArrayInputStream;
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

/**
 * Decodes/encodes an ArcGrid file
 *
 * @author Andrea Aime - OpenGeo
 */
public class ArcGridPPIO extends CDataPPIO {

    private final WPSResourceManager resources;

    protected ArcGridPPIO(WPSResourceManager resources) {
        super(GridCoverage2D.class, GridCoverage2D.class, "application/arcgrid");
        this.resources = resources;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // in order to read a grid coverage we need to first store it on disk
        File root = new File(System.getProperty("java.io.tmpdir", "."));
        File f = File.createTempFile("wps", "asc", root);
        GridCoverageReaderResource resource = null;
        try {
            FileUtils.copyInputStreamToFile(input, f);
            ArcGridFormat format = new ArcGridFormat();
            if (!format.accepts(f)) {
                throw new WPSException("Could not read " + getMimeType() + " coverage");
            }
            ArcGridReader reader = format.getReader(f);
            resource = new GridCoverageReaderResource(reader, f);
            return reader.read(null);
        } finally {
            if (resource != null) {
                resources.addResource(resource);
            } else {
                f.delete();
            }
        }
    }

    @Override
    public Object decode(String arcgrid) throws Exception {
        // if the user forgot to add the final newline let's just add it
        if (!arcgrid.endsWith("\n")) {
            arcgrid += "\n";
        }
        return decode(new ByteArrayInputStream(arcgrid.getBytes()));
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        Parameter<Boolean> forceSquareCells =
                new Parameter<>(ArcGridFormat.FORCE_CELLSIZE, Boolean.TRUE);
        new ArcGridFormat().getWriter(os).write((GridCoverage2D) value, forceSquareCells);
    }

    @Override
    public String getFileExtension() {
        return "asc";
    }
}

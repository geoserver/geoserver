/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.wcs.responses.GeoTiffWriterHelper;
import org.geoserver.wps.WPSException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.process.ProcessException;

/**
 * Decodes/encodes a GeoTIFF file
 * 
 * @author Andrea Aime - OpenGeo
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
public class GeoTiffPPIO extends BinaryPPIO {

    protected GeoTiffPPIO() {
        super(GridCoverage2D.class, GridCoverage2D.class, "image/tiff");
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // in order to read a grid coverage we need to first store it on disk
        File root = new File(System.getProperty("java.io.tmpdir", "."));
        File f = File.createTempFile("wps", "tiff", root);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(f);
            IOUtils.copy(input, os);
        } finally {
            IOUtils.closeQuietly(os);
        }

        // and then we try to read it as a geotiff
        AbstractGridFormat format = GridFormatFinder.findFormat(f);
        if (format instanceof UnknownFormat) {
            throw new WPSException(
                    "Could not find the GeoTIFF GT2 format, please check it's in the classpath");
        }
        return format.getReader(f).read(null);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        GridCoverage2D coverage = (GridCoverage2D) value;
        GeoTiffWriterHelper helper = new GeoTiffWriterHelper(coverage);

        try {
            helper.write(os);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public String getFileExtension() {
        return "tiff";
    }

}

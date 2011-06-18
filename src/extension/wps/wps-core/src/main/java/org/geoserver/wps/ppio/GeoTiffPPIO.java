/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.media.jai.JAI;

import org.apache.commons.io.IOUtils;
import org.geoserver.wps.WPSException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverageWriter;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Decodes/encodes a GeoTIFF file
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class GeoTiffPPIO extends BinaryPPIO {

    private final static GeoTiffWriteParams DEFAULT_WRITE_PARAMS;

    static {
        // setting the write parameters (write out using tiling)
        DEFAULT_WRITE_PARAMS = new GeoTiffWriteParams();
        DEFAULT_WRITE_PARAMS.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        final Dimension defaultTileSize = JAI.getDefaultTileSize();
        DEFAULT_WRITE_PARAMS.setTiling(defaultTileSize.width, defaultTileSize.height);
    }

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

        GeoTiffFormat format = new GeoTiffFormat();
        final ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
        params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(
                DEFAULT_WRITE_PARAMS);
        final GeneralParameterValue[] wps = (GeneralParameterValue[]) params.values().toArray(
                new GeneralParameterValue[1]);
        // write out the coverage
        AbstractGridCoverageWriter writer = (AbstractGridCoverageWriter) format.getWriter(os);
        if (writer == null)
            throw new WPSException(
                    "Could not find the GeoTIFF writer, please check it's in the classpath");
        try {
            writer.write(coverage, wps);
        } finally {
            try {
                writer.dispose();
            } catch (Exception e) {
                // swallow
            }
        }
    }

}

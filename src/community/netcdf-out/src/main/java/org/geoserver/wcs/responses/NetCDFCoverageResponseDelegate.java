/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.util.logging.Logging;

import ucar.ma2.InvalidRangeException;

/**
 * {@link CoverageResponseDelegate} implementation for NetCDF multidimensional Grids
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class NetCDFCoverageResponseDelegate extends BaseCoverageResponseDelegate implements
        CoverageResponseDelegate {

    private final static String MIME_TYPE = "application/x-netcdf";

    public static final Logger LOGGER = Logging.getLogger("org.geoserver.wcs.responses.NetCDFCoverageResponseDelegate");

    @SuppressWarnings("serial")
    public NetCDFCoverageResponseDelegate(GeoServer geoserver) {
        super(geoserver, Arrays.asList("NetCDF"/* ,"netcdf-GZIP" */), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put("NetCDF", "nc");
                        put("netcdf", "nc");
                        put("NETCDF", "nc");
                        put(MIME_TYPE, "nc");
                    }
                }, new HashMap<String, String>() { // mime types
                    {
                        put("NetCDF", MIME_TYPE);
                        put("netcdf", MIME_TYPE);
                        put("NETCDF", MIME_TYPE);
                    }
                });
    }

//    private boolean isOutputCompressed(String outputFormat) {
//        return false;// "NetCDF-GZIP".equalsIgnoreCase(outputFormat) || "application/netcdf;gzipped=\"true\"".equals(outputFormat);
//    }

    public void encode(GridCoverage2D sourceCoverage, String outputFormat,
            Map<String, String> econdingParameters, OutputStream output) throws ServiceException,
            IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException(new StringBuffer(
                    "It seems prepare() has not been called").append(" or has not succeed")
                    .toString());
        }
        if (!(sourceCoverage instanceof GranuleStack)) {
            throw new IllegalArgumentException(
                    "NetCDF encoding only supports granuleStack coverages");
        }
        GranuleStack granuleStack = (GranuleStack) sourceCoverage;

        // GZIPOutputStream gzipOut = null;
        // if (isOutputCompressed(outputFormat)) {
        // gzipOut = new GZIPOutputStream(output);
        // output = gzipOut;
        // }
        File tempFile = null;
        NetCDFOutputManager manager = null;
        try {
            tempFile = File.createTempFile("tempNetCDF", ".nc");
            manager = new NetCDFOutputManager(granuleStack, tempFile);
            manager.write();
            streamBack(tempFile, output);

        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            sourceCoverage.dispose(true);
            boolean deleted = FileUtils.deleteQuietly(tempFile);
            if (!deleted) {
                LOGGER.warning("Could not delete temp file: " + tempFile.getAbsolutePath());
            }
            manager.close();
        }
    }

    /**
     * Stream back the content of the temporary file to the output stream
     * @param file the temporary file containing the NetCDF output.
     * @param output the outputStream where to write the output
     * @throws IOException
     */
    private void streamBack(final File file, final OutputStream output) throws IOException {
        final byte[] buffer = new byte[8 * 1024];

        if (file.exists()) {
            final InputStream in = new FileInputStream(file);
            int c;
            try {
                while (-1 != (c = in.read(buffer))) {
                    output.write(buffer, 0, c);
                }

            } finally {
                in.close();
            }
        }
        output.flush();

    }

}

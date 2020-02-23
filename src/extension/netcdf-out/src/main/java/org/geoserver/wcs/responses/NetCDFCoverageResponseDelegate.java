/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ucar.ma2.InvalidRangeException;

/**
 * {@link CoverageResponseDelegate} implementation for NetCDF multidimensional Grids
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class NetCDFCoverageResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate, ApplicationContextAware {

    public static final Logger LOGGER =
            Logging.getLogger("org.geoserver.wcs.responses.NetCDFCoverageResponseDelegate");
    private List<NetCDFEncoderFactory> encoderFactories;

    @SuppressWarnings("serial")
    public NetCDFCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList(NetCDFUtilities.NETCDF), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put(NetCDFUtilities.NETCDF, "nc");
                        put(NetCDFUtilities.NETCDF.toLowerCase(), "nc");
                        put(NetCDFUtilities.NETCDF.toUpperCase(), "nc");
                        put(NetCDFUtilities.NETCDF3_MIMETYPE, "nc");
                        put(NetCDFUtilities.NETCDF4_MIMETYPE, "nc");
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put(NetCDFUtilities.NETCDF, NetCDFUtilities.NETCDF3_MIMETYPE);
                        put(NetCDFUtilities.NETCDF.toLowerCase(), NetCDFUtilities.NETCDF3_MIMETYPE);
                        put(NetCDFUtilities.NETCDF.toUpperCase(), NetCDFUtilities.NETCDF3_MIMETYPE);
                        put(NetCDFUtilities.NETCDF4, NetCDFUtilities.NETCDF4_MIMETYPE);
                        put(
                                NetCDFUtilities.NETCDF4.toLowerCase(),
                                NetCDFUtilities.NETCDF4_MIMETYPE);
                        put(
                                NetCDFUtilities.NETCDF4.toUpperCase(),
                                NetCDFUtilities.NETCDF4_MIMETYPE);
                    }
                });
    }

    public void encode(
            GridCoverage2D sourceCoverage,
            String outputFormat,
            Map<String, String> encodingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        GranuleStack granuleStack = toGranuleStack(sourceCoverage);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("tempNetCDF", ".nc");
            for (NetCDFEncoderFactory factory : encoderFactories) {
                NetCDFEncoder encoder =
                        factory.getEncoderFor(
                                granuleStack, tempFile, encodingParameters, outputFormat);
                if (encoder != null) {
                    encoder.write();
                    encoder.close();
                    break;
                }
            }
            streamBack(tempFile, output);

        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            sourceCoverage.dispose(true);
            boolean deleted = FileUtils.deleteQuietly(tempFile);
            if (!deleted) {
                LOGGER.warning("Could not delete temp file: " + tempFile.getAbsolutePath());
            }
        }
    }

    public GranuleStack toGranuleStack(GridCoverage2D sourceCoverage) {
        if (sourceCoverage == null) {
            throw new IllegalStateException(
                    new StringBuffer("It seems prepare() has not been called")
                            .append(" or has not succeed")
                            .toString());
        }
        if (!(sourceCoverage instanceof GranuleStack)) {
            throw new IllegalArgumentException(
                    "NetCDF encoding only supports granuleStack coverages");
        }
        return (GranuleStack) sourceCoverage;
    }

    /**
     * Stream back the content of the temporary file to the output stream
     *
     * @param file the temporary file containing the NetCDF output.
     * @param output the outputStream where to write the output
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.encoderFactories = GeoServerExtensions.extensions(NetCDFEncoderFactory.class);
    }

    @Override
    public String getFileName(GridCoverage2D value, String coverageId, String format) {
        GranuleStack granuleStack = toGranuleStack(value);

        for (NetCDFEncoderFactory factory : encoderFactories) {
            String fileName = factory.getOutputFileName(granuleStack, coverageId, format);
            if (fileName != null) {
                return fileName;
            }
        }

        return super.getFileName(value, coverageId, format);
    }
}

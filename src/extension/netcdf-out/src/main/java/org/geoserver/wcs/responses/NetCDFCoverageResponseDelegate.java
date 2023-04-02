/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import static java.util.Map.entry;
import static org.geotools.imageio.netcdf.utilities.NetCDFUtilities.NETCDF;
import static org.geotools.imageio.netcdf.utilities.NetCDFUtilities.NETCDF3_MIMETYPE;
import static org.geotools.imageio.netcdf.utilities.NetCDFUtilities.NETCDF4;
import static org.geotools.imageio.netcdf.utilities.NetCDFUtilities.NETCDF4_MIMETYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.wcs2_0.response.MultidimensionalCoverageResponse;
import org.geotools.coverage.grid.GridCoverage2D;
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
        implements CoverageResponseDelegate,
                ApplicationContextAware,
                MultidimensionalCoverageResponse {

    public static final Logger LOGGER =
            Logging.getLogger("org.geoserver.wcs.responses.NetCDFCoverageResponseDelegate");
    private List<NetCDFEncoderFactory> encoderFactories;

    @SuppressWarnings("serial")
    public NetCDFCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList(NETCDF, NETCDF4),
                Map.ofEntries(
                        entry(NETCDF, "nc"),
                        entry(NETCDF.toLowerCase(), "nc"),
                        entry(NETCDF.toUpperCase(), "nc"),
                        entry(NETCDF3_MIMETYPE, "nc"),
                        entry(NETCDF4_MIMETYPE, "nc")),
                Map.ofEntries(
                        entry(NETCDF, NETCDF3_MIMETYPE),
                        entry(NETCDF.toLowerCase(), NETCDF3_MIMETYPE),
                        entry(NETCDF.toUpperCase(), NETCDF3_MIMETYPE),
                        entry(NETCDF4, NETCDF4_MIMETYPE),
                        entry(NETCDF4.toLowerCase(), NETCDF4_MIMETYPE),
                        entry(NETCDF4.toUpperCase(), NETCDF4_MIMETYPE)));
    }

    @Override
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
            try (InputStream in = new FileInputStream(file)) {
                int c;
                while (-1 != (c = in.read(buffer))) {
                    output.write(buffer, 0, c);
                }
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

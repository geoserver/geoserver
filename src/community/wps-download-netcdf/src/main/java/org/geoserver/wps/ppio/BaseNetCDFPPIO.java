/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.geoserver.wcs.responses.AbstractNetCDFEncoder.COMPRESSION_KEY;
import static org.geoserver.wcs.responses.AbstractNetCDFEncoder.COPY_GLOBAL_ATTRIBUTES_KEY;
import static org.geoserver.wcs.responses.AbstractNetCDFEncoder.COPY_VARIABLE_ATTRIBUTES_KEY;
import static org.geoserver.wcs.responses.AbstractNetCDFEncoder.DATA_PACKING_KEY;
import static org.geoserver.wcs.responses.AbstractNetCDFEncoder.SHUFFLE_KEY;
import static org.geoserver.wcs.responses.AbstractNetCDFEncoder.UOM_KEY;
import static org.geoserver.wcs.responses.AbstractNetCDFEncoder.VARIABLE_NAME_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.responses.NetCDFCoverageResponseDelegate;
import org.geoserver.wcs2_0.response.WCS20GetCoverageResponse;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.process.ProcessException;

/**
 * NetCDF PPIO that encodes a GranuleStack into a NetCDF output. At the moment: - No decoding capability is supported.
 */
public abstract class BaseNetCDFPPIO extends BinaryPPIO {

    private static final Set<String> SUPPORTED_PARAMS = new HashSet<>();

    static {
        SUPPORTED_PARAMS.add(SHUFFLE_KEY);
        SUPPORTED_PARAMS.add(DATA_PACKING_KEY);
        SUPPORTED_PARAMS.add(COMPRESSION_KEY);
        SUPPORTED_PARAMS.add(VARIABLE_NAME_KEY);
        SUPPORTED_PARAMS.add(UOM_KEY);
        SUPPORTED_PARAMS.add(COPY_GLOBAL_ATTRIBUTES_KEY);
        SUPPORTED_PARAMS.add(COPY_VARIABLE_ATTRIBUTES_KEY);
        SUPPORTED_PARAMS.add(WCS20GetCoverageResponse.COVERAGE_ID_PARAM);
    }

    /** NetCDF PPIO */
    public static class NetCDF3PPIO extends BaseNetCDFPPIO {

        public NetCDF3PPIO() {
            super(NetCDFUtilities.NETCDF3_MIMETYPE);
        }
    }

    /** NetCDF-4 Specific PPIO */
    public static class NetCDF4PPIO extends BaseNetCDFPPIO {

        public NetCDF4PPIO() {
            super(NetCDFUtilities.NETCDF4_MIMETYPE);
        }
    }

    private NetCDFCoverageResponseDelegate delegate;

    protected BaseNetCDFPPIO(String mimeType) {
        super(GridCoverage2D.class, GridCoverage2D.class, mimeType);
        this.delegate = GeoServerExtensions.bean(NetCDFCoverageResponseDelegate.class);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        encode(value, Collections.emptyMap(), os);
    }

    @Override
    public void encode(Object value, Map<String, Object> encodingParameters, OutputStream os) throws Exception {
        GridCoverage2D coverage = (GridCoverage2D) value;
        try {
            Map<String, String> params = encodingParameters.entrySet().stream()
                    .filter(e -> SUPPORTED_PARAMS.contains(e.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue() != null ? e.getValue().toString() : null));
            delegate.encode(coverage, getMimeType(), params, os);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
    }

    @Override
    public String getFileExtension() {
        return "nc";
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("The NetCDFPPIO only supports encoding.");
    }
}

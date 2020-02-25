/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs2_0.response.WCS20GetCoverageResponse;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import ucar.ma2.InvalidRangeException;

/**
 * Writes out a NetCDF file (the write parameters are provided during construction, see {@link
 * NetCDFEncoderFactory}
 */
public interface NetCDFEncoder {

    /** Writes out the NetCDF file */
    void write() throws IOException, InvalidRangeException;

    /** Close and release resources */
    void close();

    /** Extracts the NetCDF encoding settings out of the encoding parameters */
    static NetCDFLayerSettingsContainer getSettings(Map<String, String> encodingParameters) {
        Set<String> keys = encodingParameters.keySet();
        if (keys != null
                && !keys.isEmpty()
                && keys.contains(WCS20GetCoverageResponse.COVERAGE_ID_PARAM)) {
            String coverageId = encodingParameters.get(WCS20GetCoverageResponse.COVERAGE_ID_PARAM);
            if (coverageId != null) {
                return getSettings(coverageId);
            }
        }

        return null;
    }

    /** Extracts the NetCDF encoding settings from the coverage identifier */
    static NetCDFLayerSettingsContainer getSettings(String coverageId) {
        GeoServer geoserver = GeoServerExtensions.bean(GeoServer.class);
        MetadataMap map = null;
        if (geoserver != null) {
            Catalog gsCatalog = geoserver.getCatalog();
            LayerInfo info = NCNameResourceCodec.getCoverage(gsCatalog, coverageId);
            if (info != null) {
                map = info.getResource().getMetadata();
            }
        }
        if (map != null
                && !map.isEmpty()
                && map.containsKey(NetCDFSettingsContainer.NETCDFOUT_KEY)) {
            NetCDFLayerSettingsContainer settings =
                    (NetCDFLayerSettingsContainer)
                            map.get(
                                    NetCDFSettingsContainer.NETCDFOUT_KEY,
                                    NetCDFLayerSettingsContainer.class);
            return settings;
        }

        return null;
    }
}

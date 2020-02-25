/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geotools.util.DateRange;

/** Returns a GHRSST custom encoder if the configuration has GHRSST encoding enabled */
public class GHRSSTEncoderFactory implements NetCDFEncoderFactory {

    @Override
    public NetCDFEncoder getEncoderFor(
            GranuleStack granuleStack,
            File file,
            Map<String, String> encodingParameters,
            String outputFormat)
            throws IOException {
        NetCDFLayerSettingsContainer settings = NetCDFEncoder.getSettings(encodingParameters);
        if (settings != null
                && Boolean.TRUE.equals(
                        settings.getMetadata().get(GHRSSTEncoder.SETTINGS_KEY, Boolean.class))) {
            return new GHRSSTEncoder(granuleStack, file, encodingParameters, outputFormat);
        }

        // if no GHRSST settings, or disabled, then look for some other encoder
        return null;
    }

    @Override
    public String getOutputFileName(GranuleStack granuleStack, String coverageId, String format) {
        // is the layer configured to have a GHRSST format?
        NetCDFLayerSettingsContainer settings = NetCDFEncoder.getSettings(coverageId);
        MetadataMap metadata = settings.getMetadata();
        if (settings == null
                || !Boolean.TRUE.equals(metadata.get(GHRSSTEncoder.SETTINGS_KEY, Boolean.class))) {
            // nope;
            return null;
        }

        // grab reference date/time
        NetCDFDimensionsManager dimensionsManager = new NetCDFDimensionsManager();
        dimensionsManager.collectCoverageDimensions(granuleStack);
        Date referenceDate = null;
        for (NetCDFDimensionsManager.NetCDFDimensionMapping dimension :
                dimensionsManager.getDimensions()) {
            if ("time".equalsIgnoreCase(dimension.getName())) {
                TreeSet<Object> values =
                        (TreeSet<Object>) dimension.getDimensionValues().getValues();
                Object first = values.first();
                if (first instanceof Date) {
                    referenceDate = (Date) first;
                } else if (first instanceof DateRange) {
                    referenceDate = ((DateRange) first).getMinValue();
                } else {
                    throw new IllegalArgumentException(
                            "Unrecognized data type for reference date: " + first);
                }
            }
        }

        if (referenceDate == null) {
            throw new IllegalArgumentException(
                    "Could not locate a reference date in the input data, a GHRSST file "
                            + "should have a time dimension");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
        String formattedDate = dateFormat.format(referenceDate);

        // yessir, the pattern is
        // <Indicative Date><Indicative Time>-<RDAC>-<Processing Level>_GHRSST-<SST Type>-
        // <Product String>-<Additional Segregator>-v<GDS Version>-fv<File Version>.<File Type>
        // The default values in the following are setup to build a valid filename using a specific
        // case,
        // unfortunately there are not valid entries for unkonwns
        StringBuilder sb = new StringBuilder();
        sb.append(formattedDate);
        sb.append("-");
        sb.append(getConfiguration(metadata, GHRSSTEncoder.SETTINGS_RDAC_KEY, "EUR"));
        sb.append("-");
        sb.append(getConfiguration(metadata, GHRSSTEncoder.SETTINGS_PROCESSING_LEVEL_KEY, "L3U"));
        sb.append("_GHRSST-");
        sb.append(getConfiguration(metadata, GHRSSTEncoder.SETTINGS_SST_TYPE, "SSTint"));
        sb.append("-");
        sb.append(
                getConfiguration(metadata, GHRSSTEncoder.SETTINGS_PRODUCT_STRING, "AVHRR_METOP_A"));
        // additional segregator is optional, not needed here
        sb.append("-v02.0"); // GHRSST specification version
        sb.append("-fv01.0.nc");

        return sb.toString();
    }

    private String getConfiguration(MetadataMap metadata, String key, String defaultValue) {
        String value = metadata.get(key, String.class);
        return value == null ? defaultValue : value;
    }
}

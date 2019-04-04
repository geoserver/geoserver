/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContent;
import org.geotools.util.Converters;

/** @see RawMapResponse */
public class OpenLayersMapOutputFormat implements GetMapOutputFormat {

    /** The mime type for the response header */
    public static final String MIME_TYPE = "text/html; subtype=openlayers";

    /** System property name to toggle OL3 support. */
    public static final String ENABLE_OL3 = "ENABLE_OL3";

    /** The formats accepted in a GetMap request for this producer and stated in getcaps */
    private static final Set<String> OUTPUT_FORMATS =
            new HashSet<String>(Arrays.asList("application/openlayers", "openlayers", MIME_TYPE));

    private final OpenLayers2MapOutputFormat ol2Format;
    private final OpenLayers3MapOutputFormat ol3Format;

    public OpenLayersMapOutputFormat(
            OpenLayers2MapOutputFormat ol2Format, OpenLayers3MapOutputFormat ol3Format) {
        this.ol2Format = ol2Format;
        this.ol3Format = ol3Format;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames() */
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#getMimeType() */
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return AbstractOpenLayersMapOutputFormat.CAPABILITIES;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    public RawMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        if (isOL3Enabled(mapContent) && ol3Format.browserSupportsOL3(mapContent)) {
            return ol3Format.produceMap(mapContent);
        } else {
            return ol2Format.produceMap(mapContent);
        }
    }

    protected boolean isOL3Enabled(WMSMapContent mapContent) {
        GetMapRequest req = mapContent.getRequest();

        // check format options
        Object enableOL3 =
                Converters.convert(req.getFormatOptions().get(ENABLE_OL3), Boolean.class);
        if (enableOL3 == null) {
            // check system property
            enableOL3 = GeoServerExtensions.getProperty(ENABLE_OL3);
        }

        // enable by default
        return enableOL3 == null || Converters.convert(enableOL3, Boolean.class);
    }
}

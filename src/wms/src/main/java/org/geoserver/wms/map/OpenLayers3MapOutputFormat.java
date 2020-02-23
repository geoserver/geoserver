/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

/**
 * Subclass of {@link OpenLayersMapOutputFormat} allowing to explictly request a OpenLayers 3 client
 */
public class OpenLayers3MapOutputFormat extends AbstractOpenLayersMapOutputFormat {

    /** The freemarker template for OL3 */
    static final String OL3_TEMPLATE_FTL = "OpenLayers3MapTemplate.ftl";

    /** Format name for OL3 preview */
    public static final String OL3_FORMAT = "application/openlayers3";

    /** The mime type for the response header */
    public static final String MIME_TYPE = "text/html; subtype=openlayers3";

    /** The formats accepted in a GetMap request for this producer and stated in getcaps */
    private static final Set<String> OUTPUT_FORMATS =
            new HashSet<>(Arrays.asList(OL3_FORMAT, MIME_TYPE));

    public OpenLayers3MapOutputFormat(WMS wms) {
        super(wms);
    }

    @Override
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    protected String getTemplateName(WMSMapContent mapContent) {
        if (!browserSupportsOL3(mapContent)) {
            throw new ServiceException("OpenLayers 3 is not supported on the current browser");
        }
        return OL3_TEMPLATE_FTL;
    }

    protected boolean browserSupportsOL3(WMSMapContent mc) {
        String agent = mc.getRequest().getHttpRequestHeader("USER-AGENT");
        if (agent == null) {
            // play it safe
            return false;
        }

        Pattern MSIE_PATTERN = Pattern.compile(".*MSIE (\\d+)\\..*");
        Matcher matcher = MSIE_PATTERN.matcher(agent);
        if (!matcher.matches()) {
            return true;
        } else {
            return Integer.valueOf(matcher.group(1)) > 8;
        }
    }

    /**
     * OL3 does support a very limited set of unit types, we have to try and return one of those,
     * otherwise the scale won't be shown.
     */
    @Override
    protected String getUnits(WMSMapContent mapContent) {
        CoordinateReferenceSystem crs = mapContent.getRequest().getCrs();
        // first rough approximation, meters for projected CRS, degrees for the
        // others
        String result = crs instanceof ProjectedCRS ? "m" : "degrees";
        try {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            // use the unicode escape sequence for the degree sign so its not
            // screwed up by different local encodings
            if ("ft".equals(unit) || "feets".equals(unit)) result = "feet";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }
        return result;
    }
}

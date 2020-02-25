/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

/**
 * Subclass of {@link OpenLayersMapOutputFormat} allowing to explictly request a OpenLayers 2 client
 */
public class OpenLayers2MapOutputFormat extends AbstractOpenLayersMapOutputFormat {

    /** The freemarker template for OL2 */
    static final String OL2_TEMPLATE_FTL = "OpenLayers2MapTemplate.ftl";

    /** Format name for OL2 preview */
    public static final String OL2_FORMAT = "application/openlayers2";

    /** The mime type for the response header */
    public static final String MIME_TYPE = "text/html; subtype=openlayers2";

    /** The formats accepted in a GetMap request for this producer and stated in getcaps */
    private static final Set<String> OUTPUT_FORMATS =
            new HashSet<>(Arrays.asList(OL2_FORMAT, MIME_TYPE));

    public OpenLayers2MapOutputFormat(WMS wms) {
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
        return OL2_TEMPLATE_FTL;
    }

    /**
     * OL does support only a limited number of unit types, we have to try and return one of those,
     * otherwise the scale won't be shown. From the OL guide: possible values are "degrees" (or
     * "dd"), "m", "ft", "km", "mi", "inches".
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
            final String degreeSign = "\u00B0";
            if (degreeSign.equals(unit) || "degrees".equals(unit) || "dd".equals(unit))
                result = "degrees";
            else if ("m".equals(unit) || "meters".equals(unit)) result = "m";
            else if ("km".equals(unit) || "kilometers".equals(unit)) result = "mi";
            else if ("in".equals(unit) || "inches".equals(unit)) result = "inches";
            else if ("ft".equals(unit) || "feets".equals(unit)) result = "ft";
            else if ("mi".equals(unit) || "miles".equals(unit)) result = "mi";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }
        return result;
    }
}

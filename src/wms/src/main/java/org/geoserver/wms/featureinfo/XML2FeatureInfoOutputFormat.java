/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import org.geoserver.wms.WMS;

/**
 * A GetFeatureInfo response handler specialized in producing GML 2 data for a GetFeatureInfo
 * request.
 *
 * <p>This class is an alternative to <code>GML2FeatureInfoOutputFormat</code>.
 *
 * @see GML3FeatureInfoOutputFormat
 * @author Alex van den Hoogen (Geodan)
 */
public class XML2FeatureInfoOutputFormat extends GML2FeatureInfoOutputFormat {

    /**
     * The MIME type of the format this response produces: <code>"text/xml"</code>. This is an
     * alternative format for GML2: <code>"application/vnd.ogc.gml"</code>.
     */
    public static final String FORMAT = "text/xml";

    /**
     * Default constructor, sets up the supported output format String.
     *
     * @param wms WMS to use.
     */
    public XML2FeatureInfoOutputFormat(WMS wms) {
        super(wms, FORMAT);
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import org.geoserver.wfs.xml.GML3OutputFormat;

/**
 * A GetFeatureInfo response handler specialized in producing GML 3.1.1 data for a GetFeatureInfo
 * request.
 *
 * <p>This class is an alternative to <code>GML3FeatureInfoOutputFormat</code>.
 *
 * @see GML3FeatureInfoOutputFormat
 * @author Alex van den Hoogen (Geodan)
 */
public class XML311FeatureInfoOutputFormat extends GML3FeatureInfoOutputFormat {

    /**
     * The MIME type of the format this response produces: <code>"text/xml; subtype=gml/3.1.1"
     * </code>. This is an alternative format for GML3: <code>"application/vnd.ogc.gml/3.1.1"</code>
     * .
     */
    public static final String FORMAT = "text/xml; subtype=gml/3.1.1";

    /**
     * Default constructor, sets up the supported output format String.
     *
     * @param outputFormat format to use
     */
    public XML311FeatureInfoOutputFormat(GML3OutputFormat outputFormat) {
        super(outputFormat, FORMAT);
    }
}

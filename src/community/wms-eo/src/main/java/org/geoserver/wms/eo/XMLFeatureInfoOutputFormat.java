/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wms.featureinfo.GML3FeatureInfoOutputFormat;

/**
 * A GetFeatureInfo response handler specialized in producing xml data for a GetFeatureInfo request.
 * It will produce GML 3 data using 'text/xml' as mime type.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class XMLFeatureInfoOutputFormat extends GML3FeatureInfoOutputFormat {

    /** The MIME type of the format this response produces: <code>"text/xml"</code> */
    public static final String FORMAT = "text/xml";

    /** Default constructor, sets up the supported output format string. */
    public XMLFeatureInfoOutputFormat(final GML3OutputFormat outputFormat) {
        super(outputFormat, FORMAT);
    }
}

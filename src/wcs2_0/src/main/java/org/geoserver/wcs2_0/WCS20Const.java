/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import org.geotools.ows.v2_0.OWS;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Some consts useful through the module.
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public class WCS20Const {
    public static final String V201 = "2.0.1"; // current
    public static final String V20 = "2.0.0"; // old and deprecated, but tested by CITE
    public static final String V111 = "1.1.1";
    public static final String V110 = "1.1.0";

    public static final String CUR_VERSION = WCS20Const.V201;

    public static final String SERVICE_NAME = "WCS";

    protected static final String URI_WCS = "http://www.opengis.net/wcs/2.0";

    public static final String OVERVIEW_POLICY_EXTENSION = "OverviewPolicy";

    public static final String OVERVIEW_POLICY_EXTENSION_LOWERCASE =
            OVERVIEW_POLICY_EXTENSION.toLowerCase();

    public static final String OVERVIEW_POLICY_EXTENSION_NAMESPACE =
            "http://www.geoserver.org/WCS_service-extension_overviewpolicy/1.0";

    public static AttributesImpl getDefaultNamespaces() {

        final AttributesImpl attributes = new AttributesImpl();

        attributes.addAttribute("", "xmlns:wcs", "xmlns:wcs", "", URI_WCS);
        attributes.addAttribute("", "xmlns:ows", "xmlns:ows", "", OWS.NAMESPACE);

        //            attributes.addAttribute("", "xmlns:ogc", "xmlns:ogc", "",
        // "http://www.opengis.net/ogc");
        attributes.addAttribute("", "xmlns:gml", "xmlns:gml", "", "http://www.opengis.net/gml/3.2");
        attributes.addAttribute(
                "", "xmlns:gmlcov", "xmlns:gmlcov", "", "http://www.opengis.net/gmlcov/1.0");

        attributes.addAttribute(
                "", "xmlns:xlink", "xmlns:xlink", "", "http://www.w3.org/1999/xlink");
        attributes.addAttribute(
                "", "xmlns:xsi", "xmlns:xsi", "", "http://www.w3.org/2001/XMLSchema-instance");

        return attributes;
    }
}

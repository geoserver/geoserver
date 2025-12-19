/* (c) 2014 - 2016 Open Source Geospatial Foundation. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import javax.xml.namespace.QName;

/** Shared WFS constants across all versions */
public class WFSConstants {

    /** WFS 1.0.0 Namespace */
    public static final String NAMESPACE_1_0_0 = "http://www.opengis.net/wfs";

    /** WFS 1.1.0 Namespace */
    public static final String NAMESPACE_1_1_0 = "http://www.opengis.net/wfs";

    /** WFS 2.0 Namespace */
    public static final String NAMESPACE_2_0 = "http://www.opengis.net/wfs/2.0";

    /** WFS 1.0.0 Schema Location - Basic */
    public static final String CANONICAL_SCHEMA_LOCATION_BASIC_1_0 =
            "http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd";

    /** WFS 1.0.0 Schema Location - Capabilities */
    public static final String CANONICAL_SCHEMA_LOCATION_CAPABILITIES_1_0 =
            "http://schemas.opengis.net/wfs/1.0.0/WFS-capabilities.xsd";

    /** WFS 1.0.0 Schema Location */
    public static final String CANONICAL_SCHEMA_LOCATION_1_0 = "http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd";

    /** WFS 1.1.0 Schema Location */
    public static final String CANONICAL_SCHEMA_LOCATION_1_1 = "http://schemas.opengis.net/wfs/1.1.0/wfs.xsd";

    /** WFS 2.0 Schema Location */
    public static final String CANONICAL_SCHEMA_LOCATION_2_0 = "http://schemas.opengis.net/wfs/2.0/wfs.xsd";

    /** Feature Collection element name for WFS 1.1 */
    public static final QName FEATURECOLLECTION_1_1 = new QName(NAMESPACE_1_1_0, "FeatureCollection");
}

/* 
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock data for {@link Gsml30WfsTest}.
 * 
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class Gsml30MockData extends AbstractAppSchemaMockData {

    public static final String GSML_PREFIX = "gsml";

    public static final String GSML_URI = "urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0";

    public static final String GSML_SCHEMA_LOCATION_URL = "https://www.seegrid.csiro.au/subversion/GeoSciML/branches/3.0.0_rc1_gml3.2/geosciml-core/3.0.0/xsd/geosciml-core.xsd";

    public static final String CGU_PREFIX = "cgu";

    public static final String CGU_URI = "urn:cgi:xmlns:CGI:Utilities:3.0.0";

    public static final String SWE_PREFIX = "swe";

    public static final String SWE_URI = "http://www.opengis.net/swe/1.0/gml32";

    /**
     * Map of namespace prefix to namespace URI.
     */
    @SuppressWarnings("serial")
    private static final Map<String, String> NAMESPACES = Collections
            .unmodifiableMap(new LinkedHashMap<String, String>() {
                {
                    put(GSML_PREFIX, GSML_URI);
                    put(CGU_PREFIX, CGU_URI);
                    put(SWE_PREFIX, SWE_URI);
                    put("gml", "http://www.opengis.net/gml/3.2");
                    put("wfs", "http://www.opengis.net/wfs/2.0");
                    put("xlink", "http://www.w3.org/1999/xlink");
                }
            });

    public Gsml30MockData() {
        super(NAMESPACES);
    }

    /**
     * @see org.geoserver.test.AbstractAppSchemaMockData#addContent()
     */
    @Override
    public void addContent() {
        addFeatureType(GSML_PREFIX, "MappedFeature", "Gsml30WfsTest.xml",
                "Gsml30WfsTest.properties");
    }

}

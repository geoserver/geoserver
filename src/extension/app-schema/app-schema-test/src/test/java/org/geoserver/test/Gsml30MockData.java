/* 
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mock data for {@link Gsml30WfsTest}.
 * 
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class Gsml30MockData extends AbstractAppSchemaMockData {

    public static final String GSML_SCHEMA_LOCATION = "https://www.seegrid.csiro.au/subversion/GeoSciML/branches/3.0.0_rc1_gml3.2/geosciml-core/3.0.0/xsd/geosciml-core.xsd";

    /**
     * Map of namespace prefix to namespace URI.
     */
    @SuppressWarnings("serial")
    private static final Map<String, String> NAMESPACES = Collections
            .unmodifiableMap(new TreeMap<String, String>() {
                {
                    put("cgu", "urn:cgi:xmlns:CGI:Utilities:3.0.0");
                    put("gco", "http://www.isotc211.org/2005/gco");
                    put("gmd", "http://www.isotc211.org/2005/gmd");
                    put("gml", "http://www.opengis.net/gml/3.2");
                    put("gsml", "urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0");
                    put("swe", "http://www.opengis.net/swe/1.0/gml32");
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

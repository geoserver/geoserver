/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mock data for {@link Gsml32BoreholeWfsTest}.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class Gsml32BoreholeIntervalMockData extends AbstractAppSchemaMockData {

    public static final String GSML_SCHEMA_LOCATION =
            "https://www.seegrid.csiro.au/subversion/GeoSciML/branches/3.2.0/schemas/geosciml/3.2/geosciml.xsd";

    public static final String GSMLBH_PREFIX = "gsmlbh";

    protected static final Map<String, String> GSML32_NAMESPACES =
            Collections.unmodifiableMap(
                    new TreeMap<String, String>() {
                        /** serialVersionUID */
                        private static final long serialVersionUID = -4796243306761831446L;

                        {
                            put("cgu", "urn:cgi:xmlns:CGI:Utilities:3.0.0");
                            put("gco", "http://www.isotc211.org/2005/gco");
                            put("gmd", "http://www.isotc211.org/2005/gmd");
                            put("gml", "http://www.opengis.net/gml/3.2");
                            put("gsml", "http://xmlns.geosciml.org/GeoSciML-Core/3.2");
                            put("gsmlbh", "http://xmlns.geosciml.org/Borehole/3.2");
                            put("sa", "http://www.opengis.net/sampling/2.0");
                            put("sams", "http://www.opengis.net/samplingSpatial/2.0");
                            put("swe", "http://www.opengis.net/swe/2.0");
                            put("wfs", "http://www.opengis.net/wfs/2.0");
                            put("xlink", "http://www.w3.org/1999/xlink");
                        }
                    });

    public Gsml32BoreholeIntervalMockData() {
        super(GSML32_NAMESPACES);
    }

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(
                GSMLBH_PREFIX,
                "Borehole",
                "Gsml32BoreholeInterval.xml",
                "Gsml32Borehole.properties");
    }
}

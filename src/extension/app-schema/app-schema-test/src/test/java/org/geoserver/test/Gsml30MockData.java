/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for {@link Gsml30WfsTest}.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class Gsml30MockData extends AbstractAppSchemaMockData {

    public static final String GSML_SCHEMA_LOCATION =
            "https://www.seegrid.csiro.au/subversion/GeoSciML/branches/3.0.0_rc1_gml3.2/geosciml-core/3.0.0/xsd/geosciml-core.xsd";

    public Gsml30MockData() {
        super(GML32_NAMESPACES);
    }

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(
                GSML_PREFIX, "MappedFeature", "Gsml30WfsTest.xml", "Gsml30WfsTest.properties");
    }
}

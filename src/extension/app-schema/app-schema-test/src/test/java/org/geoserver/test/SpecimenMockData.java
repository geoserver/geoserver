/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for {@link SpecimenWfsTest}.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class SpecimenMockData extends AbstractAppSchemaMockData {

    public static final String GSML_SCHEMA_LOCATION =
            "http://schemas.opengis.net/samplingSpecimen/2.0/specimen.xsd";

    public SpecimenMockData() {
        super(GML32_NAMESPACES);
    }

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(
                SPEC_PREFIX, "SF_Specimen", "SpecimenWfsTest.xml", "SpecimenWfsTest.properties");
    }
}

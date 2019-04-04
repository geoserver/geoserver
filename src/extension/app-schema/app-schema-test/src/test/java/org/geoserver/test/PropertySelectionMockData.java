/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/** @author Niels Charlier, Curtin University Of Technology */
public class PropertySelectionMockData extends AbstractAppSchemaMockData {

    /** Prefix for ex namespace. */
    protected static final String EX_PREFIX = "ex";

    /** URI for ex namespace. */
    protected static final String EX_URI = "http://example.com";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(EX_PREFIX, EX_URI);

        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeaturePropertySelection.xml",
                "MappedFeatureNoId.properties");
        addFeatureType(
                GSML_PREFIX,
                "GeologicUnit",
                "GeologicUnit.xml",
                "GeologicUnit.properties",
                "CGITermValue.xml",
                "CGITermValue.properties",
                "exposureColor.properties",
                "CompositionPart.xml",
                "CompositionPart.properties",
                "ControlledConcept.xml",
                "ControlledConcept.properties");

        addFeatureType(
                EX_PREFIX,
                "MyTestFeature",
                "SameNameDiffNamespace.xml",
                "SameNameDiffNamespace.properties",
                "SameNameDiffNamespace.xsd");
    }
}

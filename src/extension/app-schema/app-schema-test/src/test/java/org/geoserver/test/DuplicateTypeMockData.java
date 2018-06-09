/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for {@link DuplicateTypeTest}, which tests two WFS feature types (XSD elements) with
 * the same XSD type.
 *
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class DuplicateTypeMockData extends AbstractAppSchemaMockData {

    @Override
    public void addContent() {
        // these two types are the same as for FeatureChainingWfsTest
        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeaturePropertyfile.xml",
                "MappedFeaturePropertyfile.properties");
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
        // add the WFS type that has the same XSD type as gsml:MappedFeature
        addFeatureType(
                GSML_PREFIX,
                "DuplicateMappedFeature",
                "DuplicateTypeTest.xml",
                "DuplicateTypeTest.xsd",
                "DuplicateTypeTest.properties");
    }
}

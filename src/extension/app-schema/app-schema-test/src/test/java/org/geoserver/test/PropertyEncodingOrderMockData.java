/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

/** @author Xiangtan Lin, CSIRO Information Management and Technology */
public class PropertyEncodingOrderMockData extends AbstractAppSchemaMockData {

    protected static final String ER_PREFIX = "er";

    protected static final String ER_URI = "urn:cgi:xmlns:GGIC:EarthResource:1.1";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(ER_PREFIX, ER_URI);
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
        addFeatureType(
                ER_PREFIX,
                "MineralOccurrence",
                "er_MineralOccurrence.xml",
                "er_MineralOccurrence.properties",
                "CGI_PlanarOrientation.xml",
                "CGI_PlanarOrientation.properties");
        addFeatureType(
                GSML_PREFIX,
                "CGI_PlanarOrientation",
                "CGI_PlanarOrientation.xml",
                "CGI_PlanarOrientation.properties");
        addFeatureType(GSML_PREFIX, "Borehole", "Borehole.xml", "Borehole.properties");
    }
}

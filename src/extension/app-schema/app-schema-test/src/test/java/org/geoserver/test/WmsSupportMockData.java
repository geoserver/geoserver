/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/** @author Niels Charlier */

import org.geoserver.data.test.MockData;
import org.geotools.data.complex.AppSchemaDataAccess;

/**
 * Mock data for testing integration of {@link AppSchemaDataAccess} with Wms.
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class WmsSupportMockData extends AbstractAppSchemaMockData {

    /** Prefix for ex namespace. */
    protected static final String EX_PREFIX = "ex";

    /** URI for ex namespace. */
    protected static final String EX_URI = "http://example.com";

    /** Prefix for om namespace. */
    protected static final String OM_PREFIX = "om";

    /** URI for om namespace. */
    protected static final String OM_URI = "http://www.opengis.net/om/1.0";

    /** Schema URL for observation and measurements */
    protected static final String OM_SCHEMA_LOCATION_URL =
            "http://schemas.opengis.net/om/1.0.0/observation.xsd";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(EX_PREFIX, EX_URI);
        putNamespace(OM_PREFIX, OM_URI);
        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeatureWms.xml",
                "MappedFeatureWms.properties");
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
    }
}

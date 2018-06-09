/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;

/**
 * Mock data for testing GEOS-5618: using functions in idExpression with joining.
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class IdFunctionMockData extends AbstractAppSchemaMockData {

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeatureGetId.xml",
                "MappedFeaturePropertyfile.properties");
        addFeatureType(
                GSML_PREFIX, "GeologicUnit", "GeologicUnitGetId.xml", "GeologicUnit.properties");
    }
}

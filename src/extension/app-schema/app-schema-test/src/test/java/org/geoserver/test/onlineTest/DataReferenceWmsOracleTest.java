/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.data.test.SystemTestData;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;

/** @author Niels Charlier */
public class DataReferenceWmsOracleTest extends DataReferenceWmsOracleWithJoiningTest {

    public DataReferenceWmsOracleTest() throws Exception {
        super();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties()
                .setProperty("app-schema.joining", "false");
        super.setUpTestData(testData);
    }
}

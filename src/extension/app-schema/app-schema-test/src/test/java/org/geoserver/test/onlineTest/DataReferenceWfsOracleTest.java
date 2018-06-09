/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.data.test.SystemTestData;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;

/** @author Victor Tey(CSIRO Earth Science and Resource Engineering) */
public class DataReferenceWfsOracleTest extends DataReferenceWfsOracleWithJoiningTest {

    public DataReferenceWfsOracleTest() throws Exception {
        super();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties()
                .setProperty("app-schema.joining", "false");
        super.setUpTestData(testData);
    }
}

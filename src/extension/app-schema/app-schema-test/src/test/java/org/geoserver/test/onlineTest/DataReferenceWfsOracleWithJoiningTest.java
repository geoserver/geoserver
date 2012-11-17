/* Copyright (c) 2011-2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.junit.Test;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.NamespaceTestData;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;

public class DataReferenceWfsOracleWithJoiningTest extends DataReferenceWfsOracleTest {

    public DataReferenceWfsOracleWithJoiningTest() throws Exception {
        super();
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties().setProperty ("app-schema.joining", "true");
        super.onSetUp(testData);
    }
    
    @Override
    @Test
    public void testFilteringSplit() throws Exception {
        //this is a non joining test
    }

}

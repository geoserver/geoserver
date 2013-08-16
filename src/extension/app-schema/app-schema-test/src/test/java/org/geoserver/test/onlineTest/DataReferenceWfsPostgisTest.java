/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.data.test.SystemTestData;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
/**
 * 
 * @author Victor Tey(CSIRO Earth Science and Resource Engineering)
 *
 */
public class DataReferenceWfsPostgisTest extends DataReferenceWfsPostgisWithJoiningTest {

    public DataReferenceWfsPostgisTest() throws Exception {
        super();
    }
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
    	AppSchemaDataAccessRegistry.getAppSchemaProperties().setProperty ("app-schema.joining", "false");                
        super.setUpTestData(testData);
    }
}

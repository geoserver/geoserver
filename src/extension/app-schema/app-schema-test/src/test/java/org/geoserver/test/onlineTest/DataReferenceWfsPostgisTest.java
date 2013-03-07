/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.onlineTest.setup.AppSchemaReferenceMockData;
import org.geoserver.test.onlineTest.setup.ReferenceDataPostgisSetup;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
/**
 * 
 * @author Victor Tey(CSIRO Earth Science and Resource Engineering)
 *
 */
public class DataReferenceWfsPostgisTest extends DataReferenceWfsOnlineTest {

    public DataReferenceWfsPostgisTest() throws Exception {
        super();
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties().setProperty ("app-schema.joining", "false");
        super.onSetUp(testData);
    }

    @Override
    protected AppSchemaReferenceMockData createTestData() {
        return new AppSchemaReferenceMockData();

    }

    @Override
    public AbstractReferenceDataSetup getReferenceDataSetup() throws Exception {
        return new ReferenceDataPostgisSetup();
    }

}

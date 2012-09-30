/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.test.NamespaceTestData;
import org.geoserver.test.onlineTest.setup.AppSchemaWmsReferenceMockData;
import org.geoserver.test.onlineTest.setup.ReferenceDataOracleSetup;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;

/**
 * 
 * @author Niels Charlier
 * 
 */
public class DataReferenceWmsOracleTest extends DataReferenceWmsOnlineTest {

    public DataReferenceWmsOracleTest() throws Exception {
        super();
    }

    @Override
    protected AppSchemaWmsReferenceMockData createTestData() {
    	AppSchemaWmsReferenceMockData mockData = new AppSchemaWmsReferenceMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("simplelithology", "styles/cgi-simplelithology-2008.sld");
        mockData.addStyle("stratchart", "styles/ics-stratchart-2008.sld");
        return mockData;
    }

    @Override
    public AbstractReferenceDataSetup getReferenceDataSetup() throws Exception {
        return new ReferenceDataOracleSetup();
    }

}

/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import junit.framework.Test;

import org.geoserver.test.NamespaceTestData;
import org.geoserver.test.onlineTest.setup.AppSchemaWmsReferenceMockData;
import org.geoserver.test.onlineTest.setup.ReferenceDataPostgisSetup;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;

public class DataReferenceWmsPostgisTest extends DataReferenceWmsOnlineTest {

    public DataReferenceWmsPostgisTest() throws Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        try {
            return new OneTimeTestSetup(new DataReferenceWmsPostgisTest());
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    protected NamespaceTestData buildTestData() {
    	AppSchemaWmsReferenceMockData mockData = new AppSchemaWmsReferenceMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("simplelithology", "styles/cgi-simplelithology-200811.sld");
        return mockData;
    }

    @Override
    public AbstractReferenceDataSetup getReferenceDataSetup() throws Exception {
        return new ReferenceDataPostgisSetup();
    }

}

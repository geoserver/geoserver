/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import junit.framework.Test;


import org.geoserver.test.NamespaceTestData;
import org.geoserver.test.onlineTest.setup.AppSchemaReferenceMockData;
import org.geoserver.test.onlineTest.setup.ReferenceDataOracleSetup;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;

public class DataReferenceWfsOracleTest extends DataReferenceWfsOnlineTest {

    public DataReferenceWfsOracleTest() throws Exception {
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
            return new OneTimeTestSetup(new DataReferenceWfsOracleTest());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new AppSchemaReferenceMockData();
    }

    @Override
    public AbstractReferenceDataSetup getReferenceDataSetup() throws Exception {
        return new ReferenceDataOracleSetup();
    }

}

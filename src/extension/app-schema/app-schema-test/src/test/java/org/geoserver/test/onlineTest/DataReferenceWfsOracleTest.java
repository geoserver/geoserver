/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.test.NamespaceTestData;
import org.geoserver.test.onlineTest.setup.AppSchemaReferenceMockData;
import org.geoserver.test.onlineTest.setup.ReferenceDataOracleSetup;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;

public class DataReferenceWfsOracleTest extends DataReferenceWfsOnlineTest {

    public DataReferenceWfsOracleTest() throws Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    protected AppSchemaReferenceMockData createTestData() {
        return new AppSchemaReferenceMockData();
    }

    @Override
    public AbstractReferenceDataSetup getReferenceDataSetup() throws Exception {
        return new ReferenceDataOracleSetup();
    }

}

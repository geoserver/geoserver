/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.security.AccessMode;

public class DataAccesRuleDAOConcurrencyTest
        extends AbstractAccesRuleDAOConcurrencyTest<DataAccessRuleDAO> {

    @Override
    protected DataAccessRuleDAO buildDAO() throws Exception {
        // make a nice little catalog that does always tell us stuff is there
        Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspaceByName(anyObject()))
                .andReturn(new WorkspaceInfoImpl())
                .anyTimes();
        expect(catalog.getLayerByName((String) anyObject()))
                .andReturn(new LayerInfoImpl())
                .anyTimes();
        replay(catalog);

        // base rules
        Properties props = new Properties();
        props.put("mode", "CHALLENGE");
        props.put("*.*.r", "*");

        return new MemoryDataAccessRuleDAO(catalog, props);
    }

    @Override
    protected Void manipulate(int c) {
        // prepare
        String customRole = "R_TEST_" + c;
        String ws = "w" + c;
        String layer = "l" + c;

        // read access, loads all (the concurrent modification exception normally happens here)
        dao.getRules();
        // add rule
        DataAccessRule rule =
                new DataAccessRule(ws, layer, AccessMode.READ, "R_READ", "R_WRITE", customRole);
        dao.addRule(rule);
        // check it's there
        assertEquals(Collections.singleton(rule), dao.getRulesAssociatedWithRole(customRole));
        // another read access
        dao.getRules();
        // remove the rule
        dao.removeRule(rule);

        return null;
    }
}

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Properties;

public class ServiceAccessRuleDAOTest
        extends AbstractAccesRuleDAOConcurrencyTest<ServiceAccessRuleDAO> {

    /** A DAO that won't check the file system */
    static class MemoryServiceAccessRuleDAO extends ServiceAccessRuleDAO {

        public MemoryServiceAccessRuleDAO(Properties props) {
            super(null);
            loadRules(props);
        }

        @Override
        protected void checkPropertyFile(boolean force) {
            // skip checking
            lastModified = Long.MAX_VALUE;
        }
    }

    @Override
    protected ServiceAccessRuleDAO buildDAO() throws Exception {
        // base rules
        Properties props = new Properties();
        props.put("*.*", "*");

        return new MemoryServiceAccessRuleDAO(props);
    }

    @Override
    protected Void manipulate(int c) {
        // prepare
        String service = "ows" + c;
        String method = "operation" + c;
        String customRole = "R_TEST" + c;

        // read access, loads all (the concurrent modification exception normally happens here)
        dao.getRules();
        // add rule
        ServiceAccessRule rule = new ServiceAccessRule(service, method, customRole);
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

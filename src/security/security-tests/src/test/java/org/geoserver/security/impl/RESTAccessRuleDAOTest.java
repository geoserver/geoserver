/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.easymock.EasyMock;
import org.geoserver.config.GeoServerDataDirectory;
import org.hamcrest.CoreMatchers;

public class RESTAccessRuleDAOTest extends AbstractAccesRuleDAOConcurrencyTest<RESTAccessRuleDAO> {

    /** A DAO that won't check the file system */
    static class MemoryRESTAccessRuleDAO extends RESTAccessRuleDAO {

        public MemoryRESTAccessRuleDAO(GeoServerDataDirectory dd, Properties props)
                throws IOException {
            super(dd);
            loadRules(props);
        }

        @Override
        protected void checkPropertyFile(boolean force) {
            // skip checking
            lastModified = Long.MAX_VALUE;
        }
    }

    @Override
    protected RESTAccessRuleDAO buildDAO() throws Exception {
        // base rules
        Properties props = new Properties();
        props.put("/rest/**;GET,POST,PUT,DELETE=", "ROLE_ADMINISTRATOR");

        GeoServerDataDirectory dd = EasyMock.mock(GeoServerDataDirectory.class);
        return new MemoryRESTAccessRuleDAO(dd, props);
    }

    @Override
    protected Void manipulate(int c) {
        // prepare
        String customRole = "R_TEST" + c;
        String rule = "/rest/resource" + c + ";GET,POST,PUT,DELETE=" + customRole;

        // read access, loads all (the concurrent modification exception normally happens here)
        dao.getRules();
        dao.addRule(rule);
        // check it's there
        assertThat(dao.getRules(), CoreMatchers.hasItem(rule));
        // remove the rule
        dao.removeRule(rule);

        return null;
    }
}

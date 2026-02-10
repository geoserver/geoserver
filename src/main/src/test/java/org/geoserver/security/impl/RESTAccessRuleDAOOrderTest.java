/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;
import org.easymock.EasyMock;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.util.LinkedProperties;
import org.junit.Test;

/**
 * Tests to verify that RESTAccessRuleDAO preserves rule order, which is critical for correct security evaluation, and
 * that it uses LinkedProperties for order preservation.
 */
public class RESTAccessRuleDAOOrderTest {

    /** A DAO that won't check the file system for testing */
    static class MemoryRESTAccessRuleDAO extends RESTAccessRuleDAO {
        public MemoryRESTAccessRuleDAO(GeoServerDataDirectory dd, Properties props) throws IOException {
            super(dd);
            loadRules(props);
        }

        @Override
        protected void checkPropertyFile(boolean force) {
            // skip checking - use in-memory properties only
            lastModified = Long.MAX_VALUE;
        }
    }

    @Test
    public void testToPropertiesReturnsLinkedProperties() throws IOException {
        Properties props = new Properties();
        props.put("/first/**;GET", "ROLE_A");
        props.put("/second/**;GET", "ROLE_B");
        props.put("/third/**;GET", "ROLE_C");

        GeoServerDataDirectory dd = EasyMock.mock(GeoServerDataDirectory.class);
        MemoryRESTAccessRuleDAO dao = new MemoryRESTAccessRuleDAO(dd, props);

        Properties result = dao.toProperties();

        // Verify it's a LinkedProperties instance
        assertTrue("toProperties should return LinkedProperties", result instanceof LinkedProperties);
    }

    @Test
    public void testRuleOrderPreservedInProperties() throws IOException {
        // Create LinkedProperties with insertion order
        LinkedProperties props = new LinkedProperties();
        // LinkedProperties preserves insertion order; no additional call required
        props.put("/z-last/**;GET", "ROLE_Z");
        props.put("/a-first/**;GET", "ROLE_A");
        props.put("/m-middle/**;GET", "ROLE_M");

        GeoServerDataDirectory dd = EasyMock.mock(GeoServerDataDirectory.class);
        MemoryRESTAccessRuleDAO dao = new MemoryRESTAccessRuleDAO(dd, props);

        Properties result = dao.toProperties();

        // Verify order is preserved (insertion order, not alphabetical)
        Object[] keys = result.keySet().toArray();
        assertEquals("/z-last/**;GET", keys[0]);
        assertEquals("/a-first/**;GET", keys[1]);
        assertEquals("/m-middle/**;GET", keys[2]);
    }
}

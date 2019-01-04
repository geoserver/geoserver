/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.*;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Properties;
import junit.framework.TestCase;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.security.AccessMode;
import org.junit.Before;
import org.junit.Test;

public class DataAccessRuleDAOTest extends TestCase {

    DataAccessRuleDAO dao;
    Properties props;

    @Before
    public void setUp() throws Exception {
        // make a nice little catalog that does always tell us stuff is there
        Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspaceByName((String) anyObject()))
                .andReturn(new WorkspaceInfoImpl())
                .anyTimes();
        expect(catalog.getLayerByName((String) anyObject()))
                .andReturn(new LayerInfoImpl())
                .anyTimes();
        replay(catalog);

        // prepare some base rules
        props = new Properties();
        props.put("mode", "CHALLENGE");
        props.put("topp.states.w", "ROLE_TSW");
        props.put("topp.*.w", "ROLE_TW");
        props.put("*.*.r", "*");
        props.put("group.r", "ROLE_GROUP");

        dao = new MemoryDataAccessRuleDAO(catalog, props);
    }

    @Test
    public void testRulesForRole() {

        assertEquals(0, dao.getRulesAssociatedWithRole("CHALLENGE").size());
        assertEquals(0, dao.getRulesAssociatedWithRole("NOTEXISTEND").size());
        assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_TSW").size());
        assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_TW").size());
        assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_GROUP").size());
    }

    @Test
    public void testParseGlobalLayerGroupRule() {
        DataAccessRule r = dao.parseDataAccessRule("group.r", "ROLE_GROUP_OWNER");
        assertEquals(r.getRoot(), "group");
        assertNull(r.getLayer());
        assertTrue(r.isGlobalGroupRule());
        assertEquals(AccessMode.READ, r.getAccessMode());
    }

    @Test
    public void testParse() {
        assertEquals(4, dao.getRules().size());

        // check the first rule
        DataAccessRule rule = dao.getRules().get(0);
        assertEquals("*.*.r", rule.getKey());
        assertEquals(1, rule.getRoles().size());
        assertEquals("*", rule.getRoles().iterator().next());
    }

    @Test
    public void testAdd() {
        assertEquals(4, dao.getRules().size());
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        assertTrue(dao.addRule(newRule));
        assertEquals(5, dao.getRules().size());
        assertEquals(newRule, dao.getRules().get(1));
        assertFalse(dao.addRule(newRule));
    }

    @Test
    public void testRemove() {
        assertEquals(4, dao.getRules().size());
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        assertFalse(dao.removeRule(newRule));
        DataAccessRule first = dao.getRules().get(0);
        assertTrue(dao.removeRule(first));
        assertFalse(dao.removeRule(first));
        assertEquals(3, dao.getRules().size());
    }

    @Test
    public void testStore() {
        Properties newProps = dao.toProperties();

        // properties equality does not seem to work...
        assertEquals(newProps.size(), props.size());
        for (Object key : newProps.keySet()) {
            Object newValue = newProps.get(key);
            Object oldValue = newProps.get(key);
            assertEquals(newValue, oldValue);
        }
    }

    @Test
    public void testParsePlain() {
        DataAccessRule rule = dao.parseDataAccessRule("a.b.r", "ROLE_WHO_CARES");
        assertEquals("a", rule.getRoot());
        assertEquals("b", rule.getLayer());
        assertFalse(rule.isGlobalGroupRule());
        assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testParseSpaces() {
        DataAccessRule rule = dao.parseDataAccessRule(" a  . b . r ", "ROLE_WHO_CARES");
        assertEquals("a", rule.getRoot());
        assertEquals("b", rule.getLayer());
        assertFalse(rule.isGlobalGroupRule());
        assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testParseEscapedDots() {
        DataAccessRule rule = dao.parseDataAccessRule("w. a\\.b . r ", "ROLE_WHO_CARES");
        assertEquals("w", rule.getRoot());
        assertEquals("a.b", rule.getLayer());
        assertFalse(rule.isGlobalGroupRule());
        assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testStoreEscapedDots() throws Exception {
        dao.clear();
        dao.addRule(
                new DataAccessRule(
                        "it.geosolutions",
                        "layer.dots",
                        AccessMode.READ,
                        Collections.singleton("ROLE_ABC")));
        Properties ps = dao.toProperties();

        assertEquals(2, ps.size());
        assertEquals("ROLE_ABC", ps.getProperty("it\\.geosolutions.layer\\.dots.r"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ps.store(bos, null);
    }
}

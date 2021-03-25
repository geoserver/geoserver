/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.security.AccessMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DataAccessRuleDAOTest {

    DataAccessRuleDAO dao;
    Properties props;

    @Before
    public void setUp() throws Exception {
        // make a nice little catalog that does always tell us stuff is there
        Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspaceByName(anyObject()))
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

        Assert.assertEquals(0, dao.getRulesAssociatedWithRole("CHALLENGE").size());
        Assert.assertEquals(0, dao.getRulesAssociatedWithRole("NOTEXISTEND").size());
        Assert.assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_TSW").size());
        Assert.assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_TW").size());
        Assert.assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_GROUP").size());
    }

    @Test
    public void testParseGlobalLayerGroupRule() {
        DataAccessRule r = dao.parseDataAccessRule("group.r", "ROLE_GROUP_OWNER");
        Assert.assertEquals(r.getRoot(), "group");
        Assert.assertNull(r.getLayer());
        Assert.assertTrue(r.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, r.getAccessMode());
    }

    @Test
    public void testParse() {
        Assert.assertEquals(4, dao.getRules().size());

        // check the first rule
        DataAccessRule rule = dao.getRules().get(0);
        Assert.assertEquals("*.*.r", rule.getKey());
        Assert.assertEquals(1, rule.getRoles().size());
        Assert.assertEquals("*", rule.getRoles().iterator().next());
    }

    @Test
    public void testAdd() {
        Assert.assertEquals(4, dao.getRules().size());
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        Assert.assertTrue(dao.addRule(newRule));
        Assert.assertEquals(5, dao.getRules().size());
        Assert.assertEquals(newRule, dao.getRules().get(1));
        Assert.assertFalse(dao.addRule(newRule));
    }

    @Test
    public void testRemove() {
        Assert.assertEquals(4, dao.getRules().size());
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        Assert.assertFalse(dao.removeRule(newRule));
        DataAccessRule first = dao.getRules().get(0);
        Assert.assertTrue(dao.removeRule(first));
        Assert.assertFalse(dao.removeRule(first));
        Assert.assertEquals(3, dao.getRules().size());
    }

    @Test
    public void testStore() {
        Properties newProps = dao.toProperties();

        // properties equality does not seem to work...
        Assert.assertEquals(newProps.size(), props.size());
        for (Object key : newProps.keySet()) {
            Object newValue = newProps.get(key);
            Object oldValue = newProps.get(key);
            Assert.assertEquals(newValue, oldValue);
        }
    }

    @Test
    public void testParsePlain() {
        DataAccessRule rule = dao.parseDataAccessRule("a.b.r", "ROLE_WHO_CARES");
        Assert.assertEquals("a", rule.getRoot());
        Assert.assertEquals("b", rule.getLayer());
        Assert.assertFalse(rule.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testParseSpaces() {
        DataAccessRule rule = dao.parseDataAccessRule(" a  . b . r ", "ROLE_WHO_CARES");
        Assert.assertEquals("a", rule.getRoot());
        Assert.assertEquals("b", rule.getLayer());
        Assert.assertFalse(rule.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testParseEscapedDots() {
        DataAccessRule rule = dao.parseDataAccessRule("w. a\\.b . r ", "ROLE_WHO_CARES");
        Assert.assertEquals("w", rule.getRoot());
        Assert.assertEquals("a.b", rule.getLayer());
        Assert.assertFalse(rule.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, rule.getAccessMode());
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

        Assert.assertEquals(2, ps.size());
        Assert.assertEquals("ROLE_ABC", ps.getProperty("it\\.geosolutions.layer\\.dots.r"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ps.store(bos, null);
    }
}

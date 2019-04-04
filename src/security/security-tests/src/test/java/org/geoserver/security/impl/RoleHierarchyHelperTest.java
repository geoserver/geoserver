/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class RoleHierarchyHelperTest {

    protected Map<String, String> createFromArray(String[][] array) {
        Map<String, String> mappings = new HashMap<String, String>();
        for (int i = 0; i < array.length; i++) {
            mappings.put(array[i][0], array[i][1]);
        }
        return mappings;
    }

    @Test
    public void testValidTree() throws Exception {
        Map<String, String> map =
                createFromArray(
                        new String[][] {
                            {"node1", null},
                            {"node11", "node1"},
                            {"node12", "node1"},
                            {"node111", "node11"},
                            {"node112", "node11"},
                        });
        RoleHierarchyHelper helper = new RoleHierarchyHelper(map);

        assertFalse(helper.containsRole("abc"));
        assertTrue(helper.containsRole("node11"));
        assertFalse(helper.isRoot("node11"));
        assertTrue(helper.isRoot("node1"));
        assertEquals(1, helper.getRootRoles().size());
        assertTrue(helper.getRootRoles().contains("node1"));

        assertEquals(3, helper.getLeafRoles().size());
        assertTrue(helper.getLeafRoles().contains("node111"));
        assertTrue(helper.getLeafRoles().contains("node112"));
        assertTrue(helper.getLeafRoles().contains("node12"));

        assertEquals("node1", helper.getParent("node11"));
        assertNull(helper.getParent("node1"));

        assertEquals(0, helper.getAncestors("node1").size());
        assertEquals(1, helper.getAncestors("node12").size());
        assertTrue(helper.getAncestors("node12").contains("node1"));
        assertEquals(2, helper.getAncestors("node112").size());
        assertTrue(helper.getAncestors("node112").contains("node11"));
        assertTrue(helper.getAncestors("node112").contains("node1"));

        assertEquals(2, helper.getChildren("node1").size());
        assertTrue(helper.getChildren("node1").contains("node11"));
        assertTrue(helper.getChildren("node1").contains("node12"));

        assertEquals(0, helper.getChildren("node12").size());
        assertEquals(2, helper.getChildren("node11").size());
        assertTrue(helper.getChildren("node11").contains("node111"));
        assertTrue(helper.getChildren("node11").contains("node112"));

        assertEquals(4, helper.getDescendants("node1").size());
        assertTrue(helper.getDescendants("node1").contains("node11"));
        assertTrue(helper.getDescendants("node1").contains("node12"));
        assertTrue(helper.getDescendants("node1").contains("node111"));
        assertTrue(helper.getDescendants("node1").contains("node112"));

        assertEquals(0, helper.getDescendants("node12").size());

        assertEquals(2, helper.getDescendants("node11").size());
        assertTrue(helper.getDescendants("node11").contains("node111"));
        assertTrue(helper.getDescendants("node11").contains("node112"));

        assertTrue(helper.isValidParent("node11", null));
        assertTrue(helper.isValidParent("node11", "node12"));
        assertFalse(helper.isValidParent("node11", "node11"));
        assertFalse(helper.isValidParent("node1", "node111"));

        boolean fail = true;
        try {
            helper.isRoot("abc");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");
    }

    @Test
    public void testInValidTree1() throws Exception {
        Map<String, String> map = createFromArray(new String[][] {{"node1", "node1"}});
        RoleHierarchyHelper helper = new RoleHierarchyHelper(map);
        boolean fail;

        fail = true;
        try {
            helper.getParent("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        fail = true;
        try {
            helper.getAncestors("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        fail = true;
        try {
            helper.getChildren("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        fail = true;
        try {
            helper.getDescendants("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");
    }

    @Test
    public void testInValidTree2() throws Exception {
        Map<String, String> map =
                createFromArray(
                        new String[][] {
                            {"node1", "node2"},
                            {"node2", "node1"}
                        });
        RoleHierarchyHelper helper = new RoleHierarchyHelper(map);
        boolean fail;

        helper.getParent("node1"); // ok

        fail = true;
        try {
            helper.getAncestors("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        helper.getChildren("node1"); // ok

        fail = true;
        try {
            helper.getDescendants("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");
    }
}

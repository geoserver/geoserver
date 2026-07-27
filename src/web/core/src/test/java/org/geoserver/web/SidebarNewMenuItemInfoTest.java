/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SidebarNewMenuItemInfoTest {

    @Test
    public void testDefaultValues() {
        SidebarNewMenuItemInfo info = new SidebarNewMenuItemInfo();
        assertEquals(100, info.getOrder());
        assertNull(info.getCategory());
        assertNull(info.getIcon());
    }

    @Test
    public void testSetOrder() {
        SidebarNewMenuItemInfo info = new SidebarNewMenuItemInfo();
        info.setOrder(25);
        assertEquals(25, info.getOrder());
    }

    @Test
    public void testSetCategory() {
        SidebarNewMenuItemInfo info = new SidebarNewMenuItemInfo();
        Category cat = new Category();
        cat.setNameKey("testCat");
        info.setCategory(cat);
        assertNotNull(info.getCategory());
        assertEquals("testCat", info.getCategory().getNameKey());
    }

    @Test
    public void testSetIcon() {
        SidebarNewMenuItemInfo info = new SidebarNewMenuItemInfo();
        info.setIcon("gs-icon-add");
        assertEquals("gs-icon-add", info.getIcon());
    }

    @Test
    public void testCompareToSameOrder() {
        SidebarNewMenuItemInfo a = new SidebarNewMenuItemInfo();
        SidebarNewMenuItemInfo b = new SidebarNewMenuItemInfo();
        assertEquals(0, a.compareTo(b));
    }

    @Test
    public void testCompareToDifferentOrder() {
        SidebarNewMenuItemInfo a = new SidebarNewMenuItemInfo();
        a.setOrder(10);
        SidebarNewMenuItemInfo b = new SidebarNewMenuItemInfo();
        b.setOrder(50);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void testSortByOrder() {
        SidebarNewMenuItemInfo first = new SidebarNewMenuItemInfo();
        first.setOrder(30);
        first.setTitleKey("third");

        SidebarNewMenuItemInfo second = new SidebarNewMenuItemInfo();
        second.setOrder(10);
        second.setTitleKey("first");

        SidebarNewMenuItemInfo third = new SidebarNewMenuItemInfo();
        third.setOrder(20);
        third.setTitleKey("second");

        List<SidebarNewMenuItemInfo> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        list.add(third);
        Collections.sort(list);

        assertEquals("first", list.get(0).getTitleKey());
        assertEquals("second", list.get(1).getTitleKey());
        assertEquals("third", list.get(2).getTitleKey());
    }

    @Test
    public void testInheritedComponentInfoProperties() {
        SidebarNewMenuItemInfo info = new SidebarNewMenuItemInfo();
        info.setId("sidebarNewWorkspace");
        info.setTitleKey("newWorkspace");
        info.setDescriptionKey("createNewWorkspace");

        assertEquals("sidebarNewWorkspace", info.getId());
        assertEquals("newWorkspace", info.getTitleKey());
        assertEquals("createNewWorkspace", info.getDescriptionKey());
    }

    @Test
    public void testDefaultAuthorizerIsAllow() {
        SidebarNewMenuItemInfo info = new SidebarNewMenuItemInfo();
        assertNotNull(info.getAuthorizer());
        assertEquals(ComponentAuthorizer.ALLOW, info.getAuthorizer());
    }
}

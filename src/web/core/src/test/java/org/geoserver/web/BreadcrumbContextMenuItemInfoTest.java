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
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.Test;

public class BreadcrumbContextMenuItemInfoTest {

    @Test
    public void testDefaultValues() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        assertEquals(100, info.getOrder());
        assertEquals("LAYER", info.getTargetLevel());
        assertNull(info.getCategory());
        assertNull(info.getIcon());
    }

    @Test
    public void testSetOrder() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        info.setOrder(50);
        assertEquals(50, info.getOrder());
    }

    @Test
    public void testSetTargetLevel() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        info.setTargetLevel("WORKSPACE");
        assertEquals("WORKSPACE", info.getTargetLevel());
    }

    @Test
    public void testSetCategory() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        Category cat = new Category();
        cat.setNameKey("testCategory");
        cat.setOrder(10);
        info.setCategory(cat);
        assertNotNull(info.getCategory());
        assertEquals("testCategory", info.getCategory().getNameKey());
    }

    @Test
    public void testSetIcon() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        info.setIcon("img/icons/silk/folder.png");
        assertEquals("img/icons/silk/folder.png", info.getIcon());
    }

    @Test
    public void testCompareToSameOrder() {
        BreadcrumbContextMenuItemInfo a = new BreadcrumbContextMenuItemInfo();
        BreadcrumbContextMenuItemInfo b = new BreadcrumbContextMenuItemInfo();
        assertEquals(0, a.compareTo(b));
    }

    @Test
    public void testCompareToDifferentOrder() {
        BreadcrumbContextMenuItemInfo a = new BreadcrumbContextMenuItemInfo();
        a.setOrder(10);
        BreadcrumbContextMenuItemInfo b = new BreadcrumbContextMenuItemInfo();
        b.setOrder(20);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void testSortByOrder() {
        BreadcrumbContextMenuItemInfo first = new BreadcrumbContextMenuItemInfo();
        first.setOrder(30);
        first.setTitleKey("third");

        BreadcrumbContextMenuItemInfo second = new BreadcrumbContextMenuItemInfo();
        second.setOrder(10);
        second.setTitleKey("first");

        BreadcrumbContextMenuItemInfo third = new BreadcrumbContextMenuItemInfo();
        third.setOrder(20);
        third.setTitleKey("second");

        List<BreadcrumbContextMenuItemInfo> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        list.add(third);
        Collections.sort(list);

        assertEquals("first", list.get(0).getTitleKey());
        assertEquals("second", list.get(1).getTitleKey());
        assertEquals("third", list.get(2).getTitleKey());
    }

    @Test
    public void testGetPageParametersWithBothNames() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters("myWorkspace", "myLayer");

        assertEquals("myWorkspace", params.get("workspace").toString());
        assertEquals("myLayer", params.get("layer").toString());
        assertEquals("myLayer", params.get("group").toString());
    }

    @Test
    public void testGetPageParametersWorkspaceOnly() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters("myWorkspace", null);

        assertEquals("myWorkspace", params.get("workspace").toString());
        assertTrue(params.get("layer").isNull());
        assertTrue(params.get("group").isNull());
    }

    @Test
    public void testGetPageParametersResourceOnly() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters(null, "myLayer");

        assertTrue(params.get("workspace").isNull());
        assertEquals("myLayer", params.get("layer").toString());
        assertEquals("myLayer", params.get("group").toString());
    }

    @Test
    public void testGetPageParametersNullBoth() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters(null, null);

        assertTrue(params.get("workspace").isNull());
        assertTrue(params.get("layer").isNull());
    }

    @Test
    public void testGetPageParametersEmptyStrings() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters("", "");

        assertTrue(params.get("workspace").isNull());
        assertTrue(params.get("layer").isNull());
    }

    @Test
    public void testInheritedComponentInfoProperties() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        info.setId("testId");
        info.setTitleKey("testTitle");
        info.setDescriptionKey("testDescription");
        info.setComponentClass(Page.class);

        assertEquals("testId", info.getId());
        assertEquals("testTitle", info.getTitleKey());
        assertEquals("testDescription", info.getDescriptionKey());
        assertEquals(Page.class, info.getComponentClass());
    }

    @Test
    public void testDefaultAuthorizerIsAllow() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        assertNotNull(info.getAuthorizer());
        assertEquals(ComponentAuthorizer.ALLOW, info.getAuthorizer());
    }

    @Test
    public void testGetPageParametersWithLevelLayer() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters("ws", "myLayer", "LAYER");

        assertEquals("ws", params.get("workspace").toString());
        assertEquals("myLayer", params.get("layer").toString());
        assertTrue(params.get("group").isNull());
    }

    @Test
    public void testGetPageParametersWithLevelLayerGroup() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters("ws", "myGroup", "LAYER_GROUP");

        assertEquals("ws", params.get("workspace").toString());
        assertTrue(params.get("layer").isNull());
        assertEquals("myGroup", params.get("group").toString());
    }

    @Test
    public void testGetPageParametersWithUnknownLevel() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters("ws", "res", "WORKSPACE");

        assertEquals("ws", params.get("workspace").toString());
        assertEquals("res", params.get("layer").toString());
        assertEquals("res", params.get("group").toString());
    }

    @Test
    public void testGetPageParametersWithNullLevel() {
        BreadcrumbContextMenuItemInfo info = new BreadcrumbContextMenuItemInfo();
        PageParameters params = info.getPageParameters("ws", "res", null);

        assertEquals("ws", params.get("workspace").toString());
        assertEquals("res", params.get("layer").toString());
        assertEquals("res", params.get("group").toString());
    }
}

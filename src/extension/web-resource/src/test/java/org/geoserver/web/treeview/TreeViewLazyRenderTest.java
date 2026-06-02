/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.treeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that TreeView uses lazy rendering: children of collapsed nodes are not rendered, and expanding a node via the
 * checkbox causes children to be rendered via AJAX refresh.
 */
public class TreeViewLazyRenderTest extends GeoServerWicketTestSupport {

    private static class MockNode implements TreeNode<Integer> {
        private static final long serialVersionUID = 1012858609071186910L;

        protected int data;
        protected MockNode parent;
        protected List<MockNode> children = new ArrayList<>();
        protected IModel<Boolean> expanded = new Model<>(false);

        public MockNode(int data, MockNode parent) {
            this.data = data;
            this.parent = parent;
            if (parent != null) {
                parent.children.add(this);
            }
        }

        @Override
        public Collection<? extends TreeNode<Integer>> getChildren() {
            return children;
        }

        @Override
        public TreeNode<Integer> getParent() {
            return parent;
        }

        @Override
        public Integer getObject() {
            return data;
        }

        @Override
        public IModel<Boolean> getExpanded() {
            return expanded;
        }

        @Override
        public String getUniqueId() {
            return "" + data;
        }

        @Override
        public boolean isLeaf() {
            return children.isEmpty();
        }
    }

    // Tree structure:
    //   1 (root, expanded)
    //   +-- 2 (directory, collapsed)
    //   |   +-- 4 (leaf)
    //   |   +-- 5 (leaf)
    //   +-- 3 (directory, collapsed)
    //   |   +-- 6 (leaf)
    //   +-- 7 (leaf)
    protected MockNode root;
    protected MockNode dirA;
    protected MockNode dirB;
    protected MockNode leafInA1;
    protected MockNode leafInA2;
    protected MockNode leafInB;
    protected MockNode leafInRoot;
    protected TreeView<Integer> treeView;

    @Before
    public void initialize() {
        root = new MockNode(1, null);
        root.expanded.setObject(true);

        dirA = new MockNode(2, root);
        dirB = new MockNode(3, root);
        leafInA1 = new MockNode(4, dirA);
        leafInA2 = new MockNode(5, dirA);
        leafInB = new MockNode(6, dirB);
        leafInRoot = new MockNode(7, root);

        treeView = new TreeView<>("treeView", root);
    }

    @Test
    public void testCollapsedNodeChildrenNotRendered() {
        // dirA and dirB are collapsed
        assertFalse(dirA.expanded.getObject());
        assertFalse(dirB.expanded.getObject());

        tester.startComponentInPage(treeView);

        // Root is expanded, so its direct children (dirA, dirB, leafInRoot) should be rendered
        assertNotNull(tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2"));
        assertNotNull(tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:3"));
        assertNotNull(tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:7"));

        // dirA is collapsed, so its children (4, 5) should NOT be rendered
        Component dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertNotNull(dirAChildren);
        assertTrue(dirAChildren instanceof RepeatingView);
        assertEquals("Collapsed node should have no rendered children", 0, ((RepeatingView) dirAChildren).size());

        // dirB is collapsed, so its child (6) should NOT be rendered
        Component dirBChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:3:children");
        assertNotNull(dirBChildren);
        assertTrue(dirBChildren instanceof RepeatingView);
        assertEquals("Collapsed node should have no rendered children", 0, ((RepeatingView) dirBChildren).size());
    }

    @Test
    public void testExpandedNodeChildrenRendered() {
        // Expand dirA before rendering
        dirA.expanded.setObject(true);

        tester.startComponentInPage(treeView);

        // dirA is expanded, so its children (4, 5) should be rendered
        Component dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertNotNull(dirAChildren);
        assertTrue(dirAChildren instanceof RepeatingView);
        assertEquals("Expanded node should have its children rendered", 2, ((RepeatingView) dirAChildren).size());

        // Verify specific children exist
        assertNotNull(tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children:4"));
        assertNotNull(tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children:5"));

        // dirB is still collapsed, so its child should NOT be rendered
        Component dirBChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:3:children");
        assertEquals(0, ((RepeatingView) dirBChildren).size());
    }

    @Test
    public void testCheckboxAjaxRefreshesNodeView() {
        // Start with dirA expanded so the checkbox is checked
        dirA.expanded.setObject(true);

        tester.startComponentInPage(treeView);

        // Verify dirA children ARE rendered initially (expanded)
        Component dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals("Expanded node should have children rendered", 2, ((RepeatingView) dirAChildren).size());

        // Collapse dirA via the checkbox (AJAX) - clicking a checked checkbox unchecks it.
        // This exercises the real onUpdate() path and will fail if
        // AjaxCheckBox.onUpdate() forgets to call target.add(...)
        tester.executeAjaxEvent("treeView:rootView:1:children:2:cbExpand", "click");

        // The checkbox should have toggled the expanded model to false
        assertFalse("Checkbox click should have set expanded to false", dirA.expanded.getObject());

        // Children should no longer be rendered (lazy rendering removes them)
        dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals(
                "After collapsing via checkbox, children should not be rendered",
                0,
                ((RepeatingView) dirAChildren).size());
    }

    @Test
    public void testCollapseRemovesChildren() {
        // Start with dirA expanded
        dirA.expanded.setObject(true);

        tester.startComponentInPage(treeView);

        // Verify children are rendered
        Component dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals(2, ((RepeatingView) dirAChildren).size());

        // Collapse dirA
        dirA.expanded.setObject(false);
        tester.startComponentInPage(treeView);

        // Children should no longer be rendered
        dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals("After collapsing, children should not be rendered", 0, ((RepeatingView) dirAChildren).size());
    }

    @Test
    public void testExpandCollapseExpandCycle() {
        tester.startComponentInPage(treeView);

        // Initially collapsed - no children rendered
        Component dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals(0, ((RepeatingView) dirAChildren).size());

        // Expand
        dirA.expanded.setObject(true);
        tester.startComponentInPage(treeView);
        dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals(2, ((RepeatingView) dirAChildren).size());

        // Collapse
        dirA.expanded.setObject(false);
        tester.startComponentInPage(treeView);
        dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals(0, ((RepeatingView) dirAChildren).size());

        // Expand again - children should reappear
        dirA.expanded.setObject(true);
        tester.startComponentInPage(treeView);
        dirAChildren = tester.getComponentFromLastRenderedPage("treeView:rootView:1:children:2:children");
        assertEquals("Re-expanding should render children again", 2, ((RepeatingView) dirAChildren).size());
    }
}

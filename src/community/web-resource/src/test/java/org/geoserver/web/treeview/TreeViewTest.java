/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.treeview;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

/** @author Niels Charlier */
public class TreeViewTest extends GeoServerWicketTestSupport {

    private class MockNode implements TreeNode<Integer> {
        private static final long serialVersionUID = 1012858609071186910L;

        protected int data;
        protected MockNode parent;
        protected List<MockNode> children = new ArrayList<MockNode>();
        protected IModel<Boolean> expanded = new Model<Boolean>(false);

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
    }

    protected final MockNode one = new MockNode(1, null);
    protected final MockNode two = new MockNode(2, one);
    protected final MockNode three = new MockNode(3, one);
    protected final MockNode four = new MockNode(4, two);
    protected final MockNode five = new MockNode(5, one);
    protected TreeView<Integer> treeView;

    @Before
    public void initialize() {
        treeView = new TreeView<Integer>("treeView", one);
    }

    @Test
    public void testSelection() {
        // initially nothing selected
        assertTrue(treeView.getSelectedNodes().isEmpty());
        assertEquals(0, treeView.getSelectedViews().length);

        // select programmatically, without ajax
        treeView.setSelectedNode(four);
        assertArrayEquals(new Object[] {four}, treeView.getSelectedNodes().toArray());
        // automatic expand
        assertEquals(true, two.getExpanded().getObject());
        assertEquals(true, one.getExpanded().getObject());
        // view
        tester.startComponentInPage(treeView);
        assertEquals("4", treeView.getSelectedViews()[0].getId());
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:children:4:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));

        // to test selection listening:
        final AtomicBoolean fired = new AtomicBoolean();
        treeView.addSelectionListener(
                target -> {
                    fired.set(true);
                });

        // select programmatically, with ajax
        treeView.add(
                new AjaxEventBehavior("testSelectWithAjax") {
                    private static final long serialVersionUID = 4422989219680841271L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        treeView.setSelectedNode(three, target);
                    }
                });
        fired.set(false);
        tester.executeAjaxEvent(treeView, "testSelectWithAjax");
        assertTrue(fired.get());
        assertArrayEquals(new Object[] {three}, treeView.getSelectedNodes().toArray());
        assertEquals("3", treeView.getSelectedViews()[0].getId());
        assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:children:4:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));

        // select with gui
        fired.set(false);
        tester.executeAjaxEvent("treeView:rootView:1:children:2:label:selectableLabel", "click");
        assertTrue(fired.get());
        assertArrayEquals(new Object[] {two}, treeView.getSelectedNodes().toArray());
        assertEquals("2", treeView.getSelectedViews()[0].getId());
        assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:label:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));

        // automatic unselect when unexpanding
        tester.executeAjaxEvent("treeView:rootView:1:cbExpand", "click");
        assertEquals(false, one.getExpanded().getObject());
        assertTrue(treeView.getSelectedNodes().isEmpty());
        assertEquals(0, treeView.getSelectedViews().length);

        // multi-select toggle with ctrl
        tester.executeAjaxEvent("treeView:rootView:1:children:2:label:selectableLabel", "click");
        fired.set(false);
        tester.getRequest().addParameter("ctrl", "true");
        tester.executeAjaxEvent("treeView:rootView:1:children:3:selectableLabel", "click");
        assertTrue(fired.get());
        assertEquals(2, treeView.getSelectedNodes().size());
        assertTrue(treeView.getSelectedNodes().contains(two));
        assertTrue(treeView.getSelectedNodes().contains(three));
        assertEquals(2, treeView.getSelectedViews().length);
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:label:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        fired.set(false);
        tester.getRequest().addParameter("ctrl", "true");
        tester.executeAjaxEvent("treeView:rootView:1:children:2:label:selectableLabel", "click");
        assertTrue(fired.get());
        assertArrayEquals(new Object[] {three}, treeView.getSelectedNodes().toArray());
        assertEquals("3", treeView.getSelectedViews()[0].getId());
        assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:children:4:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));

        // multi-select with shift
        tester.executeAjaxEvent("treeView:rootView:1:children:2:label:selectableLabel", "click");
        fired.set(false);
        tester.getRequest().addParameter("shift", "true");
        tester.executeAjaxEvent("treeView:rootView:1:children:5:selectableLabel", "click");
        assertTrue(fired.get());
        assertEquals(3, treeView.getSelectedNodes().size());
        assertTrue(treeView.getSelectedNodes().contains(two));
        assertTrue(treeView.getSelectedNodes().contains(three));
        assertTrue(treeView.getSelectedNodes().contains(five));
        assertEquals(3, treeView.getSelectedViews().length);
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:5:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:label:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));

        // same but upside down
        tester.executeAjaxEvent("treeView:rootView:1:children:5:selectableLabel", "click");
        fired.set(false);
        tester.getRequest().addParameter("shift", "true");
        tester.executeAjaxEvent("treeView:rootView:1:children:2:label:selectableLabel", "click");
        assertTrue(fired.get());
        assertEquals(3, treeView.getSelectedNodes().size());
        assertTrue(treeView.getSelectedNodes().contains(two));
        assertTrue(treeView.getSelectedNodes().contains(three));
        assertTrue(treeView.getSelectedNodes().contains(five));
        assertEquals(3, treeView.getSelectedViews().length);
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:5:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:label:selectableLabel")
                        .getBehaviors()
                        .contains(TreeView.SELECTED_BEHAVIOR));
    }

    @Test
    public void testNearestView() {
        tester.startComponentInPage(treeView);
        MockNode five = new MockNode(5, four);
        assertEquals("4", treeView.getNearestView(five).getId());
        tester.startComponentInPage(treeView);
        assertEquals("5", treeView.getNearestView(five).getId());
    }

    @Test
    public void testMarks() {
        final String TESTMARK = "testMark";

        treeView.setSelectedNodes(Collections.emptySet());
        treeView.registerMark("testMark");
        assertNotNull(treeView.marks.get(TESTMARK));

        treeView.addMarked("testMark", two);
        treeView.addMarked("testMark", three);

        assertFalse(treeView.hasMark(TESTMARK, one));
        assertTrue(treeView.hasMark(TESTMARK, two));
        assertTrue(treeView.hasMark(TESTMARK, three));
        assertFalse(treeView.hasMark(TESTMARK, four));

        final AttributeAppender app = treeView.marks.get(TESTMARK).getBehaviour();

        tester.startComponentInPage(treeView);
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:label:selectableLabel")
                        .getBehaviors()
                        .contains(app));
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(app));

        treeView.clearMarked("testMark");
        assertFalse(treeView.hasMark(TESTMARK, two));
        assertFalse(treeView.hasMark(TESTMARK, three));

        tester.startComponentInPage(treeView);
        assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:2:label:selectableLabel")
                        .getBehaviors()
                        .contains(app));
        assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "treeView:rootView:1:children:3:selectableLabel")
                        .getBehaviors()
                        .contains(app));
    }
}

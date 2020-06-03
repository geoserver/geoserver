/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.treeview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentLabel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * A component to display a tree that can be navigated through (folders can be expanded and closed,
 * items can be selected)
 *
 * @author Niels Charlier
 */
public class TreeView<T> extends Panel {

    private static final long serialVersionUID = 2683470514874500599L;

    /** Behaviour for selected node */
    protected static final AttributeAppender SELECTED_BEHAVIOR =
            new ClassAppender(new Model<String>("selected"));

    /** Implement this to listen to selection events */
    public static interface SelectionListener<T> extends Serializable {
        public void onSelect(AjaxRequestTarget target);
    }

    /** The selection listeners */
    protected List<SelectionListener<T>> selectionListeners = new ArrayList<SelectionListener<T>>();

    /** Model for selected node */
    protected IModel<Collection<TreeNode<T>>> selectedNodeModel;

    /** Custom marked items */
    protected Map<String, Mark> marks = new HashMap<String, Mark>();

    /**
     * Constructor
     *
     * @param id component id
     * @param root the root node (creates model automatically)
     */
    public TreeView(String id, TreeNode<T> root) {
        this(id, new Model<TreeNode<T>>(root));
    }

    /**
     * Constructor
     *
     * @param id component id
     * @param rootModel model of the root node
     */
    public TreeView(String id, IModel<TreeNode<T>> rootModel) {
        this(id, rootModel, new CollectionModel<TreeNode<T>>(new HashSet<TreeNode<T>>()));
    }

    /**
     * Constructor
     *
     * @param id component id
     * @param rootModel model of the root node
     * @param selectedNodeModel model of the selected node
     */
    public TreeView(
            String id,
            IModel<TreeNode<T>> rootModel,
            IModel<Collection<TreeNode<T>>> selectedNodeModel) {
        super(id, rootModel);
        this.selectedNodeModel = selectedNodeModel;
        final RepeatingView rootView = new RepeatingView("rootView");
        rootView.add(createTreeNodeView(rootModel.getObject().getUniqueId(), rootModel));
        add(rootView);
        setOutputMarkupId(true);
    }

    /** Get the root Node. */
    @SuppressWarnings("unchecked")
    public TreeNode<T> getRootNode() {
        return (TreeNode<T>) getDefaultModelObject();
    }

    /** Get the selected Node Model. */
    public IModel<? extends Collection<TreeNode<T>>> getSelectedNodeModel() {
        return selectedNodeModel;
    }

    /** Get the selected Nodes. */
    public Collection<TreeNode<T>> getSelectedNodes() {
        return Collections.unmodifiableCollection(selectedNodeModel.getObject());
    }

    /** Get the selected Node if it is single, null otherwise. */
    public TreeNode<T> getSelectedNode() {
        if (selectedNodeModel.getObject().size() == 1) {
            return selectedNodeModel.getObject().iterator().next();
        }
        return null;
    }

    /**
     * Get the view for the selected node. Refreshing this view refreshes all child nodes as well.
     * It is efficient to only refresh the necessary node(s) on ajax requests, rather than the whole
     * tree.
     */
    public Panel[] getSelectedViews() {
        List<Panel> views = new ArrayList<Panel>();
        if (!selectedNodeModel.getObject().isEmpty()) {
            for (TreeNode<T> selectedNode : selectedNodeModel.getObject()) {
                views.add(getNearestViewInternal(selectedNode));
            }
            return views.toArray(new Panel[views.size()]);
        } else {
            return new Panel[] {};
        }
    }

    /**
     * Get the view for a node, if there is not yet a view for this node, it will return the nearest
     * parent node. Refreshing this view refreshes all child nodes as well. It is efficient to only
     * refresh the necessary node(s) on ajax requests, rather than the whole tree.
     */
    public Panel getNearestView(TreeNode<T> node) {
        return getNearestViewInternal(node);
    }

    /** Add a selection listener */
    public void addSelectionListener(SelectionListener<T> listener) {
        selectionListeners.add(listener);
    }

    /**
     * Change selected node, expand if necessary. Events aren't called. Caller is responsible for
     * refreshing the view(s).
     */
    public void setSelectedNodes(Collection<TreeNode<T>> selectedNodes) {
        setSelectedNodes(selectedNodes, null);
    }

    /** Change selected node, expand if necessary, fire events, and refresh the whole treeview. */
    public void setSelectedNodes(Collection<TreeNode<T>> selectedNodes, AjaxRequestTarget target) {
        // expand if necessary
        for (TreeNode<T> selectedNode : selectedNodes) {
            if (selectedNode != null) {
                TreeNode<T> node = selectedNode.getParent();
                while (node != null) {
                    node.getExpanded().setObject(true);
                    node = node.getParent();
                }
            }
        }
        setSelectedNodesInternal(selectedNodes, target);
        if (target != null) {
            target.add(this);
        }
    }

    /**
     * Change selected node (single), expand if necessary. Events aren't called. Caller is
     * responsible for refreshing the view(s).
     */
    public void setSelectedNode(TreeNode<T> selectedNode) {
        setSelectedNode(selectedNode, null);
    }

    /**
     * Change selected node (single), expand if necessary, fire events, and refresh the whole
     * treeview.
     */
    public void setSelectedNode(TreeNode<T> selectedNode, AjaxRequestTarget target) {
        setSelectedNodes(
                selectedNode == null ? Collections.emptySet() : Collections.singleton(selectedNode),
                target);
    }

    /**
     * Test if node has been selected.
     *
     * @param node node to test.
     * @return whether node is selected or not.
     */
    public boolean isSelected(TreeNode<T> node) {
        for (TreeNode<T> selectedNode : selectedNodeModel.getObject()) {
            if (selectedNode.isSameAs(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Register a customised mark. The style of the mark can be defined in CSS with ".css-treeview
     * a.{markName}"
     *
     * @param markName name of the mark
     */
    public void registerMark(String markName) {
        marks.put(markName, new Mark(markName));
    }

    /**
     * Add marked node to a mark.
     *
     * @param markName name of the mark
     * @param node node to be added
     */
    public void addMarked(String markName, TreeNode<T> node) {
        marks.get(markName).getMarked().add(node.getUniqueId());
    }

    /**
     * Clear all marked nodes from a mark.
     *
     * @param markName name of the mark to be cleared
     */
    public void clearMarked(String markName) {
        marks.get(markName).getMarked().clear();
    }

    /**
     * Check if a node has a mark
     *
     * @param markName name of the mark
     * @param node the node that may or may not be marked
     * @return whether the mode is marked
     */
    public boolean hasMark(String markName, TreeNode<T> node) {
        return marks.get(markName).getMarked().contains(node.getUniqueId());
    }

    // ------------------- internal methods/classes

    /** Select the node without automatic expansion but with event dispatching. */
    protected void setSelectedNodesInternal(
            Collection<TreeNode<T>> selectedNodes, AjaxRequestTarget target) {
        selectedNodeModel.setObject(selectedNodes);
        if (target != null) {
            for (SelectionListener<T> listener : selectionListeners) {
                listener.onSelect(target);
            }
        }
    }

    /**
     * Get the view for a node, if there is not yet a view for this node, it will return the nearest
     * parent node. Refreshing this view refreshes all child nodes as well. It is efficient to only
     * refresh the necessary node(s) on ajax requests, rather than the whole tree.
     */
    protected TreeNodeView getNearestViewInternal(TreeNode<T> node) {
        TreeNode<T> parent = node.getParent();
        if (parent == null) {
            return getRoot();
        } else {
            TreeNodeView parentView = getNearestViewInternal(parent);
            if (parentView.getNode().isSameAs(parent)) {
                TreeNodeView childView = parentView.getChildView(node.getUniqueId());
                if (childView != null) {
                    return childView;
                }
            }
            return parentView;
        }
    }

    /** Get the root view. */
    @SuppressWarnings("unchecked")
    protected TreeNodeView getRoot() {
        return (TreeNodeView) get("rootView").get(getRootNode().getUniqueId());
    }

    /**
     * Create a view from a node
     *
     * @param id wicket id for view
     * @param node the node
     * @return the view
     */
    protected TreeNodeView createTreeNodeView(String id, IModel<TreeNode<T>> node) {
        if (node.getObject().isLeaf()) {
            return new TreeLeafView(id, node);
        } else {
            return new TreeExpandableNodeView(id, node);
        }
    }

    /**
     * View for one tree node (base)
     *
     * @author Niels Charlier
     */
    protected abstract class TreeNodeView extends Panel {

        private static final long serialVersionUID = 2940674057639126436L;

        protected Component selectableLabel;

        public TreeNodeView(String id, IModel<TreeNode<T>> nodeModel) {
            super(id, nodeModel);
            setOutputMarkupId(true);
        }

        @SuppressWarnings("unchecked")
        public TreeNode<T> getNode() {
            return (TreeNode<T>) getDefaultModelObject();
        }

        protected Component createSelectableLabel() {
            return selectableLabel =
                    new Label("selectableLabel", getNode().getLabel())
                            .add(
                                    new AjaxEventBehavior("click") {
                                        private static final long serialVersionUID =
                                                -3705747320247194977L;

                                        @Override
                                        protected void updateAjaxAttributes(
                                                AjaxRequestAttributes attributes) {
                                            super.updateAjaxAttributes(attributes);
                                            attributes
                                                    .getDynamicExtraParameters()
                                                    .add(
                                                            "return {'ctrl' : attrs.event.ctrlKey, 'shift' : attrs.event.shiftKey}");
                                            attributes.setPreventDefault(true);
                                        }

                                        @Override
                                        public void onEvent(AjaxRequestTarget target) {
                                            boolean shift =
                                                    RequestCycle.get()
                                                            .getRequest()
                                                            .getRequestParameters()
                                                            .getParameterValue("shift")
                                                            .toBoolean();
                                            boolean ctrl =
                                                    RequestCycle.get()
                                                            .getRequest()
                                                            .getRequestParameters()
                                                            .getParameterValue("ctrl")
                                                            .toBoolean();

                                            if (ctrl) { // toggle selection of this node
                                                Set<TreeNode<T>> newSelectedNodes =
                                                        new HashSet<TreeNode<T>>();
                                                if (isSelected(getNode())) {
                                                    for (TreeNode<T> selectedNode :
                                                            getSelectedNodes()) {
                                                        if (!selectedNode.isSameAs(getNode())) {
                                                            newSelectedNodes.add(selectedNode);
                                                        }
                                                    }
                                                } else {
                                                    newSelectedNodes.addAll(getSelectedNodes());
                                                    newSelectedNodes.add(getNode());
                                                }
                                                setSelectedNodesInternal(newSelectedNodes, target);
                                            } else if (shift) { // group select to nearest sibling
                                                boolean select = false;
                                                boolean moveOn = false;
                                                Set<TreeNode<T>> newSelectedNodes =
                                                        new HashSet<TreeNode<T>>(
                                                                getSelectedNodes());
                                                for (TreeNode<T> sibling :
                                                        getNode().getParent().getChildren()) {
                                                    if (!select
                                                            && (sibling.isSameAs(getNode())
                                                                    || isSelected(sibling))) {
                                                        select = true;
                                                        moveOn =
                                                                !sibling.isSameAs(
                                                                        getNode()); // we _must_
                                                        // move on to
                                                        // clicked node
                                                    } else if (select
                                                            && (sibling.isSameAs(getNode())
                                                                    || (!moveOn
                                                                            && isSelected(
                                                                                    sibling)))) {
                                                        select = false;
                                                        break;
                                                    }
                                                    if (select) {
                                                        newSelectedNodes.add(sibling);
                                                        target.add(getNearestViewInternal(sibling));
                                                    }
                                                }
                                                if (!select) {
                                                    newSelectedNodes.add(getNode());
                                                    setSelectedNodesInternal(
                                                            newSelectedNodes, target);
                                                } // if we never went out of select, there was no
                                                // selected sibling to being with and we are
                                                // just going to ignore this.
                                            } else {
                                                // replace selection, old one is removed
                                                target.add(getSelectedViews());
                                                setSelectedNodesInternal(
                                                        Collections.singleton(getNode()), target);
                                            }
                                            target.add(TreeNodeView.this);
                                        }
                                    })
                            .setOutputMarkupId(true);
        }

        public TreeNodeView getChildView(String id) {
            return null;
        }

        @Override
        protected void onBeforeRender() {
            super.onBeforeRender();
            if (selectableLabel.getBehaviors().contains(SELECTED_BEHAVIOR)) {
                if (!isSelected(getNode())) {
                    selectableLabel.remove(SELECTED_BEHAVIOR);
                }
            } else {
                if (isSelected(getNode())) {
                    selectableLabel.add(SELECTED_BEHAVIOR);
                }
            }
            for (Mark mark : marks.values()) {
                if (selectableLabel.getBehaviors().contains(mark.getBehaviour())) {
                    if (!mark.getMarked().contains(getNode().getUniqueId())) {
                        selectableLabel.remove(mark.getBehaviour());
                    }
                } else {
                    if (mark.getMarked().contains(getNode().getUniqueId())) {
                        selectableLabel.add(mark.getBehaviour());
                    }
                }
            }
        }
    }

    /** View for an expandable tree node (directory node) */
    protected class TreeExpandableNodeView extends TreeNodeView {

        private static final long serialVersionUID = 2940674057639126436L;

        public TreeExpandableNodeView(String id, IModel<TreeNode<T>> nodeModel) {
            super(id, nodeModel);
            final AjaxCheckBox cbExpand =
                    new AjaxCheckBox("cbExpand", nodeModel.getObject().getExpanded()) {
                        private static final long serialVersionUID = 7602857423814264211L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            if (!getModelObject() && !selectedNodeModel.getObject().isEmpty()) {
                                // if any nodes in the current selection become hidden, we unselect
                                // them automatically
                                Set<TreeNode<T>> newSelectedNodes = new HashSet<TreeNode<T>>();
                                for (TreeNode<T> selectedNode : selectedNodeModel.getObject()) {
                                    TreeNode<T> node = selectedNode.getParent();
                                    boolean selectionHidden = false;
                                    while (!selectionHidden
                                            && node != null) { // loop through parents
                                        if (node.isSameAs(nodeModel.getObject())) {
                                            selectionHidden = true;
                                        }
                                        node = node.getParent();
                                    }
                                    if (!selectionHidden) {
                                        newSelectedNodes.add(selectedNode);
                                    }
                                }
                                if (newSelectedNodes.size()
                                        != selectedNodeModel.getObject().size()) {
                                    setSelectedNodesInternal(newSelectedNodes, target);
                                    target.add(TreeExpandableNodeView.this);
                                }
                            }
                        }
                    };
            add(cbExpand);
            add(new FormComponentLabel("label", cbExpand).add(createSelectableLabel()));
            // add(new FormComponentLabel("label", cbExpand));
            // add(createSelectableLabel());
            add(new RepeatingView("children"));
        }

        @Override
        protected void onBeforeRender() {
            super.onBeforeRender();
            final RepeatingView children = (RepeatingView) get("children");
            children.removeAll();
            for (TreeNode<T> child : getNode().getChildren()) {
                children.add(
                        createTreeNodeView(child.getUniqueId(), new Model<TreeNode<T>>(child)));
            }
        }

        @SuppressWarnings("unchecked")
        public TreeNodeView getChildView(String id) {
            return (TreeNodeView) ((RepeatingView) get("children")).get(id);
        }
    }

    /** View for an tree node leaf */
    protected class TreeLeafView extends TreeNodeView {

        private static final long serialVersionUID = 2940674057639126436L;

        public TreeLeafView(String id, IModel<TreeNode<T>> nodeModel) {
            super(id, nodeModel);
            add(createSelectableLabel());
        }
    }

    /** Custom mark data */
    protected static class Mark implements Serializable {
        private static final long serialVersionUID = -827616908801489309L;

        private AttributeAppender behaviour;
        private Set<String> marked = new HashSet<String>();

        public Mark(String behaviourName) {
            this.behaviour = new AttributeAppender("class", new Model<String>(behaviourName), " ");
        }

        public AttributeAppender getBehaviour() {
            return behaviour;
        }

        public Set<String> getMarked() {
            return marked;
        }
    }
}

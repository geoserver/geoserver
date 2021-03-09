/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.panel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.CheckedFolder;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.extensions.markup.html.repeater.util.TreeModelProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.SetModel;

/** Panel that includes a NestedTree wicket component and a label. */
public class NestedTreePanel extends Panel {

    private static final long serialVersionUID = 1L;

    private Label label;
    private NestedTree<DefaultMutableTreeNode> tree;
    private TreeModelProvider<DefaultMutableTreeNode> modelProvider;

    public NestedTreePanel(
            final String id,
            final IModel<Serializable> paramValue,
            final IModel<String> paramLabelModel,
            final List<? extends Serializable> options,
            final boolean required) {

        super(id, paramValue);
        // label
        String requiredMark = required ? " *" : "";
        label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);
        // tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        final Set<DefaultMutableTreeNode> checkedNodes = new HashSet<>();
        this.buildTree(treeModel, checkedNodes);
    }

    public void buildTree(DefaultTreeModel treeModel, Set<DefaultMutableTreeNode> checkedNodes) {
        modelProvider =
                new TreeModelProvider<DefaultMutableTreeNode>(treeModel) {
                    @Override
                    public IModel<DefaultMutableTreeNode> model(DefaultMutableTreeNode object) {
                        IModel<DefaultMutableTreeNode> model = Model.of(object);
                        return model;
                    }
                };
        final SetModel<DefaultMutableTreeNode> checkedNodesModel = new SetModel<>(checkedNodes);
        tree =
                new NestedTree<DefaultMutableTreeNode>(
                        "paramValue", modelProvider, checkedNodesModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Component newContentComponent(
                            String id, IModel<DefaultMutableTreeNode> model) {
                        CheckedFolder<DefaultMutableTreeNode> ret =
                                new AutocheckedFolder<>(id, this, model, checkedNodesModel);
                        return ret;
                    }
                };
        tree.add(new WindowsTheme());
        tree.setOutputMarkupId(true);

        label.setVisible(true);
        tree.setVisible(true);
        int rootNodeChildCount = treeModel.getChildCount(treeModel.getRoot());
        // if no childs at root level then hide the tree
        if (rootNodeChildCount == 0) {
            label.setVisible(false);
            tree.setVisible(false);
        }

        addOrReplace(tree);
    }
}

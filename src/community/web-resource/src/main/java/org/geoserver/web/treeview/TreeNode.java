/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.treeview;

import java.io.Serializable;
import java.util.Collection;
import org.apache.wicket.model.IModel;

/**
 * Model for a node in a Tree. This interface must be implemented to make your own custom TreeView.
 *
 * @author Niels Charlier
 */
public interface TreeNode<T> extends Serializable {

    /**
     * Get collection of children (in order of display)
     *
     * @return collection of children
     */
    public Collection<? extends TreeNode<T>> getChildren();

    /**
     * Get the parent.
     *
     * @return the parent of the node (null if root).
     */
    public TreeNode<T> getParent();

    /**
     * Get node data object.
     *
     * @return data object
     */
    public T getObject();

    /**
     * Provide a model for whether node is expanded or not. Must contain the same value for each
     * instance representing the same node.
     *
     * @return expanded model, null if the node is a leaf.
     */
    public IModel<Boolean> getExpanded();

    /**
     * Provide a unique ID for this node.
     *
     * @return unique id
     */
    public String getUniqueId();

    /**
     * Determine if the node is a leaf.
     *
     * @return true if the node is a leaf
     */
    default boolean isLeaf() {
        return getChildren().size() == 0;
    }

    /**
     * Get label (textual representation) of node.
     *
     * @return textual representation of node
     */
    default String getLabel() {
        return getObject().toString();
    }

    /** Determine if this is the same node (all fields must be the same as well) */
    default boolean isSameAs(TreeNode<T> node) {
        return (getUniqueId().equals(node.getUniqueId()));
    }
}

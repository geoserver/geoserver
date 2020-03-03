/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.treeview.TreeNode;
import org.geoserver.web.treeview.TreeView;

/**
 * Holds Clipboard information for Resource TreeView and updates marks accordingly.
 *
 * @author Niels Charlier
 */
public class ClipBoard implements Serializable {

    private static final long serialVersionUID = -5996563694112082364L;

    private static final String MARK_COPY = "copy";

    private static final String MARK_CUT = "cut";

    /** The associated Resource TreeView */
    protected TreeView<Resource> treeView;

    /** The resource nodes on the clipboard */
    protected Set<TreeNode<Resource>> items = new HashSet<TreeNode<Resource>>();

    /** Whether resource node was cut or copied. */
    protected boolean clipBoardIsCopy;

    /**
     * Constructor.
     *
     * @param treeView the tree view
     */
    public ClipBoard(TreeView<Resource> treeView) {
        this.treeView = treeView;
        treeView.registerMark(MARK_CUT);
        treeView.registerMark(MARK_COPY);
    }

    /**
     * Get the resource node that is on the clipboard.
     *
     * @return the resource node on the clipboard.
     */
    public Set<TreeNode<Resource>> getItems() {
        return Collections.unmodifiableSet(items);
    }

    /**
     * Get the cut/copy state.
     *
     * @return Whether resource node was cut or copied.
     */
    public boolean isCopy() {
        return clipBoardIsCopy;
    }

    /**
     * Put new nodes on the clipboard, replacing anything that is currently on it (or erase it).
     *
     * @param node the new cut/copied resource node
     * @param clipBoardIsCopy the cut/copy state
     * @param target ajaxrequesttarget to update the marks on the view
     */
    public void setItems(
            Collection<? extends TreeNode<Resource>> nodes,
            boolean clipBoardIsCopy,
            AjaxRequestTarget target) {
        treeView.clearMarked(MARK_CUT);
        treeView.clearMarked(MARK_COPY);
        for (TreeNode<Resource> clipBoardItem : items) {
            target.add(treeView.getNearestView(clipBoardItem));
        }
        items.clear();
        items.addAll(nodes);
        this.clipBoardIsCopy = clipBoardIsCopy;
        for (TreeNode<Resource> clipBoardItem : items) {
            treeView.addMarked(clipBoardIsCopy ? MARK_COPY : MARK_CUT, clipBoardItem);
        }
    }

    /**
     * Put new nodes on the clipboard, replacing anything that is currently on it (or erase it). The
     * cut/copy state is left unchanged.
     *
     * @param node the new cut/copied resource node ("null" to erase clipboard)
     * @param target ajaxrequesttarget to update the marks on the view
     */
    public void setItems(Collection<? extends TreeNode<Resource>> nodes, AjaxRequestTarget target) {
        setItems(nodes, clipBoardIsCopy, target);
    }

    /**
     * Add a node to the current clipboard.
     *
     * @param node the new cut/copied resource node
     * @param clipBoardIsCopy the cut/copy state
     * @param target ajaxrequesttarget to update the marks on the view
     */
    public void addItem(TreeNode<Resource> clipBoardItem, AjaxRequestTarget target) {
        target.add(treeView.getNearestView(clipBoardItem));
        items.add(clipBoardItem);
        treeView.addMarked(clipBoardIsCopy ? MARK_COPY : MARK_CUT, clipBoardItem);
    }

    /** Clear all non-existing resources from the clip board. */
    public void clearRemoved() {
        Set<TreeNode<Resource>> newClipboard = new HashSet<TreeNode<Resource>>();
        for (TreeNode<Resource> node : items) {
            if (Resources.exists(node.getObject())) {
                newClipboard.add(node);
            }
        }
        items = newClipboard;
    }
}

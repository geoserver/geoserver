/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.treeview.TreeNode;

/**
 * Implementation of TreeNode for Resource
 *
 * @author Niels Charlier
 */
public class ResourceNode implements TreeNode<Resource>, Comparable<ResourceNode> {

    private static final long serialVersionUID = 4941394483034830079L;

    private Resource resource;

    private ResourceExpandedStates expandedStates;

    private String uniqueId;

    public ResourceNode(Resource resource, ResourceExpandedStates expandedStates) {
        this.resource = Resources.serializable(resource);
        this.expandedStates = expandedStates;
        this.uniqueId = getNodeId(resource);
    }

    /**
     * It's handy to use the path as the id, however two characters are forbidden in Wicket ids,
     * <code>:</code> and <code>~</code>. If those are present, switch to a SHA-256 instead
     * (likelyness of conflict is astronomically low, provides better chances thatn just replacing
     * the forbidden chars with a fixed sane replacement).
     */
    private String getNodeId(Resource resource) {
        String path = resource.path();
        if (path.isEmpty()) {
            return "/";
        } else if (path.contains(":") || path.contains("~")) {
            return Hashing.sha256().hashString(path, StandardCharsets.UTF_8).toString();
        } else {
            return path;
        }
    }

    @Override
    public Set<TreeNode<Resource>> getChildren() {
        Set<TreeNode<Resource>> children = new TreeSet<TreeNode<Resource>>();
        for (Resource res : resource.list()) {
            children.add(new ResourceNode(res, expandedStates));
        }
        return children;
    }

    @Override
    public TreeNode<Resource> getParent() {
        if (resource.name().isEmpty()) {
            return null;
        } else {
            return new ResourceNode(resource.parent(), expandedStates);
        }
    }

    @Override
    public Resource getObject() {
        return resource;
    }

    @Override
    public String getLabel() {
        String name = resource.name();
        if (name.isEmpty()) {
            return "/";
        } else {
            return name;
        }
    }

    @Override
    public boolean isLeaf() {
        return resource.getType() == Resource.Type.RESOURCE;
    }

    @Override
    public IModel<Boolean> getExpanded() {
        if (isLeaf()) {
            return null;
        } else {
            return expandedStates.getResourceExpandedState(resource);
        }
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public int compareTo(ResourceNode other) {
        int i = resource.name().compareTo(other.resource.name());
        if (i == 0 && resource.parent() != null) {
            i = resource.parent().name().compareTo(other.resource.parent().name());
        }
        return i;
    }

    @Override
    public boolean equals(Object node) {
        return node instanceof ResourceNode && isSameAs((ResourceNode) node);
    }

    @Override
    public int hashCode() {
        return getUniqueId().hashCode();
    }
}

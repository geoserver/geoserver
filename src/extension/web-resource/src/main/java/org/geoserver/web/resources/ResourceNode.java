/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.TreeSet;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.resource.Paths;
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
        if (Paths.isAbsolute(resource.path())) {
            // double check resource browser cannot be used to edit
            // absolute path locations
            throw new IllegalStateException("Path location not supported by Resource Browser");
        }
        this.resource = Resources.serializable(resource);
        this.expandedStates = expandedStates;
        this.uniqueId = getUniqueId(this.resource.path());
    }

    public static String getUniqueId(String path) {
        if (path.isEmpty()) {
            // top-level unique id used for wicket selection, not intended as a path
            return "/";
        } else if (path.contains(":") || path.contains("~")) {
            // Base64 encode the file path to replace special characters that are
            // not allowed in Wicket component IDs.
            byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
            return Base64.getUrlEncoder().encodeToString(bytes);
        } else {
            // Helps prevent duplicate Wicket IDs if there is a filename that
            // is the Base64 encoded path of a file that has to be encoded
            // without having to unnecessarily encode everything.
            return path;
        }
    }

    @Override
    public Set<TreeNode<Resource>> getChildren() {
        Set<TreeNode<Resource>> children = new TreeSet<>();
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

    @Override
    public String toString() {
        return "ResourceNode " + "'" + uniqueId + "'";
    }
}

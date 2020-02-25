/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Represents a hierarchical security tree node. The tree as a whole is represented by its root
 *
 * @author Andrea Aime - TOPP
 */
public class SecureTreeNode {

    /** Special role set used to mean every possible role in the system */
    static final Set<String> EVERYBODY = Collections.singleton("*");

    /** The role given to the administrators */
    static final String ROOT_ROLE = GeoServerRole.ADMIN_ROLE.getAuthority();

    /** Depth or the security tree root */
    static int ROOT_DEPTH = 0;

    /** Depth of a workspace/global group rule */
    static int WS_LG_DEPTH = 1;

    /** Depth of a resource specific rule */
    static int RESOURCE_DEPTH = 2;

    Map<String, SecureTreeNode> children = new HashMap<String, SecureTreeNode>();

    SecureTreeNode parent;

    /**
     * A map from access mode to set of roles that can perform that kind of access. Given a certain
     * access mode, the intepretation of the associated set of roles is:
     *
     * <ul>
     *   <li>roles set null: no local rule, fall back on the parent of this node
     *   <li>roles set empty: nobody can perform this kind of access
     *   <li>roles list equals to {@link #EVERYBODY}: everybody can perform accesses in that mode
     *   <li>otherwise: only users having at least one granted authority matching one of the roles
     *       in the set can access
     * </ul>
     */
    Map<AccessMode, Set<String>> authorizedRoles = new HashMap<AccessMode, Set<String>>();

    /** Builds a child of the specified parent node */
    private SecureTreeNode(SecureTreeNode parent) {
        this.parent = parent;
        // no rule specified, full fall back on the node's parent
    }

    /** Builds the root of a security tree */
    public SecureTreeNode() {
        // by default we allow access for everybody in all modes for the root
        // node, since we have no parent to fall back onto
        // -> except for admin access, default is administrator
        for (AccessMode mode : AccessMode.values()) {
            switch (mode) {
                case ADMIN:
                    authorizedRoles.put(mode, Collections.singleton(ROOT_ROLE));
                    break;
                default:
                    authorizedRoles.put(mode, EVERYBODY);
            }
        }
    }

    /** Returns a child with the specified name, or null */
    public SecureTreeNode getChild(String name) {
        return children.get(name);
    }

    /** Adds a child to this path element */
    public SecureTreeNode addChild(String name) {
        if (getChild(name) != null)
            throw new IllegalArgumentException(
                    "This pathElement " + name + " is already among my children");

        SecureTreeNode child = new SecureTreeNode(this);
        children.put(name, child);
        return child;
    }

    /**
     * Tells if the user is allowed to access the PathElement with the specified access mode. If no
     * information can be found in the current node, the decision is delegated to the parent. If the
     * root is reached and it has no security definition, access will be granted. Otherwise, the
     * first path element with a role list for the specified access mode will return true if the
     * user has a {@link GrantedAuthority} matching one of the specified roles, false otherwise
     */
    public boolean canAccess(Authentication user, AccessMode mode) {
        Set<String> roles = getAuthorizedRoles(mode);

        if (GeoServerSecurityFilterChainProxy.isSecurityEnabledForCurrentRequest() == false)
            return true;

        // if we don't know, we ask the parent, otherwise we assume
        // the object is unsecured
        if (roles == null) {
            return parent.canAccess(user, mode);
        }

        // if the roles is just "*" any granted authority will match
        if (roles.equals(EVERYBODY)) return true;

        // let's scan thru the the authorities granted to the user and
        // see if he matches any of the write roles
        if (user == null || user.getAuthorities() == null) return false;
        // look for a match on the roles, using the "root" rules as well (root can do everything)
        for (GrantedAuthority authority : user.getAuthorities()) {
            final String userRole = authority.getAuthority();
            if (roles.contains(userRole) || ROOT_ROLE.equals(userRole)) return true;
        }
        return false;
    }

    /**
     * Returns the authorized roles for the specified access mode. The collection can be null if we
     * don't have a rule, meaning the rule will have to searched in the parent node
     */
    public Set<String> getAuthorizedRoles(AccessMode mode) {
        return authorizedRoles.get(mode);
    }

    /** Sets the authorized roles for the specified access mode */
    public void setAuthorizedRoles(AccessMode mode, Set<String> roles) {
        authorizedRoles.put(mode, roles);
    }

    /**
     * Utility method that drills down from the current node using the specified list of child
     * names, and returns the latest element found along that path (might not be correspondent to
     * the full path specified, security paths can be incomplete, the definition of the parent
     * applies to the missing children as well)
     */
    public SecureTreeNode getDeepestNode(String... pathElements) {
        SecureTreeNode curr = this;
        SecureTreeNode result = this;
        for (int i = 0; i < pathElements.length; i++) {
            final SecureTreeNode next = curr.getChild(pathElements[i]);
            if (next == null) {
                return result;
            } else {
                curr = next;
                // don't return info about a node that has no explicit
                // rule associated, the parent will do
                if (curr.authorizedRoles != null && !curr.authorizedRoles.isEmpty()) {
                    result = curr;
                }
            }
        }
        return curr;
    }

    /**
     * Utility method that drills down from the current node using the specified list of child
     * names, and returns an element only if it fully matches the provided path
     */
    public SecureTreeNode getNode(String... pathElements) {
        SecureTreeNode curr = this;
        for (int i = 0; i < pathElements.length; i++) {
            final SecureTreeNode next = curr.getChild(pathElements[i]);
            if (next == null) {
                return null;
            } else {
                curr = next;
            }
        }
        return curr;
    }

    /** The children of this secure tree node */
    Map<String, SecureTreeNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        // customized toString to avoid printing the whole tree recursively, this one prints only
        // the info in the current level
        return "SecureTreeNode [childrenCount="
                + children.size()
                + ", hasParent="
                + (parent != null)
                + ", authorizedRoles="
                + authorizedRoles
                + "]";
    }

    /**
     * Returns the node depth, 0 is the root, 1 is a workspace/global layer one, 2 is layer specific
     */
    int getDepth() {
        int depth = 0;
        Set<SecureTreeNode> visited = new HashSet<>();
        SecureTreeNode n = this;
        while (n.parent != null && !visited.contains(n.parent)) {
            depth++;
            visited.add(n);
            n = n.parent;
        }

        return depth;
    }
}

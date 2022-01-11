/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.security;

import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.SecureTreeNode;
import org.opengis.feature.type.Name;
import org.springframework.security.core.Authentication;

/**
 * Default implementation of WPS access manager based on wps.xml configuration file. Builds {@link
 * #SecureTreeNode} to manage roles hierarchy as in {@link #DefaultDataAccessManager}
 */
public class DefaultProcessAccessManager implements ProcessAccessManager {

    private WpsAccessRuleDAO dao;

    long lastLoaded = Long.MIN_VALUE;

    public DefaultProcessAccessManager(WpsAccessRuleDAO dao) {
        this.dao = dao;
    }

    public CatalogMode getMode() {
        return dao.getMode();
    }

    @Override
    public ProcessAccessLimits getAccessLimits(Authentication user, String namespace) {
        SecureTreeNode node = dao.getSecurityTreeRoot().getDeepestNode(new String[] {namespace});
        return new ProcessAccessLimits(
                dao.getMode(), node.canAccess(user, AccessMode.READ), namespace);
    }

    @Override
    public ProcessAccessLimits getAccessLimits(Authentication user, Name process) {
        SecureTreeNode node =
                dao.getSecurityTreeRoot()
                        .getDeepestNode(
                                new String[] {process.getNamespaceURI(), process.getLocalPart()});
        return new ProcessAccessLimits(
                dao.getMode(), node.canAccess(user, AccessMode.READ), process.toString());
    }
}

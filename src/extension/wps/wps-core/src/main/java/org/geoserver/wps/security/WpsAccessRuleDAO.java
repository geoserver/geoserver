/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.security;

import static org.geoserver.security.impl.DataAccessRule.ANY;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.SecureTreeNode;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

/** Allows to manage the rules used by the WPS security subsystem */
public class WpsAccessRuleDAO extends ConfigurationListenerAdapter {

    private static final Logger LOGGER = Logging.getLogger(WpsAccessRuleDAO.class);

    /** property file name */
    static final String WPS_PROP_FILE = "wps.xml";

    private GeoServer gs;

    /** Default to the highest security mode */
    CatalogMode catalogMode = CatalogMode.HIDE;

    List<WpsAccessRule> rules;

    private SecureTreeNode root;

    public WpsAccessRuleDAO(GeoServer gs) throws IOException {
        this.gs = gs;
        gs.addListener(this);
    }

    public CatalogMode getMode() {
        if (root == null) {
            loadRules();
        }
        return catalogMode;
    }

    public SecureTreeNode getSecurityTreeRoot() {
        if (root == null) {
            loadRules();
        }
        return root;
    }

    /*
     * Loads rules from in memory WPSInfo and builds the WPS access rules tree
     */
    protected void loadRules() {
        WPSInfo wps = this.gs.getService(WPSInfo.class);
        TreeSet<WpsAccessRule> result = new TreeSet<WpsAccessRule>();

        if (wps != null) {
            catalogMode = CatalogMode.HIDE;
            if (wps.getCatalogMode() != null) {
                catalogMode = wps.getCatalogMode();
            }
            for (ProcessGroupInfo group : wps.getProcessGroups()) {
                Set<String> prefixes = new HashSet<String>();
                ProcessFactory pf =
                        GeoServerProcessors.getProcessFactory(group.getFactoryClass(), false);
                if (pf != null) {
                    Set<Name> names = pf.getNames();
                    for (Name name : names) {
                        prefixes.add(name.getNamespaceURI());
                    }
                }

                for (String prefix : prefixes) {
                    if (group.getRoles() != null && !group.getRoles().isEmpty()) {
                        result.add(
                                new WpsAccessRule(
                                        prefix, ANY, new HashSet<String>(group.getRoles())));
                    }
                }
                for (ProcessInfo process : group.getFilteredProcesses()) {
                    if (process.getRoles() != null && !process.getRoles().isEmpty()) {
                        result.add(
                                new WpsAccessRule(
                                        process.getName().getNamespaceURI(),
                                        process.getName().getLocalPart(),
                                        new HashSet<String>(process.getRoles())));
                    }
                }
            }
        }
        // make sure the single basic rules if the set is empty
        if (result.size() == 0) {
            result.add(new WpsAccessRule(WpsAccessRule.EXECUTE_ALL));
        }

        root = buildAuthorizationTree(result);
    }

    private SecureTreeNode buildAuthorizationTree(Collection<WpsAccessRule> rules) {
        SecureTreeNode root = new SecureTreeNode();
        for (WpsAccessRule rule : rules) {
            String group = rule.getGroupName();
            String name = rule.getWpsName();

            // look for the node where the rules will have to be set
            SecureTreeNode node;

            // check for the * group definition
            if (ANY.equals(group)) {
                node = root;
            } else {
                // get or create the group
                SecureTreeNode ws = root.getChild(group);
                if (ws == null) {
                    ws = root.addChild(group);
                }

                // if WPS is "*" the rule applies to the group, otherwise
                // get/create the WPS
                if ("*".equals(name)) {
                    node = ws;
                } else {
                    SecureTreeNode layerNode = ws.getChild(name);
                    if (layerNode == null) layerNode = ws.addChild(name);
                    node = layerNode;
                }
            }

            // actually set the rule, but don't complain for the default root contents
            if (node != root) {
                LOGGER.warning(
                        "Rule "
                                + rule
                                + " is overriding another rule targetting the same resource");
            }
            node.setAuthorizedRoles(AccessMode.READ, rule.getRoles());
            node.setAuthorizedRoles(AccessMode.WRITE, Collections.singleton("NO_ONE"));
            node.setAuthorizedRoles(AccessMode.ADMIN, Collections.singleton("NO_ONE"));
        }
        root.setAuthorizedRoles(AccessMode.READ, Collections.singleton("*"));
        root.setAuthorizedRoles(AccessMode.WRITE, Collections.singleton("NO_ONE"));
        root.setAuthorizedRoles(AccessMode.ADMIN, Collections.singleton("NO_ONE"));
        return root;
    }

    @Override
    public void reloaded() {
        this.root = null;
    }

    @Override
    public void handlePostServiceChange(ServiceInfo service) {
        if (service instanceof WPSInfo) {
            this.root = null;
        }
    }
}

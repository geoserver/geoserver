/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import static org.geoserver.security.impl.GeoServerRole.ADMIN_ROLE;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.geoserver.catalog.*;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.spring.security.GeoServerSession;
import org.springframework.security.core.GrantedAuthority;

public class AccessDataRuleInfoManager {

    private DataAccessRuleDAO dao;

    static List<AccessMode> MODES =
            Arrays.asList(AccessMode.READ, AccessMode.WRITE, AccessMode.ADMIN);

    public AccessDataRuleInfoManager() {
        this.dao = DataAccessRuleDAO.get();
    }

    public GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    public Set<String> getAvailableRoles() {
        try {
            return getSecurityManager()
                    .getRolesForAccessControl()
                    .stream()
                    .map(r -> r.getAuthority())
                    .collect(Collectors.toSet());
        } catch (IOException ioex) {
            return null;
        }
    }

    public String getWorkspaceName(CatalogInfo info) {
        if (info instanceof WorkspaceInfo) {
            return ((WorkspaceInfo) info).getName();
        } else if (info instanceof LayerInfo) {
            return ((LayerInfo) info).getResource().getStore().getWorkspace().getName();
        } else if (info instanceof LayerGroupInfo) {
            LayerGroupInfo group = (LayerGroupInfo) info;
            String wsName = group.getWorkspace() != null ? group.getWorkspace().getName() : null;
            return wsName;
        } else {
            return null;
        }
    }

    public String getLayerName(CatalogInfo info) {
        if (info instanceof PublishedInfo) {
            return ((PublishedInfo) info).getName();
        } else {
            return null;
        }
    }

    public List<DataAccessRule> getRules() {
        DataAccessRuleDAO dao = getSecurityManager().getDataAccessRuleDAO();
        return dao.getRules();
    }

    public Set<DataAccessRule> getWorkspaceDataAccessRules(String workspaceName) {
        return getRules()
                .stream()
                .filter(
                        r ->
                                r.getRoot().equalsIgnoreCase(workspaceName)
                                        && r.getLayer().equals("*"))
                .collect(Collectors.toSet());
    }

    public Set<DataAccessRule> getGlobalLayerGroupSecurityRule(String layerGroupName) {

        return getRules()
                .stream()
                .filter(r -> r.getRoot().equalsIgnoreCase(layerGroupName))
                .collect(Collectors.toSet());
    }

    public Set<DataAccessRule> getLayerSecurityRule(String workspaceName, String layerName) {

        return getRules()
                .stream()
                .filter(
                        r ->
                                r.getRoot().equalsIgnoreCase(workspaceName)
                                        && r.getLayer().equalsIgnoreCase(layerName))
                .collect(Collectors.toSet());
    }

    public Set<DataAccessRule> getResourceRule(String workspaceName, CatalogInfo info) {
        Set<DataAccessRule> rules = null;
        if (info instanceof LayerInfo) {
            rules = getLayerSecurityRule(workspaceName, ((PublishedInfo) info).getName());
        } else if (info instanceof LayerGroupInfo) {
            if (workspaceName == null)
                rules = getGlobalLayerGroupSecurityRule(((LayerGroupInfo) info).getName());
            else rules = getLayerSecurityRule(workspaceName, ((PublishedInfo) info).getName());
        } else if (info instanceof WorkspaceInfo) {
            rules = getWorkspaceDataAccessRules(((WorkspaceInfo) info).getName());
        }
        return rules;
    }

    /**
     * Convert a <code>List</code> of {@Link DataAccessRule} to a <code>Set</code>> of {@Link
     * DataAccessRuleInfo} suitable to be used as a model object by {@Link AccessDataRulePanel}
     */
    public List<DataAccessRuleInfo> mapTo(
            Set<DataAccessRule> rules, Set<String> authorities, String wsName, String layerName) {
        if (rules == null || rules.isEmpty()) {
            return getNewInfoList(wsName, layerName, authorities);
        }
        List<DataAccessRuleInfo> models = new ArrayList<>(authorities.size());
        Map<String, Set<AccessMode>> modeRoleMap = new HashMap<>(authorities.size());
        for (String auth : authorities) {
            Set<AccessMode> modes = new HashSet<>(3);
            for (AccessMode mode : MODES) {
                rules.stream()
                        .filter(r -> r.getAccessMode() == mode)
                        .forEach(
                                r -> {
                                    if (r.getRoles().contains(auth)) {
                                        modes.add(mode);
                                    }
                                });
            }
            modeRoleMap.put(auth, modes);
        }
        rules.forEach(r -> authorities.removeAll(r.getRoles()));
        authorities.forEach(r -> modeRoleMap.put(r, null));
        for (String key : modeRoleMap.keySet()) {
            Set<AccessMode> ams = modeRoleMap.get(key);
            DataAccessRuleInfo model = new DataAccessRuleInfo(key, wsName, layerName);
            model.setAdminFromMode(ams);
            model.setReadFromMode(ams);
            model.setWriteFromMode(ams);
            models.add(model);
        }
        return models;
    }

    public List<DataAccessRuleInfo> getNewInfoList(
            String wsName, String layerName, Set<String> availableRoles) {
        List<DataAccessRuleInfo> rules = new ArrayList<>();
        for (String role : availableRoles) {
            DataAccessRuleInfo rule = new DataAccessRuleInfo(role, wsName, layerName);
            rule.setRead(false);
            rule.setWrite(false);
            rule.setAdmin(false);
            rules.add(rule);
        }
        return rules;
    }

    /**
     * Convert a <code>List</code> of {@Link DataAccessRuleInfo} to a <code>Set</code>> of {@Link
     * DataAccessRule} suitable to be by {@Link DataAccessRuleDAO}
     */
    public Set<DataAccessRule> mapFrom(
            List<DataAccessRuleInfo> newRules,
            Set<String> authorities,
            String wsName,
            String layerName,
            boolean globalLayerGroup) {

        Set<DataAccessRule> rules = new HashSet<>(authorities.size());
        Map<AccessMode, Set<String>> modeRoleMap = new HashMap<>(MODES.size());
        for (AccessMode mode : MODES) {
            Set<String> selectedRoles = new HashSet<>();
            for (String auth : authorities) {
                newRules.stream()
                        .filter(role -> role.getRoleName().equalsIgnoreCase(auth))
                        .forEach(
                                rule -> {
                                    if (rule.hasMode(mode)) selectedRoles.add(auth);
                                });
            }
            modeRoleMap.put(mode, selectedRoles);
        }
        for (AccessMode key : modeRoleMap.keySet()) {
            Set<String> roles = modeRoleMap.get(key);
            if (roles != null && roles.size() > 0) {
                DataAccessRule rule = new DataAccessRule();
                if (!globalLayerGroup) {
                    rule.setRoot(wsName);
                    rule.setLayer(layerName != null ? layerName : "*");
                } else {
                    rule.setRoot(layerName);
                    rule.setLayer(null);
                    rule.setGlobalGroupRule(true);
                }
                rule.setAccessMode(key);
                rule.getRoles().addAll(roles);
                rules.add(rule);
            }
        }
        return rules;
    }

    public void saveRules(Set<DataAccessRule> old, Set<DataAccessRule> news) throws IOException {
        synchronized (this) {
            old.forEach(r -> dao.removeRule(r));
            if (!news.isEmpty()) {
                news.forEach(r -> dao.addRule(r));
            }
            dao.storeRules();
        }
    }

    public List<DataAccessRuleInfo> getDataAccessRuleInfo(CatalogInfo info) {
        String workspaceName = getWorkspaceName(info);
        String layerName = getLayerName(info);
        Set<String> authorities = getAvailableRoles();
        Set<DataAccessRule> rules = getResourceRule(workspaceName, info);
        return mapTo(rules, authorities, workspaceName, layerName);
    }

    public void removeAllResourceRules(String wsName, CatalogInfo info) throws IOException {
        getResourceRule(wsName, info).forEach(r -> dao.removeRule(r));
        dao.storeRules();
    }

    public static boolean canAccess() {
        boolean isAdmin = false;
        for (GrantedAuthority auth : GeoServerSession.get().getAuthentication().getAuthorities()) {
            if (auth.getAuthority().equalsIgnoreCase(ADMIN_ROLE.getAuthority())) {
                isAdmin = true;
                break;
            }
        }
        if (!GeoServerApplication.get()
                        .getBeanOfType(SecureCatalogImpl.class)
                        .isDefaultAccessManager()
                || !isAdmin) return false;
        else return true;
    }
}

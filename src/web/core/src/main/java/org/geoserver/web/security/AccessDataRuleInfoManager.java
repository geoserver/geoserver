/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.geoserver.catalog.*;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.GeoServerApplication;

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
        dao.reload();
        return dao.getRules();
    }

    public Set<DataAccessRule> getWorkspaceDataAccessRules(String workspaceName) {
        return getRules()
                .stream()
                .filter(r -> r.getRoot().equalsIgnoreCase(workspaceName))
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
        if (info instanceof PublishedInfo) {
            return getLayerSecurityRule(workspaceName, ((PublishedInfo) info).getName());
        } else if (info instanceof WorkspaceInfo) {
            return getWorkspaceDataAccessRules(((WorkspaceInfo) info).getName());
        } else {
            return null;
        }
    }

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
                if (mode == AccessMode.ADMIN && layerName != null) {
                    continue;
                } else {
                    rules.stream()
                            .filter(r -> r.getAccessMode() == mode)
                            .forEach(
                                    r -> {
                                        if (r.getRoles().contains(auth)) {
                                            modes.add(mode);
                                        }
                                    });
                }
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

    public Set<DataAccessRule> mapFrom(
            List<DataAccessRuleInfo> newRules,
            Set<String> authorities,
            String wsName,
            String layerName) {

        Set<DataAccessRule> rules = new HashSet<>(authorities.size());
        Map<AccessMode, Set<String>> modeRoleMap = new HashMap<>(MODES.size());
        for (AccessMode mode : MODES) {
            if (mode == AccessMode.ADMIN && layerName != null) {
                continue;
            } else {
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
        }
        for (AccessMode key : modeRoleMap.keySet()) {
            Set<String> roles = modeRoleMap.get(key);
            if (roles != null && roles.size() > 0) {
                DataAccessRule rule = new DataAccessRule();
                rule.setRoot(wsName);
                rule.setLayer(layerName != null ? layerName : "*");
                rule.setAccessMode(key);
                rule.getRoles().addAll(roles);
                rules.add(rule);
            }
        }
        return rules;
    }

    public void saveRules(Set<DataAccessRule> old, Set<DataAccessRule> news) throws IOException {
        old.forEach(r -> dao.removeRule(r));
        if (!news.isEmpty()) {
            news.forEach(r -> dao.addRule(r));
        }
        dao.storeRules();
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
}

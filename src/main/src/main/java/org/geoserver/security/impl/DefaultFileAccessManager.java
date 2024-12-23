/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.FileAccessManager;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.ResourceAccessManagerWrapper;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;

/**
 * Default implementation of the {@link FileAccessManager} interface, that uses the {@link DataAccessRuleDAO} to
 * determine the file system sandbox and the {@link SecureCatalogImpl} to determine the workspaces that can be accessed.
 * In case the GEOSERVER_FILESYSTEM_SANDBOX system property is set, the sandbox is imposed by the system administrator
 * and even the GeoServer administrator will be limited to it, otherwise it is imposed by GeoServer administrator
 * throught the security subsystem and the full administrator will be able to see the whole file system while workspace
 * administrators will be limited to their own workspace directories.
 */
public class DefaultFileAccessManager implements FileAccessManager {

    private static final Logger LOGGER = Logging.getLogger(DefaultFileAccessManager.class);

    public static String GEOSERVER_DATA_SANDBOX = "GEOSERVER_FILESYSTEM_SANDBOX";

    private final DataAccessRuleDAO dao;
    private final GeoServerSecurityManager securityManager;
    private final ResourceAccessManager resourceAccessManager;
    private final SecureCatalogImpl catalog;
    private String systemSandbox;

    public DefaultFileAccessManager(
            DataAccessRuleDAO dao, SecureCatalogImpl catalog, GeoServerSecurityManager securityManager) {
        this.dao = dao;
        this.systemSandbox = GeoServerExtensions.getProperty(GEOSERVER_DATA_SANDBOX);
        this.catalog = catalog;
        this.securityManager = securityManager;
        this.resourceAccessManager = catalog.getResourceAccessManager();
    }

    @Override
    public List<File> getAvailableRoots() {
        String sandboxPath = systemSandbox != null ? systemSandbox : dao.getFilesystemSandbox();
        if (sandboxPath == null) return null;

        // the full administrator is either locked into the sandbox, if that was set
        // by the OS sysadmin, or can see the whole file system
        Authentication auth = user();
        boolean fullAdmin = securityManager.checkAuthenticationForAdminRole(auth);
        if (fullAdmin) {
            if (systemSandbox != null) return List.of(new File(systemSandbox));
            else return null;
        }

        // it's a workspace admin then
        List<String> accessibleWorkspaces = new ArrayList<>();
        try (CloseableIterator<WorkspaceInfo> workspaces = catalog.list(WorkspaceInfo.class, Filter.INCLUDE)) {
            while (workspaces.hasNext()) {
                WorkspaceInfo ws = workspaces.next();
                WorkspaceAccessLimits accessLimits = resourceAccessManager.getAccessLimits(auth, ws);
                if (accessLimits != null && accessLimits.isAdminable()) {
                    accessibleWorkspaces.add(ws.getName());
                }
            }
        }

        // maps the workspace names to actual directories
        // and creates them if they are missing, as the rest of GeoServer needs file system
        // roots that are actually there
        List<File> roots = accessibleWorkspaces.stream()
                .map(ws -> new File(sandboxPath, ws))
                .collect(Collectors.toList());
        roots.forEach(File::mkdirs);
        return roots;
    }

    @Override
    public File getSandbox() {
        String sandboxPath = systemSandbox != null ? systemSandbox : dao.getFilesystemSandbox();
        if (sandboxPath == null) return null;
        return new File(sandboxPath);
    }

    @Override
    public boolean checkAccess(File file) {
        // Convert File to Path
        String sandboxPath = systemSandbox != null ? systemSandbox : dao.getFilesystemSandbox();
        LOGGER.log(Level.FINE, () -> "Filesystem sandbox: " + sandboxPath);
        if (sandboxPath == null) return true;
        Path sandbox = canonical(sandboxPath);
        Path path = canonical(file);

        // Check if the user is a full administrator
        Authentication auth = user();
        boolean fullAdmin = securityManager.checkAuthenticationForAdminRole();
        if (fullAdmin) {
            if (systemSandbox != null) {
                if (!path.startsWith(sandbox)) {
                    LOGGER.log(Level.FINE, () -> "Checked path " + path + " does not start with " + sandbox);
                    return false;
                }
            }
            return true;
        }

        // Check if the file is within the sandbox
        if (!path.startsWith(sandbox)) return false;

        // Check if the workspace is accessible
        String workspace = sandbox.relativize(path).getName(0).toString();
        WorkspaceInfo wi = catalog.getWorkspaceByName(workspace);
        if (wi == null) {
            LOGGER.log(Level.FINE, () -> "Sandbox check, workspace not authorized " + workspace);
            return false;
        }
        WorkspaceAccessLimits accessLimits = resourceAccessManager.getAccessLimits(auth, wi);
        LOGGER.log(Level.FINE, () -> "Sandbox auth check, workspace " + workspace + " access limits " + accessLimits);
        return accessLimits != null && accessLimits.isAdminable();
    }

    /** Forces reloading the DAO and the system sandbox definitions */
    public void reload() {
        this.systemSandbox = GeoServerExtensions.getProperty(GEOSERVER_DATA_SANDBOX);
        if (systemSandbox != null) LOGGER.log(Level.FINE, () -> "System sandbox property found: " + systemSandbox);
        dao.reload();
        ResourceAccessManager ram = this.resourceAccessManager;
        while (ram instanceof ResourceAccessManagerWrapper) {
            ram = ((ResourceAccessManagerWrapper) ram).unwrap();
        }
        if (ram instanceof DefaultResourceAccessManager) {
            ((DefaultResourceAccessManager) ram).reload();
        }
    }

    private static Path canonical(String fileName) {
        return Paths.get(fileName).toAbsolutePath().normalize();
    }

    private static Path canonical(File file) {
        return file.toPath().toAbsolutePath().normalize();
    }

    /**
     * Returns true if a system sandbox is imposed via system properties, false if the sanbox is defined in the security
     * subsystem instead.
     */
    public boolean isSystemSanboxEnabled() {
        return systemSandbox != null;
    }
}

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.io.Serializable;
import java.util.Set;
import org.geoserver.security.AccessMode;

public class DataAccessRuleInfo implements Serializable {

    private String roleName;

    private boolean read = false;

    private boolean write = false;

    private boolean admin = false;

    private String layerName;

    private String workspaceName;

    public DataAccessRuleInfo(String roleName, String workspaceName, String layerName) {
        this.roleName = roleName;
        this.workspaceName = workspaceName;
        this.layerName = layerName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setReadFromMode(Set<AccessMode> modes) {
        if (modes != null && modes.contains(AccessMode.READ)) {
            this.read = true;
        } else {
            this.read = false;
        }
    }

    public boolean isWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    public void setWriteFromMode(Set<AccessMode> modes) {
        if (modes != null && modes.contains(AccessMode.WRITE)) {
            this.write = true;
        } else {
            this.write = false;
        }
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setAdminFromMode(Set<AccessMode> modes) {
        if (modes != null && modes.contains(AccessMode.ADMIN)) {
            this.admin = true;
        } else {
            this.admin = false;
        }
    }

    public boolean hasMode(AccessMode mode) {
        if (mode == AccessMode.READ && isRead()) return true;
        else if (mode == AccessMode.WRITE && isWrite()) return true;
        else if (mode == AccessMode.ADMIN && isAdmin()) return true;
        else return false;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
}

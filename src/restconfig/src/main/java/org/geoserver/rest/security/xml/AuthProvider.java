/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.geoserver.security.config.SecurityAuthProviderConfig;

@XStreamAlias("authProvider")
public class AuthProvider {
    private String id;
    private String name;
    private String className;
    private String userGroupServiceName;
    private int position;
    private SecurityAuthProviderConfig config;

    // Available & Selected, disabled when available but not selected.
    private boolean disabled;

    public AuthProvider() {}

    public AuthProvider(SecurityAuthProviderConfig provider) {
        this.id = provider.getId();
        this.name = provider.getName();
        this.className = provider.getClassName();
        this.userGroupServiceName = "default";
        this.config = provider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    public SecurityAuthProviderConfig getConfig() {
        return config;
    }

    public void setConfig(SecurityAuthProviderConfig config) {
        this.config = config;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}

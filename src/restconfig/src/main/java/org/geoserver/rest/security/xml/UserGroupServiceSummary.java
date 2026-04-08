/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;

/**
 * Lightweight summary for listing. Extend with more fields as you see fit (e.g., implementation class, readOnly flag,
 * etc.).
 */
@XStreamAlias("userGroupService")
public class UserGroupServiceSummary {
    private String name;
    private String cls;

    public UserGroupServiceSummary() {}

    public UserGroupServiceSummary(String name, String cls) {
        this.name = name;
        this.cls = cls;
    }

    public static UserGroupServiceSummary from(SecurityUserGroupServiceConfig cfg) {
        String clazz = cfg != null ? cfg.getClass().getSimpleName() : "unknown";
        assert cfg != null;
        return new UserGroupServiceSummary(cfg.getName(), clazz);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCls() {
        return cls;
    }

    public void setCls(String cls) {
        this.cls = cls;
    }
}

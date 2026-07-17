/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.Objects;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.springframework.util.ClassUtils;

/**
 * Lightweight summary for listing. Extend with more fields as you see fit (e.g., implementation class, readOnly flag,
 * etc.).
 */
@XStreamAlias("roleService")
public class RoleServiceSummary {
    private String name;
    private String cls;

    public RoleServiceSummary() {}

    public RoleServiceSummary(String name, String cls) {
        this.name = name;
        this.cls = cls;
    }

    public static RoleServiceSummary from(SecurityRoleServiceConfig cfg) {
        Objects.requireNonNull(cfg, "cfg");
        return new RoleServiceSummary(cfg.getName(), ClassUtils.getShortName(cfg.getClassName()));
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

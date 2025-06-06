/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.geoserver.security.config.SecurityFilterConfig;

@XStreamAlias("authFilter")
public class AuthFilter {
    private String id;
    private String className;
    private String name;

    @XStreamAlias("config")
    private SecurityFilterConfig config;

    public AuthFilter() {}

    public AuthFilter(SecurityFilterConfig securityFilterConfig) {

        this.name = securityFilterConfig.getName();
        this.className = securityFilterConfig.getClass().getName();
        this.config = securityFilterConfig;
        this.id = securityFilterConfig.getId();
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

    public SecurityFilterConfig getConfig() {
        config.setId(id);
        config.setName(name);
        return config;
    }

    public void setConfig(SecurityFilterConfig config) {
        this.config = config;
    }
}

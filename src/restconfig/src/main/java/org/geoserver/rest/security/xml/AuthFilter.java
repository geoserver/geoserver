package org.geoserver.rest.security.xml;

import org.geoserver.security.config.SecurityFilterConfig;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public class AuthFilter {
    private String id;
    private String name;
    private String className;
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

    @XmlElement
    public SecurityFilterConfig getConfig() {
        return config;
    }

    public void setConfig(SecurityFilterConfig config) {
        this.config = config;
    }
}

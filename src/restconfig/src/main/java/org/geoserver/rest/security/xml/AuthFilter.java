/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.geoserver.security.config.SecurityFilterConfig;

@XmlRootElement
@JsonRootName(value = "authFilter")
public class AuthFilter {
    @XmlTransient
    private String id;

    @XmlTransient
    private String name;

    private SecurityFilterConfig config;

    public AuthFilter() {}

    public AuthFilter(SecurityFilterConfig securityFilterConfig) {
        this.id = securityFilterConfig.getId();
        this.name = securityFilterConfig.getName();
        this.config = securityFilterConfig;
    }

    @XmlElement(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

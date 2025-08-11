/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.security.config.SecurityFilterConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
@XStreamAlias("authFilter")
@XmlRootElement(name = "AuthFilter")
@XmlAccessorType(XmlAccessType.FIELD)
public class AuthFilter {
    private String name;

    @XStreamAlias("config")
    private SecurityFilterConfig config;

    public AuthFilter() {}

    public AuthFilter(SecurityFilterConfig securityFilterConfig) {
        this.name = securityFilterConfig.getName();
        this.config = securityFilterConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SecurityFilterConfig getConfig() {
        config.setName(name);
        return config;
    }

    public void setConfig(SecurityFilterConfig config) {
        this.config = config;
    }
}

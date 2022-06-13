/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.geoserver.metadata.data.dto.NamespaceConfiguration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NamespaceConfigurationImpl implements NamespaceConfiguration {

    private String prefix;

    private String uri;

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getURI() {
        return uri;
    }
}

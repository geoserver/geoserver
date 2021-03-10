/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain;

/**
 * DomainModel configuration for Smart AppSchema. It keeps root entity and detailed information
 * about selected user attributes and relations that will be used for mappings to output formats.
 *
 * @author Jose Macchi - Geosolutions
 */
public final class DomainModelConfig {

    private String rootEntityName;

    public String getRootEntityName() {
        return rootEntityName;
    }

    public void setRootEntityName(String rootEntityName) {
        this.rootEntityName = rootEntityName;
    }
}

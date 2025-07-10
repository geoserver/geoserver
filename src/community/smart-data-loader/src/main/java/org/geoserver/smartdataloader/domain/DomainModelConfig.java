/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * DomainModel configuration for Smart AppSchema. It keeps root entity and detailed information about selected user
 * attributes and relations that will be used for mappings to output formats.
 *
 * @author Jose Macchi - Geosolutions
 */
public final class DomainModelConfig {

    private String rootEntityName;
    private Map<String, String> overrideExpressions = new HashMap<>();
    private String entitiesPrefix;

    public String getRootEntityName() {
        return rootEntityName;
    }

    public void setRootEntityName(String rootEntityName) {
        this.rootEntityName = rootEntityName;
    }

    public Map<String, String> getOverrideExpressions() {
        return overrideExpressions;
    }

    public void setOverrideExpressions(Map<String, String> overrideExpressions) {
        this.overrideExpressions = overrideExpressions;
    }

    public String getEntitiesPrefix() {
        return entitiesPrefix;
    }

    public void setEntitiesPrefix(String entitiesPrefix) {
        this.entitiesPrefix = entitiesPrefix;
    }

    @Override
    public String toString() {
        return "DomainModelConfig{" + "rootEntityName='"
                + rootEntityName + '\'' + ", overrideExpressions="
                + overrideExpressions + ", entitiesPrefix='"
                + entitiesPrefix + '\'' + '}';
    }
}

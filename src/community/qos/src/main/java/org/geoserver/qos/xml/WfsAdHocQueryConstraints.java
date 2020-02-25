/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.List;

/**
 * qos-wfs:AdHocQueryConstraints data model
 *
 * @author Fernando Miño, Geosolutions
 */
public class WfsAdHocQueryConstraints extends LimitedAreaRequestConstraints {

    /*<qos-wfs:TypeNames>ad:Address</qos-wfs:TypeNames> */
    private List<String> typeNames;
    private Integer count;
    private String resolveReferences;
    private String sortBy;
    private String propertyName;

    public WfsAdHocQueryConstraints() {
        super();
    }

    public List<String> getTypeNames() {
        return typeNames;
    }

    public void setTypeNames(List<String> typeNames) {
        this.typeNames = typeNames;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getResolveReferences() {
        return resolveReferences;
    }

    public void setResolveReferences(String resolveReferences) {
        this.resolveReferences = resolveReferences;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
}

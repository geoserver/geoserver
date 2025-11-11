/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Base implementation for EO access limit configuration, with elements shared amongst Collections and Products. */
public abstract class EOAccessLimitsInfoImpl implements EOAccessLimitInfo {

    protected String cqlFilter;
    protected List<String> roles = new ArrayList<>();

    protected EOAccessLimitsInfoImpl() {}

    protected EOAccessLimitsInfoImpl(String cqlFilter, List<String> roles) {
        this.cqlFilter = cqlFilter;
        this.roles = roles == null ? null : new ArrayList<>(roles);
    }

    @Override
    public String getCQLFilter() {
        return cqlFilter;
    }

    @Override
    public void setCQLFilter(String cqlFilter) {
        this.cqlFilter = cqlFilter;
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Sets the roles allowed to access the collections/products matched by the CQL filter. To be used when
     * deserializing only.
     */
    public void setRoles(List<String> roles) {
        this.roles = roles == null ? null : new ArrayList<>(roles);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EOAccessLimitsInfoImpl that = (EOAccessLimitsInfoImpl) o;
        return Objects.equals(cqlFilter, that.cqlFilter) && Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cqlFilter, roles);
    }
}

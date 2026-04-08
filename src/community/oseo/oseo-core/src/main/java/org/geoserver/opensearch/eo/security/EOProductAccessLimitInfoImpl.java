/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.security;

import java.util.List;
import java.util.Objects;

/** Implementation of EOProductAccessLimitInfo */
public class EOProductAccessLimitInfoImpl extends EOAccessLimitsInfoImpl implements EOProductAccessLimitInfo {

    private String collection;

    public EOProductAccessLimitInfoImpl() {}

    public EOProductAccessLimitInfoImpl(String collection, String cqlFilter, List<String> roles) {
        super(cqlFilter, roles);
        this.collection = collection;
    }

    @Override
    public String getCollection() {
        return collection;
    }

    @Override
    public void setCollection(String collection) {
        this.collection = collection;
    }

    @Override
    public String toString() {
        return "EOProductAccessLimitInfoImpl{" + "collection='"
                + collection + '\'' + ", cqlFilter='"
                + cqlFilter + '\'' + ", roles="
                + roles + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EOProductAccessLimitInfoImpl that = (EOProductAccessLimitInfoImpl) o;
        return Objects.equals(collection, that.collection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collection);
    }
}

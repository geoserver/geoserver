/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.security;

import java.util.List;

/** Implementation of EOCollectionAccessLimitInfo */
public class EOCollectionAccessLimitInfoImpl extends EOAccessLimitsInfoImpl implements EOCollectionAccessLimitInfo {

    public EOCollectionAccessLimitInfoImpl() {}

    public EOCollectionAccessLimitInfoImpl(String cqlFilter, List<String> roles) {
        super(cqlFilter, roles);
    }

    @Override
    public String toString() {
        return "EOCollectionAccessLimitInfoImpl{" + "cqlFilter='" + cqlFilter + '\'' + ", roles=" + roles + '}';
    }
}

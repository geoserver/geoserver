/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.filter.sort.SortBy;

/**
 * Decorates a given query capabilities, subclasses should override methods they inded to change
 *
 * @author Andrea Aime - GeoSolutions
 */
abstract class QueryCapabilitiesDecorator extends QueryCapabilities {
    QueryCapabilities delegate;

    public QueryCapabilitiesDecorator(QueryCapabilities delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public boolean isOffsetSupported() {
        return delegate.isOffsetSupported();
    }

    @Override
    public boolean supportsSorting(SortBy[] sortAttributes) {
        return delegate.supportsSorting(sortAttributes);
    }

    @Override
    public boolean isReliableFIDSupported() {
        return delegate.isReliableFIDSupported();
    }

    @Override
    public boolean isUseProvidedFIDSupported() {
        return delegate.isUseProvidedFIDSupported();
    }

    @Override
    public boolean isJoiningSupported() {
        return delegate.isJoiningSupported();
    }

    @Override
    public boolean isVersionSupported() {
        return delegate.isVersionSupported();
    }
}

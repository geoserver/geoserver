/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import org.geotools.data.QueryCapabilities;
import org.opengis.filter.sort.SortBy;

/**
 * Decorates a given query capabilities, subclasses should override
 * methods they inded to change
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
abstract class QueryCapabilitiesDecorator extends QueryCapabilities {
    QueryCapabilities delegate;
    
    public QueryCapabilitiesDecorator(QueryCapabilities delegate) {
        super();
        this.delegate = delegate;
    }

    public boolean isOffsetSupported() {
        return delegate.isOffsetSupported();
    }

    public boolean supportsSorting(SortBy[] sortAttributes) {
        return delegate.supportsSorting(sortAttributes);
    }

    public boolean isReliableFIDSupported() {
        return delegate.isReliableFIDSupported();
    }

    public boolean isUseProvidedFIDSupported() {
        return delegate.isUseProvidedFIDSupported();
    }

    public boolean isJoiningSupported() {
        return delegate.isJoiningSupported();
    }

    public boolean isVersionSupported() {
        return delegate.isVersionSupported();
    }
    
}

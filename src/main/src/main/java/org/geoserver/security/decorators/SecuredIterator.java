/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.Iterator;
import org.geoserver.catalog.impl.AbstractDecorator;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.WrapperPolicy;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;

/**
 * Applies the write policy to removals
 *
 * @author Andrea Aime - GeoSolutions
 * @deprecated This class is not longer used, {@link SecuredFeatureIterator} and {@link
 *     SecuredSimpleFeatureIterator} are used instead.
 */
public class SecuredIterator extends AbstractDecorator<Iterator>
        implements Iterator, FeatureIterator {
    WrapperPolicy policy;
    Object current;
    Filter writeFilter;

    public SecuredIterator(Iterator wrapped, WrapperPolicy policy) {
        super(wrapped);
        this.policy = policy;
        writeFilter = SecurityUtils.getWriteQuery(policy).getFilter();
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public Feature next() {
        this.current = delegate.next();
        return (Feature) current;
    }

    public void remove() {
        if (writeFilter.evaluate(current)) {
            delegate.remove();
        } else {
            throw unsupportedOperation();
        }
    }

    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link
     * UnsupportedOperationException} in case we have to conceal the fact the data is actually
     * writable, using an Spring security exception otherwise to force an authentication from the
     * user
     */
    RuntimeException unsupportedOperation() {
        if (policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess();
        } else return new UnsupportedOperationException("This iterator is read only");
    }

    public void close() {
        if (delegate instanceof FeatureIterator) {
            ((FeatureIterator) delegate).close();
        }
    }
}

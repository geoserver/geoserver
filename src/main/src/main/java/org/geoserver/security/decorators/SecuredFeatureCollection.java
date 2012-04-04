/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.geoserver.security.SecurityUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Wrapper;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Secures a feature collection according to the given policy. The implementation assumes
 * all of the attributes that should not be read have been shaved off already, and similarly,
 * that the read filters have been applied already in the delegate, and adds control over writes
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 * @param <T>
 * @param <F>
 */
public class SecuredFeatureCollection<T extends FeatureType, F extends Feature> extends
        DecoratingFeatureCollection<T, F> {
    static final Logger LOGGER = Logging.getLogger(SecuredFeatureCollection.class);
    
    WrapperPolicy policy;

    SecuredFeatureCollection(FeatureCollection<T, F> delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    public Iterator iterator() {
        return (Iterator) SecuredObjects.secure(delegate.iterator(), policy);
    }
    
    @Override
    public org.geotools.feature.FeatureIterator<F> features() {
        return (FeatureIterator) SecuredObjects.secure(delegate.features(), policy);
    }

    public FeatureCollection<T, F> sort(SortBy order) {
        // attributes should have been shaved already
        final FeatureCollection<T, F> fc = delegate.sort(order);
        if(fc == null)
            return null;
        else
            return (FeatureCollection) SecuredObjects.secure(fc, policy);
    }

    public FeatureCollection<T, F> subCollection(Filter filter) {
        final FeatureCollection<T, F> fc = delegate.subCollection(filter);
        if(fc == null)
            return null;
        else
            return (FeatureCollection) SecuredObjects.secure(fc, policy);
    }
    
    @Override
    public void close(FeatureIterator<F> close) {
        if(close instanceof Wrapper && ((Wrapper) close).isWrapperFor(FeatureIterator.class))
            delegate.close(((Wrapper) close).unwrap(FeatureIterator.class));
        else
            delegate.close(close);
    }
    
    @Override
    public void close(Iterator<F> close) {
        if(close instanceof Wrapper && ((Wrapper) close).isWrapperFor(Iterator.class))
            delegate.close(((Wrapper) close).unwrap(Iterator.class));
        else
            delegate.close(close);
    }

    // ---------------------------------------------------------------------
    // Write related methods
    // ---------------------------------------------------------------------

    public boolean add(F o) {
        Query writeQuery = getWriteQuery(policy);
        final Filter filter = writeQuery.getFilter();
        if(filter == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else {
            if(filter.evaluate(o)) {
                if(writeQuery.getPropertyNames() == Query.ALL_NAMES) {
                    return delegate.add(o);
                } else {
                    // TODO: shave off attributes we cannot write
                    LOGGER.log(Level.SEVERE, "Unfinished implementation, we need to shave off " +
                    "the attributes one cannot write!");
                    return add(o);
                }             
            } else {
                return false;
            }
        }
    }

    public boolean addAll(Collection c) {
        Query writeQuery = getWriteQuery(policy);
        final Filter filter = writeQuery.getFilter();
        if(filter == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else {
            List filtered = filterCollection(c, writeQuery);
            
            return addAll(filtered);
        }
    }

    /**
     * Filters out all features that cannot be modified/removed
     * @param collection
     * @param writeQuery
     * @return
     */
    List filterCollection(Collection collection, Query writeQuery) {
        // warn about inability to shave off complex features
        if(writeQuery.getPropertyNames() != Query.ALL_NAMES) {
            LOGGER.log(Level.SEVERE, "Unfinished implementation, we need to shave off " +
            "the attributes one cannot write!");
        }
        
        // filter out anything we cannot write
        final Filter filter = writeQuery.getFilter();
        List filtered = new ArrayList();
        for (Object feature : collection) {
            if(filter.evaluate(feature)) {
                filtered.add(feature);
            }
        }
        return filtered;
    }

    public void clear() {
        Query writeQuery = getWriteQuery(policy);
        final Filter filter = writeQuery.getFilter();
        if(filter == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else {
            delegate.clear();
        }
    }

    public boolean remove(Object o) {
        Query writeQuery = getWriteQuery(policy);
        final Filter filter = writeQuery.getFilter();
        if(filter == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else {
            if(filter.evaluate(o)) {
                return delegate.remove(o);
            } else {
                return false;
            }
        }
    }

    public boolean removeAll(Collection c) {
        Query writeQuery = getWriteQuery(policy);
        final Filter filter = writeQuery.getFilter();
        if(filter == Filter.EXCLUDE) {
            throw unsupportedOperation();
        } else {
            List filtered = filterCollection(c, writeQuery);
            
            return removeAll(filtered);
        }
    }

    public boolean retainAll(Collection c) {
        // way too inefficient and fancy to implement, besides nothing in GS uses it and
        // even ContentFeatureCollection does not implement it, so just let it go, we'll
        // cross this bridge when necessary
        throw new UnsupportedOperationException("Sorry, not even ContentFeatureCollection implements this one");
    }

    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link UnsupportedOperationException}
     * in case we have to conceal the fact the data is actually writable, using an Spring security exception otherwise
     * to force an authentication from the user
     */
    RuntimeException unsupportedOperation() {
        String typeName = getID();
        if(policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess(typeName);
        } else
            return new UnsupportedOperationException("Feature type " + typeName + " is read only");
    }
}

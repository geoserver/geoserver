/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.geoserver.security.SecurityUtils.getWriteQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

/**
 * A feature store wrapper enforcing the access policy contained in the WrapperPolicy.
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 * @param <F>
 */
public class SecuredFeatureStore<T extends FeatureType, F extends Feature>
        extends SecuredFeatureSource<T, F> implements FeatureStore<T, F> {

    Transaction transaction;

    FeatureStore<T, F> storeDelegate;

    public SecuredFeatureStore(FeatureStore delegate, WrapperPolicy policy) {
        super(delegate, policy);
        this.storeDelegate = delegate;
    }

    public List<FeatureId> addFeatures(FeatureCollection<T, F> collection) throws IOException {
        Query writeQuery = getWriteQuery(policy);

        if (writeQuery.getFilter() == Filter.EXCLUDE
                || writeQuery.getPropertyNames() == Query.NO_NAMES) {
            throw unsupportedOperation();
        } else if (writeQuery == Query.ALL) {
            // make sure it behaves like native when no write limits are imposed (even if this
            // case makes no sense, we should not wrap at all)
            return storeDelegate.addFeatures(collection);
        } else {
            // check if any of the inserted features does not pass the write filters
            if (writeQuery.getFilter() != null && writeQuery.getFilter() != Filter.INCLUDE) {
                final FilteringFeatureCollection<T, F> filtered =
                        new FilteringFeatureCollection<T, F>(collection, writeQuery.getFilter());
                if (filtered.size() < collection.size()) {
                    String typeName = getSchema().getName().getLocalPart();
                    if (policy.response == Response.CHALLENGE) {
                        throw SecureCatalogImpl.unauthorizedAccess(typeName);
                    } else {
                        throw new UnsupportedOperationException(
                                "At least one of the features inserted does not satisfy your write restrictions");
                    }
                }
            }

            // deal with writable properties
            if (writeQuery.getPropertyNames() == Query.ALL_NAMES) {
                return storeDelegate.addFeatures(collection);
            } else {
                if (collection.getSchema() instanceof SimpleFeatureType
                        && storeDelegate instanceof SimpleFeatureStore) {
                    // see if the user specified the value of any attribute she cannot write
                    final SimpleFeatureCollection simpleCollection =
                            (SimpleFeatureCollection) collection;

                    // wrap it with a collection that will check if any non writable attribute has
                    // been given a value
                    List<String> writableAttributes = Arrays.asList(writeQuery.getPropertyNames());
                    CheckAttributesFeatureCollection checker =
                            new CheckAttributesFeatureCollection(
                                    simpleCollection, writableAttributes, policy.getResponse());
                    return ((SimpleFeatureStore) storeDelegate).addFeatures(checker);
                } else {
                    // TODO: add retyping to shave off attributes we cannot write
                    LOGGER.log(
                            Level.SEVERE,
                            "Unfinished implementation, we need to shave off "
                                    + "the attributes one cannot write off complex features. "
                                    + "However at this time there is no writable complex feature!");
                    return storeDelegate.addFeatures(collection);
                }
            }
        }
    }

    public void modifyFeatures(AttributeDescriptor[] types, Object[] values, Filter filter)
            throws IOException {
        Name[] names = new Name[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getName();
        }
        modifyFeatures(names, values, filter);
    }

    public void modifyFeatures(Name[] names, Object[] values, Filter filter) throws IOException {
        // are we limiting anything?
        Query writeQuery = getWriteQuery(policy);
        if (writeQuery == Query.ALL) {
            storeDelegate.modifyFeatures(names, values, filter);
            return;
        } else if (writeQuery.getFilter() == Filter.EXCLUDE
                || writeQuery.getPropertyNames() == Query.NO_NAMES) {
            throw unsupportedOperation();
        }

        // get the mixed filter
        final Query local = new Query(null, filter);
        Query mixed = mixQueries(local, writeQuery);

        if (writeQuery.getPropertyNames() == Query.ALL_NAMES) {
            // it was just a matter of filtering.
            storeDelegate.modifyFeatures(names, values, mixed.getFilter());
        } else {
            // get the writable attribute set
            Set<String> queryNames =
                    new HashSet<String>(Arrays.asList(writeQuery.getPropertyNames()));

            // check the update fields
            for (int i = 0; i < names.length; i++) {
                final String localName = names[i].getLocalPart();
                if (queryNames.contains(localName)) {
                    String typeName = getSchema().getName().getLocalPart();
                    if (policy.getResponse() == Response.CHALLENGE) {
                        throw SecureCatalogImpl.unauthorizedAccess(typeName);
                    } else {
                        throw new UnsupportedOperationException(
                                "Trying to write on the write protected attribute " + names[i]);
                    }
                }
            }

            storeDelegate.modifyFeatures(names, values, mixed.getFilter());
        }
    }

    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter)
            throws IOException {
        modifyFeatures(new Name[] {attributeName}, new Object[] {attributeValue}, filter);
    }

    public void removeFeatures(Filter filter) throws IOException {
        // are we limiting anything?
        Query writeQuery = getWriteQuery(policy);
        if (writeQuery == Query.ALL) {
            storeDelegate.removeFeatures(filter);
        } else if (writeQuery.getFilter() == Filter.EXCLUDE
                || writeQuery.getPropertyNames() == Query.NO_NAMES) {
            throw unsupportedOperation();
        }

        // get the mixed filter
        final Query local = new Query(null, filter);
        Query mixed = mixQueries(local, writeQuery);
        storeDelegate.removeFeatures(mixed.getFilter());
    }

    public void setFeatures(FeatureReader<T, F> reader) throws IOException {
        throw new UnsupportedOperationException(
                "Unaware of any GS api using this, " + "so it has not been implemented");
    }

    public Transaction getTransaction() {
        return storeDelegate.getTransaction();
    }

    public void setTransaction(Transaction transaction) {
        storeDelegate.setTransaction(transaction);
    }

    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link
     * UnsupportedOperationException} in case we have to conceal the fact the data is actually
     * writable, using an Spring security exception otherwise to force an authentication from the
     * user
     */
    protected RuntimeException unsupportedOperation() {
        String typeName = getSchema().getName().getLocalPart();
        if (policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess(typeName);
        } else {
            return new UnsupportedOperationException(typeName + " is read only");
        }
    }
}

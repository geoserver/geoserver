/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.factory.Hints;

/**
 * Interfaces to a storage for CSW record objects. By default it has to provide support for CSW Dublin Core records (in
 * their {@link CSWRecordDescriptor#RECORD_TYPE} form, but can publish more feature types as well (e.g., ISO or ebRIM
 * records)
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface CatalogStore {

    /**
     * This query hint specifies that the query has not yet been prepared (adapted and verified). It is necessary when
     * we use multiple queryables mappings so that the query must be prepared separately for each mapping; but for
     * backwards compatibility of the API this flag was created.
     */
    public static final Hints.Key KEY_UNPREPARED = new Hints.Key(Boolean.class);

    /** Returns the supported record types */
    RecordDescriptor[] getRecordDescriptors() throws IOException;

    /** Queries a specific record type using the GeoTools Query object (which contains type name, attribute selection */
    FeatureCollection<FeatureType, Feature> getRecords(Query q, Transaction t, RecordDescriptor outputRd)
            throws IOException;

    /**
     * Returns the number of records that {@link #getRecords(Query, Transaction, String)} would return given the same
     * query and transaction
     */
    int getRecordsCount(Query q, Transaction t, RecordDescriptor outputRd) throws IOException;

    /**
     * Returns the domain of an attribute in the specified record type.
     *
     * @param typeName The record type
     * @param attributeName The attribute
     * @return An iteration of possible values, or null if domain extraction for this attribute is not supported
     * @see {@link CatalogStoreCapabilities#getDomainQueriables(Name)} to get a list of the properties which the store
     *     supports the domain extraction from
     */
    CloseableIterator<String> getDomain(Name typeName, Name attributeName) throws IOException;

    /**
     * Adds a new record to the store. This method might not be supported, see
     * {@link CatalogStoreCapabilities#supportsTransactions()} to check if the store supports transactions
     */
    List<FeatureId> addRecord(Feature f, Transaction t) throws IOException;

    /**
     * Removes records from the store. This method might not be supported, see
     * {@link CatalogStoreCapabilities#supportsTransactions()} to check if the store supports transactions
     */
    void deleteRecord(Filter f, Transaction t) throws IOException;

    /**
     * Updates records in the store. This method might not be supported, see
     * {@link CatalogStoreCapabilities#supportsTransactions()} to check if the store supports transactions
     */
    void updateRecord(Name typeName, Name[] attributeNames, Object[] attributeValues, Filter filter, Transaction t)
            throws IOException;

    /**
     * Returns the repository item for the specified record id, or null if the repository item is not found, or the
     * operation is not supported
     */
    RepositoryItem getRepositoryItem(String recordId) throws IOException;

    /** Returns the store capabilities */
    CatalogStoreCapabilities getCapabilities();

    /**
     * Maps a qualified name to it's equivalent property names for the backend store. @Deprecated Use
     * translateToPropertyNames(RecordDescriptor, Name)
     */
    @Deprecated
    default PropertyName translateProperty(RecordDescriptor rd, Name name) {
        List<PropertyName> propNames = translateToPropertyNames(rd, name);
        return propNames != null && !propNames.isEmpty() ? propNames.get(0) : null;
    }

    /** Maps a qualified name to its equivalent property names for the backend store. */
    List<PropertyName> translateToPropertyNames(RecordDescriptor rd, Name name);
}

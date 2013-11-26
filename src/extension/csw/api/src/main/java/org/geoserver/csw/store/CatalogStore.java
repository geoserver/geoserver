/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;

/**
 * Interfaces to a storage for CSW record objects. By default it has to provide support for CSW
 * Dublin Core records (in their {@link CSWRecordDescriptor#RECORD} form, but can publish more
 * feature types as well (e.g., ISO or ebRIM records)
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public interface CatalogStore {

    /**
     * Returns the supported record types
     */
    RecordDescriptor[] getRecordDescriptors() throws IOException;

    /**
     * Queries a specific record type using the GeoTools Query object (which contains type name,
     * attribute selection
     */
    FeatureCollection getRecords(Query q, Transaction t, String outputSchema) throws IOException;

    /**
     * Returns the number of records that {@link #getRecords(Query, Transaction)} would return given
     * the same query and transaction
     * 
     * @param q
     * @param t
     * @return
     * @throws IOException
     */
    int getRecordsCount(Query q, Transaction t) throws IOException;

    /**
     * Returns the domain of an attribute in the specified record type.
     * 
     * @param typeName The record type
     * @param attributeName The attribute
     * @return An iteration of possible values, or null if domain extraction for this attribute is
     *         not supported
     * 
     * @throws IOException
     * @see {@link CatalogStoreCapabilities#getDomainQueriables(Name)} to get a list of the properties
     *      which the store supports the domain extraction from
     */
    CloseableIterator<String> getDomain(Name typeName, Name attributeName) throws IOException;

    /**
     * Adds a new record to the store. This method might not be supported, see
     * {@link CatalogStoreCapabilities#supportsTransactions()} to check if the store supports
     * transactions
     * 
     * @param f
     * @param t
     * @return
     * @throws IOException
     */
    List<FeatureId> addRecord(Feature f, Transaction t) throws IOException;

    /**
     * Removes records from the store. This method might not be supported, see
     * {@link CatalogStoreCapabilities#supportsTransactions()} to check if the store supports
     * transactions
     * 
     * @param f
     * @param t
     * @throws IOException
     */
    void deleteRecord(Filter f, Transaction t) throws IOException;

    /**
     * Updates records in the store. This method might not be supported, see
     * {@link CatalogStoreCapabilities#supportsTransactions()} to check if the store supports
     * transactions
     * 
     * @param typeName
     * @param attributeNames
     * @param attributeValues
     * @param filter
     * @param t
     * @throws IOException
     */
    void updateRecord(Name typeName, Name[] attributeNames, Object[] attributeValues,
            Filter filter, Transaction t) throws IOException;
    
    /**
     * Returns the repository item for the specified record id, or null
     * if the repository item is not found, or the operation is not supported 
     * 
     * @param recordId
     * @return
     */
    RepositoryItem getRepositoryItem(String recordId) throws IOException;

    /**
     * Returns the store capabilities
     * 
     * @return
     */
    CatalogStoreCapabilities getCapabilities();
    
    
    /**
     * Maps a qualified name to it's equivalent property name for the backend store.
     *
     */
    PropertyName translateProperty(RecordDescriptor rd, Name name) ;

}

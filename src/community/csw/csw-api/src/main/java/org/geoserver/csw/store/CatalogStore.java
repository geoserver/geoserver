/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

public interface CatalogStore {

    /**
     * Returns the supported record types
     */
    FeatureType[] getRecordSchemas() throws IOException;

    /**
     * Queries a specific record type using the GeoTools Query object (which contains type name,
     * attribute selection
     */
    FeatureCollection getRecords(Query q, Transaction t) throws IOException;

    CloseableIterator<String> getDomain(Name typeName, Name attributeName) throws IOException;

    List<FeatureId> addRecord(Feature f, Transaction t) throws IOException;

    void deleteRecord(Filter f, Transaction t) throws IOException;

    void updateRecord(Name typeName, Name[] attributeNames, Object[] attributeValues,
            Filter filter, Transaction t) throws IOException;

    CatalogCapabilities getCapabilities();

}

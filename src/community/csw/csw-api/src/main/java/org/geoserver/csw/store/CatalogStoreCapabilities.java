/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.csw.records.CSWRecordDescriptor;
import org.opengis.feature.type.Name;

/**
 * Represents the capabilities of a {@link CatalogStore}
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CatalogStoreCapabilities {

    /**
     * True if the store supports transactions (insert, update, delete), false otherwise
     */
    public boolean supportsTransactions() {
        return false;
    }

    /**
     * Returns the list of queriable properties supported by this implementation for the given type
     * name (empty by default)
     * 
     * @param typeName
     * @return
     */
    public List<Name> getQueriables(Name typeName) {
        return Collections.emptyList();
    }
    
    /**
     * Returns the list of queriable properties for which an enumeration of the domain makes sense
     * 
     * @param typeName
     * @return
     */
    public List<Name> getDomainQueriables(Name typeName) {
        List<Name> queriables = new ArrayList<Name>();
        
        for(Name property : CSWRecordDescriptor.SUMMARY_ELEMENTS)
        {
            if (property.equals(typeName))
            {
                queriables.add(property);
            }
        }
        
        return queriables;
    }
    
    /**
     * Returns true if GetRepositoryItem is supported on the specified type
     * @param typeName
     * @return
     */
    public boolean supportsGetRepositoryItem(Name typeName) {
        return false;
    }
}

/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store;

import java.util.List;
import java.util.Map;
import org.geoserver.csw.records.RecordDescriptor;
import org.opengis.feature.type.Name;

/**
 * Represents the capabilities of a {@link CatalogStore}
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CatalogStoreCapabilities {
    
    protected Map<Name, RecordDescriptor> descriptors;
    
    public CatalogStoreCapabilities (Map<Name, RecordDescriptor> descriptors) {
        this.descriptors = descriptors;
    }

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
        return descriptors.get(typeName).getQueryables();
    }
    
    /**
     * Returns the list of queriable properties for which an enumeration of the domain makes sense
     * 
     * @param typeName
     * @return
     */
    public List<Name> getDomainQueriables(Name typeName) {        
        return descriptors.get(typeName).getQueryables();
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

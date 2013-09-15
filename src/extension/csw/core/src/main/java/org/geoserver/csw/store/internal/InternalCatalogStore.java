/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.feature.sort.ComplexComparatorFactory;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.csw.store.AbstractCatalogStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.SortByImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;

/**
 * Internal Catalog Store
 * Creates a Catalog Store from a GeoServer Catalog instance and a set of Mappings
 * It can map the internal GS catalog data to 1 or more CSW Record Types, based on one mapping per record type
 * 
 * @author Niels Charlier
 * 
 */
public class InternalCatalogStore extends AbstractCatalogStore {

    protected Catalog catalog;

    protected Map<String, CatalogStoreMapping> mappings = new HashMap<String, CatalogStoreMapping>();

    public InternalCatalogStore(Catalog catalog) {
        support(CSWRecordDescriptor.getInstance());
        support(MetaDataDescriptor.getInstance());

        this.catalog = catalog;
    }
    
    /**
     * Add a Mapping to the Internal Catalog Store
     * 
     * @param typeName record type name for mapping
     * @param mapping the mapping
     */
    public void addMapping(String typeName, CatalogStoreMapping mapping) {
        mappings.put(typeName, mapping);
    }
    
    /**
     * Get Mapping
     * 
     * @param typeName
     * @return the mapping
     */
    public CatalogStoreMapping getMapping(String typeName) {
        return mappings.get(typeName);
    }

    @Override
    public FeatureCollection getRecordsInternal(RecordDescriptor rd, RecordDescriptor rdOutput, Query q, Transaction t) throws IOException {

        CatalogStoreMapping mapping = getMapping(q.getTypeName());
        CatalogStoreMapping outputMapping = getMapping(rdOutput.getFeatureDescriptor().getName().getLocalPart());

        int startIndex = 0;
        if (q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }
        
        CSWUnmappingFilterVisitor unmapper = new CSWUnmappingFilterVisitor(mapping, rd);

        Filter unmapped = Filter.INCLUDE;
        // unmap filter
        if (q.getFilter() != null && q.getFilter() != Filter.INCLUDE) {
            Filter filter = q.getFilter();
            unmapped = (Filter) filter.accept(unmapper, null);
        }
        
        //unmap sortby
        SortBy[] unmappedSortBy = null;
        if (q.getSortBy() != null && q.getSortBy().length > 0) {
        	unmappedSortBy = new SortBy[q.getSortBy().length];
        	for (int i=0; i<q.getSortBy().length; i++) {
        		SortBy sortby = q.getSortBy()[i];
        		Expression expr = (Expression) sortby.getPropertyName().accept(unmapper, null);
        		
        		if (!(expr instanceof PropertyName)) {
        			throw new IOException("Sorting on " + sortby.getPropertyName() + " is not supported.");
        		}
        		
        		unmappedSortBy[i] = new SortByImpl((PropertyName) expr, sortby.getSortOrder());
            	
            }
        } 
        
        if (q.getProperties() != null && q.getProperties().size() > 0) {
        	outputMapping = outputMapping.subMapping(q.getProperties(), rd);
        }

        return new CatalogStoreFeatureCollection(startIndex,
                q.getMaxFeatures(), unmappedSortBy, unmapped, catalog, outputMapping, rdOutput);
    }

    @Override
    public PropertyName translateProperty(RecordDescriptor rd, Name name) {
        return rd.translateProperty(name);
    }

}

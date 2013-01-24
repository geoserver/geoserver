/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.opengis.cat.csw20.ElementSetType;

import org.geoserver.catalog.Catalog;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.csw.store.AbstractCatalogStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;

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

    @Override
    public FeatureCollection getRecordsInternal(RecordDescriptor rd, Query q, Transaction t) throws IOException {

        CatalogStoreMapping mapping = mappings.get(q.getTypeName());

        int startIndex = 0;
        if (q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }

        Filter unmapped = Filter.INCLUDE;
        // unmap filter
        if (q.getFilter() != null && q.getFilter() != Filter.INCLUDE) {
            Filter filter = q.getFilter();
            CSWUnmappingFilterVisitor unmapper = new CSWUnmappingFilterVisitor(mapping, rd);
            unmapped = (Filter) filter.accept(unmapper, null);
        }
               
        if (q.getProperties() != null && q.getProperties().size() > 0) {
            mapping = mapping.subMapping(q.getProperties(), rd);
        }

        FeatureCollection records = new CatalogStoreFeatureCollection(startIndex,
                q.getMaxFeatures(), q.getSortBy(), unmapped, catalog, mapping, rd);

        return records;
    }

    @Override
    public PropertyName translateProperty(RecordDescriptor rd, Name name) {
        return rd.translateProperty(name);
    }

}

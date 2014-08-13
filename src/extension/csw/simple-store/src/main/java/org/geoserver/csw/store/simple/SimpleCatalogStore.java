/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.feature.sort.ComplexComparatorFactory;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.AbstractCatalogStore;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.csw.store.CatalogStoreCapabilities;
import org.geoserver.csw.store.RepositoryItem;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * A simple implementation of {@link CatalogStore} geared towards test support. 
 * The store reads CSW records from xml files located in the root folder, it is not meant to 
 * be fast or scalable, on the contrary, to keep its implementation as simple as possible it
 * is actually slow and occasionally memory bound. 
 * <p>Do not use it for production purposes. 
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SimpleCatalogStore extends AbstractCatalogStore {

    private File root;
    
    public SimpleCatalogStore(File root) {
    	support(CSWRecordDescriptor.getInstance());
    	
        this.root = root;

        if (!root.exists()) {
            throw new IllegalArgumentException("Record directory does not exists: "
                    + root.getPath());
        } else if (!root.isDirectory()) {
            throw new IllegalArgumentException(
                    "Got an existing reference on the file system, but it's not a directory: "
                            + root.getPath());
        }
    }
    
    public FeatureCollection getRecords(Query q, Transaction t) throws IOException {
    	return getRecords(q,t,null);
    }

    @Override
    public FeatureCollection getRecordsInternal(RecordDescriptor rd, RecordDescriptor outputRd, Query q, Transaction t) throws IOException {
       
        int startIndex = 0;
        if (q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }
        FeatureCollection records = new RecordsFeatureCollection(root, startIndex);

        // filtering
        if (q.getFilter() != null && q.getFilter() != Filter.INCLUDE) {
            Filter filter = q.getFilter();
            CSWAnyExpander expander = new CSWAnyExpander();
            Filter expanded = (Filter) filter.accept(expander, null);
            
            records = new FilteringFeatureCollection<FeatureType, Feature>(records, expanded);
        }

        // sorting
        if (q.getSortBy() != null && q.getSortBy().length > 0) {
            Feature[] features = (Feature[]) records.toArray(new Feature[records.size()]);
            Comparator<Feature> comparator = ComplexComparatorFactory.buildComparator(q.getSortBy());
            Arrays.sort(features, comparator);
            
            records = new MemoryFeatureCollection(records.getSchema(), Arrays.asList(features));
        }

        // max features
        if (q.getMaxFeatures() < Query.DEFAULT_MAX) {
            records = new MaxFeaturesFeatureCollection<FeatureType, Feature>(records,
                    q.getMaxFeatures());
        }
        
        // reducing attributes
        if(q.getProperties() != null && q.getProperties().size() > 0) {
            records = new RetypingFeatureCollection(records, q.getProperties());
        }

        return records;
    }

    @Override
    public CatalogStoreCapabilities getCapabilities() {
        return new CatalogStoreCapabilities(descriptorByType) {
          
            @Override
            public boolean supportsGetRepositoryItem(Name typeName) {
                return CSWRecordDescriptor.RECORD_DESCRIPTOR.getName().equals(typeName);
            }            
            
        };
    }

    /**
     * This dummy implementation returns the file backing the record, verbatim
     */
    @Override
    public RepositoryItem getRepositoryItem(String recordId) {
        SimpleRecordIterator it = new SimpleRecordIterator(root, 0);
        while(it.hasNext()) {
            Feature f = it.next();
            if(recordId.equals(f.getIdentifier().getID())) {
                final File file = it.getLastFile();
                return new RepositoryItem() {
                    
                    @Override
                    public String getMime() {
                        return "application/xml";
                    }
                    
                    @Override
                    public InputStream getContents() throws IOException {
                        return new FileInputStream(file);
                    }
                };
            }
        }
        
        // not found
        return null;
    }

}

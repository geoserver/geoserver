/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.csw.records.CSWRecordTypes;
import org.geoserver.csw.store.CatalogCapabilities;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.csw.store.simple.sort.ComplexComparatorFactory;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

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
public class SimpleCatalogStore implements CatalogStore {

    private File root;

    public SimpleCatalogStore(File root) {
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

    @Override
    public FeatureType[] getRecordSchemas() throws IOException {
        // right now we only support CSW record, hopefully this will be extended later
        return new FeatureType[] { CSWRecordTypes.RECORD };
    }

    @Override
    public FeatureCollection getRecords(Query q, Transaction t) throws IOException {
        if (q.getTypeName() != null
                && !q.getTypeName().equals(CSWRecordTypes.RECORD.getName().getLocalPart())) {
            throw new IOException(q.getTypeName() + " is not a supported type");
        }
        if (q.getNamespace() != null
                && !q.getNamespace().toString()
                        .equals(CSWRecordTypes.RECORD.getName().getNamespaceURI())) {
            throw new IOException(q.getNamespace() + ":" + q.getTypeName()
                    + " is not a supported type");
        }

        int startIndex = 0;
        if (q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }
        FeatureCollection records = new RecordsFeatureCollection(root, startIndex);

        // filtering
        if (q.getFilter() != Filter.INCLUDE) {
            records = new FilteringFeatureCollection<FeatureType, Feature>(records, q.getFilter());
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
    public CloseableIterator<String> getDomain(Name typeName, Name attributeName) throws IOException {
//        Query q = new Query(typeName.getLocalPart());
//        q.setNamespace(new URI(typeName.getNamespaceURI()));
//        FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
//        q.setProperties(Arrays.asList(FF.property(attributeName), CSWRecordTypes.));
//        
//        getRecords(q, Transaction.AUTO_COMMIT);
        return null;
    }

    @Override
    public List<FeatureId> addRecord(Feature f, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }

    @Override
    public void deleteRecord(Filter f, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");

    }

    @Override
    public void updateRecord(Name typeName, Name[] attributeNames, Object[] attributeValues,
            Filter filter, Transaction t) throws IOException {
        throw new UnsupportedOperationException("This store does not support transactions yet");
    }

    @Override
    public CatalogCapabilities getCapabilities() {
        // for the moment let's roll with the basic capabilities, we'll add extras later
        return new CatalogCapabilities();
    }

}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

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
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.feature.FeatureCollection;

/**
 * A simple implementation of {@link CatalogStore} geared towards test support. The store reads CSW records from xml
 * files located in the root folder, it is not meant to be fast or scalable, on the contrary, to keep its implementation
 * as simple as possible it is actually slow and occasionally memory bound.
 *
 * <p>Do not use it for production purposes.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SimpleCatalogStore extends AbstractCatalogStore {

    private Resource root;

    public SimpleCatalogStore(Resource root) {
        support(CSWRecordDescriptor.getInstance());

        this.root = root;

        if (root.getType() == Type.RESOURCE) {
            throw new IllegalArgumentException(
                    "Got an existing reference on the file system, but it's not a directory: " + root.path());
        }
    }

    public FeatureCollection<FeatureType, Feature> getRecords(Query q, Transaction t) throws IOException {
        return getRecords(q, t, null);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getRecordsInternal(
            RecordDescriptor rd, RecordDescriptor outputRd, Query q, Transaction t) throws IOException {

        Query pq = prepareQuery(q, rd, rd);

        int startIndex = 0;
        if (q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }
        FeatureCollection<FeatureType, Feature> records = new RecordsFeatureCollection(root, startIndex);

        // filtering
        if (pq.getFilter() != null && pq.getFilter() != Filter.INCLUDE) {
            Filter filter = pq.getFilter();
            CSWAnyExpander expander = new CSWAnyExpander();
            Filter expanded = (Filter) filter.accept(expander, null);

            records = new FilteringFeatureCollection<>(records, expanded);
        }

        // sorting
        if (pq.getSortBy() != null && pq.getSortBy().length > 0) {
            Feature[] features = records.toArray(new Feature[records.size()]);
            Comparator<Feature> comparator = ComplexComparatorFactory.buildComparator(pq.getSortBy());
            Arrays.sort(features, comparator);

            records = new MemoryFeatureCollection(records.getSchema(), Arrays.asList(features));
        }

        // max features
        if (q.getMaxFeatures() < Query.DEFAULT_MAX) {
            records = new MaxFeaturesFeatureCollection<>(records, q.getMaxFeatures());
        }

        // reducing attributes
        if (q.getProperties() != null && !q.getProperties().isEmpty()) {
            records = new RetypingFeatureCollection<>(records, q.getProperties());
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

    /** This dummy implementation returns the file backing the record, verbatim */
    @Override
    public RepositoryItem getRepositoryItem(String recordId) {
        SimpleRecordIterator it = new SimpleRecordIterator(root, 0);
        while (it.hasNext()) {
            Feature f = it.next();
            if (recordId.equals(f.getIdentifier().getID())) {
                final Resource resource = it.getLastFile();
                return new RepositoryItem() {

                    @Override
                    public String getMime() {
                        return "application/xml";
                    }

                    @Override
                    public InputStream getContents() throws IOException {
                        return resource.in();
                    }
                };
            }
        }

        // not found
        return null;
    }
}

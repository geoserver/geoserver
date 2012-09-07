package org.geoserver.csw.store.simple;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geoserver.csw.records.CSWRecordTypes;
import org.geoserver.csw.store.CatalogCapabilities;
import org.geoserver.csw.store.CatalogStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

public class SimpleCatalogStore implements CatalogStore {
    
    private File root;

    public SimpleCatalogStore(File root) {
        this.root = root;
        
        if(!root.exists()) {
            throw new IllegalArgumentException("Record directory does not exists: " + root.getPath());
        } else if(!root.isDirectory()) {
            throw new IllegalArgumentException("Got an existing reference on the file system, but it's not a directory: " + root.getPath());
        }
    }

    @Override
    public FeatureType[] getRecordSchemas() throws IOException {
        // right now we only support CSW record, hopefully this will be extended later
        return new FeatureType[] {CSWRecordTypes.RECORD};
    }

    @Override
    public FeatureCollection getRecords(Query q, Transaction t) throws IOException {
        if(q.getTypeName() != null && !q.getTypeName().equals(CSWRecordTypes.RECORD.getName().getLocalPart())) {
            throw new IOException(q.getTypeName() + " is not a supported type");
        }
        if(q.getNamespace() != null && !q.getNamespace().toString().equals(CSWRecordTypes.RECORD.getName().getNamespaceURI())) {
            throw new IOException(q.getNamespace() + ":" + q.getTypeName() + " is not a supported type");
        }
        
        int startIndex = 0;
        if(q.getStartIndex() != null) {
            startIndex = q.getStartIndex();
        }
        FeatureCollection records = new RecordsFeatureCollection(root, startIndex);
        
        // filtering
        if(q.getFilter() != Filter.INCLUDE) {
            records = new FilteringFeatureCollection<FeatureType, Feature>(records, q.getFilter());
        }
        
        // paging
        if(q.getMaxFeatures() < Query.DEFAULT_MAX) {
            records = new MaxFeaturesFeatureCollection<FeatureType, Feature>(records, q.getMaxFeatures());
        }
        
        // TODO: attribute selection, paging, sorting
        
        return records;
    }

    @Override
    public List<Object> getDomain(Name typeName, Name attributeName) {
        // TODO Auto-generated method stub
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

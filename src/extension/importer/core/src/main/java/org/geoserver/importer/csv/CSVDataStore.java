/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geoserver.importer.csv.parse.CSVStrategy;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public class CSVDataStore extends ContentDataStore implements FileDataStore {

    private final CSVStrategy csvStrategy;

    private final CSVFileState csvFileState;

    public CSVDataStore(CSVFileState csvFileState, CSVStrategy csvStrategy) {
        this.csvFileState = csvFileState;
        this.csvStrategy = csvStrategy;

    }

    public Name getTypeName() {
        return new NameImpl(csvFileState.getTypeName());
    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        return Collections.singletonList(getTypeName());
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new CSVFeatureSource(entry, Query.ALL);
    }

    @Override
    public SimpleFeatureType getSchema() throws IOException {
        return this.csvStrategy.getFeatureType();
    }

    @Override
    public void updateSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleFeatureSource getFeatureSource() throws IOException {
        return new CSVFeatureSource(this);
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader() throws IOException {
        return new CSVFeatureSource(this).getReader();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Filter filter,
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Transaction transaction)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException();
    }

    public CSVStrategy getCSVStrategy() {
        return csvStrategy;
    }

}

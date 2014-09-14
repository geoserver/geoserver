/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv;

import java.io.IOException;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

@SuppressWarnings("unchecked")
public class CSVFeatureSource extends ContentFeatureSource {

    public CSVFeatureSource(CSVDataStore datastore) {
        this(datastore, Query.ALL);
    }

    public CSVFeatureSource(CSVDataStore datastore, Query query) {
        this(new ContentEntry(datastore, datastore.getTypeName()), query);
    }

    public CSVFeatureSource(ContentEntry entry) {
        this(entry, Query.ALL);
    }

    public CSVFeatureSource(ContentEntry entry, Query query) {
        super(entry, query);
    }

    public CSVDataStore getDataStore() {
        return (CSVDataStore) super.getDataStore();
    }

    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        ReferencedEnvelope bounds = new ReferencedEnvelope(getSchema()
                .getCoordinateReferenceSystem());
        FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReader(query);
        try {
            while (featureReader.hasNext()) {
                SimpleFeature feature = featureReader.next();
                bounds.include(feature.getBounds());
            }
        } finally {
            featureReader.close();
        }
        return bounds;
    }

    protected int getCountInternal(Query query) throws IOException {
        FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReader(query);
        int n = 0;
        try {
            for (n = 0; featureReader.hasNext(); n++) {
                featureReader.next();
            }
        } finally {
            featureReader.close();
        }
        return n;
    }

    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        CSVDataStore dataStore = getDataStore();
        return new CSVFeatureReader(dataStore.getCSVStrategy(), query);
    }

    protected SimpleFeatureType buildFeatureType() throws IOException {
        return getDataStore().getSchema();
    }
}

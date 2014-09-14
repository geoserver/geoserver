/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geoserver.importer.csv.parse.CSVIterator;
import org.geoserver.importer.csv.parse.CSVStrategy;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class CSVFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

    private SimpleFeatureType featureType;

    private CSVIterator iterator;

    public CSVFeatureReader(CSVStrategy csvStrategy) throws IOException {
        this(csvStrategy, Query.ALL);
    }

    public CSVFeatureReader(CSVStrategy csvStrategy, Query query)
            throws IOException {
        this.featureType = csvStrategy.getFeatureType();
        this.iterator = csvStrategy.iterator();
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    @Override
    public void close() throws IOException {
        iterator.close();
    }

    @Override
    public SimpleFeature next() throws IOException, IllegalArgumentException,
            NoSuchElementException {
        return iterator.next();
    }

    @Override
    public boolean hasNext() throws IOException {
        return iterator.hasNext();
    }

}

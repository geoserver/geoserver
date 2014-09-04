/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;

import com.csvreader.CsvReader;

public class CSVIterator implements Iterator<SimpleFeature> {

    private int idx;

    private SimpleFeature next;

    private final CsvReader csvReader;

    private final CSVStrategy csvStrategy;

    public CSVIterator(CSVFileState csvFileState, CSVStrategy csvStrategy) throws IOException {
        this.csvStrategy = csvStrategy;
        csvReader = csvFileState.openCSVReader();
        idx = 1;
        next = null;
    }

    private SimpleFeature buildFeature(String[] csvRecord) {
        String id = "" + idx;
        SimpleFeature feature = csvStrategy.createFeature(id, csvRecord);
        idx++;
        return feature;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = readFeature();
        } catch (IOException e) {
            next = null;
        }
        return next != null;
    }

    private SimpleFeature readFeature() throws IOException {
        if (csvReader.readRecord()) {
            String[] csvRecord = csvReader.getValues();
            return buildFeature(csvRecord);
        }
        return null;
    }

    @Override
    public SimpleFeature next() {
        if (next != null) {
            SimpleFeature result = next;
            next = null;
            return result;
        }
        SimpleFeature feature;
        try {
            feature = readFeature();
        } catch (IOException e) {
            feature = null;
        }
        if (feature == null) {
            throw new NoSuchElementException();
        }
        return feature;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove features from csv iteratore");
    }

    public void close() {
        csvReader.close();
    }

}

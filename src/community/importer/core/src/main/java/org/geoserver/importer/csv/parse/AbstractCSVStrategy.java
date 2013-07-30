package org.geoserver.importer.csv.parse;

import java.io.IOException;

import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeatureType;

public abstract class AbstractCSVStrategy implements CSVStrategy {

    protected final CSVFileState csvFileState;

    protected volatile SimpleFeatureType featureType;

    public AbstractCSVStrategy(CSVFileState csvFileState) {
        this.csvFileState = csvFileState;
        featureType = null;
    }

    protected abstract SimpleFeatureType buildFeatureType();

    @Override
    public SimpleFeatureType getFeatureType() {
        if (featureType == null) {
            synchronized (this) {
                if (featureType == null) {
                    featureType = buildFeatureType();
                }
            }
        }
        return featureType;
    }

    @Override
    public CSVIterator iterator() throws IOException {
        return new CSVIterator(csvFileState, this);
    }

}

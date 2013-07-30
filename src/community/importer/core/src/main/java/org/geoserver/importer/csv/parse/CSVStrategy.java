package org.geoserver.importer.csv.parse;

import java.io.IOException;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface CSVStrategy {
    public SimpleFeatureType getFeatureType();

    public CSVIterator iterator() throws IOException;

    public SimpleFeature createFeature(String recordId, String[] csvRecord);
}

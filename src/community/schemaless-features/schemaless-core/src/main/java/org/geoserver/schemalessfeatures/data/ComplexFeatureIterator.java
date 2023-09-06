/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.data;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureIterator;

public class ComplexFeatureIterator implements FeatureIterator<Feature> {

    private FeatureReader<FeatureType, Feature> featureReader;

    public ComplexFeatureIterator(FeatureReader<FeatureType, Feature> reader) {
        this.featureReader = reader;
    }

    @Override
    public boolean hasNext() {
        try {
            return featureReader.hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Feature next() throws NoSuchElementException {
        try {
            return featureReader.next();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            featureReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

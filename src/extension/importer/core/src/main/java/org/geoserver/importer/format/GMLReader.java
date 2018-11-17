/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.PullParser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A reader for GML files
 *
 * @author Andrea Aime - GeoSolutions
 */
class GMLReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

    private final InputStream inputStream;

    private final PullParser parser;

    private final SimpleFeatureType featureType;

    private SimpleFeature next;

    GMLReader(InputStream inputStream, Configuration configuration, SimpleFeatureType featureType) {
        this.inputStream = inputStream;
        this.featureType = featureType;
        this.parser = new PullParser(configuration, inputStream, SimpleFeature.class);
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    @Override
    public SimpleFeature next()
            throws IOException, IllegalArgumentException, NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        } else {
            SimpleFeature result = next;
            next = null;
            return result;
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        if (next != null) {
            return true;
        }
        try {
            SimpleFeature raw = (SimpleFeature) parser.parse();
            if (raw != null) {
                next = SimpleFeatureBuilder.retype(raw, featureType);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }

        return next != null;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}

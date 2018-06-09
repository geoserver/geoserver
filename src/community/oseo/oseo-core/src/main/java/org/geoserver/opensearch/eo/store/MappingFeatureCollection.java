/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.logging.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;

/**
 * Similar to gt-transform code, but building complex features on a custom transformation
 *
 * @author Andrea Aime - GeoSolution
 */
class MappingFeatureCollection extends BaseFeatureCollection<FeatureType, Feature> {

    static final Logger LOGGER = Logging.getLogger(MappingFeatureCollection.class);

    private SimpleFeatureCollection features;

    private Function<PushbackFeatureIterator<SimpleFeature>, Feature> mapper;

    public MappingFeatureCollection(
            FeatureType schema,
            SimpleFeatureCollection features,
            Function<PushbackFeatureIterator<SimpleFeature>, Feature> mapper) {
        super(schema);
        this.features = features;
        this.mapper = mapper;
    }

    @Override
    public FeatureIterator<Feature> features() {
        PushbackFeatureIterator<SimpleFeature> iterator =
                new PushbackFeatureIterator<>(features.features());
        // scan through the joined features and map them
        return new FeatureIterator<Feature>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Feature next() throws NoSuchElementException {
                Feature mapped = mapper.apply(iterator);
                return mapped;
            }

            @Override
            public void close() {
                iterator.close();
            }
        };
    }
}

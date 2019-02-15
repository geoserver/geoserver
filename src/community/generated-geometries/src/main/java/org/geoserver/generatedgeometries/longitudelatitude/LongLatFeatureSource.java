/*
 * (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.longitudelatitude;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.generatedgeometries.GeometryGenerationStrategy;
import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.IOException;

public class LongLatFeatureSource extends DecoratingSimpleFeatureSource {

    private final FeatureTypeInfo featureTypeInfo;
    private final GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy;
    private SimpleFeatureType cachedFeatureType;

    public LongLatFeatureSource(FeatureTypeInfo featureTypeInfo, SimpleFeatureSource delegate, GeometryGenerationStrategy strategy) {
        super(delegate);
        this.featureTypeInfo = featureTypeInfo;
        this.strategy = strategy;
    }

    @Override
    public SimpleFeatureType getSchema() {
        if (cachedFeatureType == null) {
            cachedFeatureType = defineFeatureType();
        }
        return cachedFeatureType;
    }

    private SimpleFeatureType defineFeatureType() {
        SimpleFeatureType src = super.getSchema();
        try {
            return strategy.defineGeometryAttributeFor(featureTypeInfo, src);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return src;
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return super.getFeatures();
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter srcFilter) throws IOException {
        Filter filter = strategy.convertFilter(featureTypeInfo, srcFilter);
        SimpleFeatureCollection features = super.getFeatures(filter);
        return getFeaturesWithGeom(features);
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query srcQuery) throws IOException {
        Query query = strategy.convertQuery(featureTypeInfo, srcQuery);
        SimpleFeatureCollection features = super.getFeatures(query);
        return getFeaturesWithGeom(features);
    }

    private SimpleFeatureCollection getFeaturesWithGeom(SimpleFeatureCollection features) {
        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        SimpleFeatureType schema = getSchema();
        SimpleFeatureIterator iterator = features.features();
        while (iterator.hasNext()) {
            SimpleFeature next = iterator.next();
            collection.add(strategy.generateGeometry(featureTypeInfo, schema, next));
        }
        iterator.close();
        return collection;
    }

}

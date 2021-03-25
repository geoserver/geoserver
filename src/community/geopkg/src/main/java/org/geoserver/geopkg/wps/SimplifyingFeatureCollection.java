/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import java.util.NoSuchElementException;
import java.util.function.Function;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

/** Wraps a feature collection, generalizing it geometry */
class SimplifyingFeatureCollection extends DecoratingSimpleFeatureCollection {

    public static SimpleFeatureCollection simplify(SimpleFeatureCollection fc, double distance) {
        Class<?> binding = fc.getSchema().getGeometryDescriptor().getType().getBinding();

        // if point geometries, no need to simplify
        Function<Geometry, Geometry> simplifier;
        if (Point.class.isAssignableFrom(binding) || MultiPoint.class.isAssignableFrom(binding)) {
            return fc;
        } else if (LineString.class.isAssignableFrom(binding)
                || MultiLineString.class.isAssignableFrom(binding)) {
            simplifier = g -> DouglasPeuckerSimplifier.simplify(g, distance);
        } else {
            simplifier = g -> TopologyPreservingSimplifier.simplify(g, distance);
        }

        return new SimplifyingFeatureCollection(fc, simplifier);
    }

    Function<Geometry, Geometry> simplifier;

    public SimplifyingFeatureCollection(
            SimpleFeatureCollection fc, Function<Geometry, Geometry> simplifier) {
        super(fc);
        this.simplifier = simplifier;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new SimplifyingFeatureIterator(delegate.features());
    }

    private class SimplifyingFeatureIterator extends DecoratingSimpleFeatureIterator {
        private final SimpleFeatureBuilder fb;
        private final GeometryDescriptor gd;

        public SimplifyingFeatureIterator(SimpleFeatureIterator features) {
            super(features);
            SimpleFeatureType schema = getSchema();
            fb = new SimpleFeatureBuilder(schema);
            gd = schema.getGeometryDescriptor();
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature feature = delegate.next();
            fb.init(feature);
            Geometry g = (Geometry) feature.getDefaultGeometry();
            Geometry simplified = simplifier.apply(g);
            simplified.setSRID(g.getSRID());
            fb.set(gd.getName(), simplified);
            return fb.buildFeature(feature.getID());
        }
    }
}

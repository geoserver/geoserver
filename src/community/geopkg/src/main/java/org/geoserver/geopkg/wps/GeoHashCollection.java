/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import java.util.NoSuchElementException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/** Decorates a collection by adding a geohash field */
class GeoHashCollection extends DecoratingSimpleFeatureCollection {

    private final String geoHashFieldName;
    private final SimpleFeatureType schema;

    protected GeoHashCollection(SimpleFeatureCollection delegate) {
        super(delegate);
        this.geoHashFieldName = lookupGeoHashFieldName(delegate);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(delegate.getSchema());
        tb.add(geoHashFieldName, String.class);
        this.schema = tb.buildFeatureType();
    }

    public String getGeoHashFieldName() {
        return geoHashFieldName;
    }

    private String lookupGeoHashFieldName(SimpleFeatureCollection delegate) {
        String geoHashField = "geohash";
        int test = 1;
        while (delegate.getSchema().getDescriptor(geoHashField) != null) {
            geoHashField = "geohash" + test;
            test++;
        }
        return geoHashField;
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new GeoHashIterator(delegate.features());
    }

    @Override
    public <F> F[] toArray(F[] a) {
        int i = 0;
        try (SimpleFeatureIterator fi = features()) {
            while (fi.hasNext()) {
                SimpleFeature next = fi.next();
                if (i < a.length) {
                    a[i] = (F) next;
                    i++;
                } else {
                    break;
                }
            }
        }

        return a;
    }

    private class GeoHashIterator extends DecoratingSimpleFeatureIterator {

        private final SimpleFeatureBuilder fb;
        private final GeoHashCalculator geohash;
        private double[] coords;

        public GeoHashIterator(SimpleFeatureIterator delegate) {
            super(delegate);
            this.fb = new SimpleFeatureBuilder(GeoHashCollection.this.schema);
            this.geohash = getCalculator();
        }

        private GeoHashCalculator getCalculator() {
            // geohash needs geometries in WGS84
            try {
                CoordinateReferenceSystem crs = getSchema().getCoordinateReferenceSystem();
                return GeoHashCalculator.get(crs);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            try {
                SimpleFeature next = delegate.next();
                fb.init(next);
                String hash = geohash.compute((Geometry) next.getDefaultGeometry());
                fb.set(geoHashFieldName, hash);
                return fb.buildFeature(next.getID());
            } catch (TransformException e) {
                throw new RuntimeException(
                        "Failed to transform geometry during geoHash calculation");
            }
        }
    }
}

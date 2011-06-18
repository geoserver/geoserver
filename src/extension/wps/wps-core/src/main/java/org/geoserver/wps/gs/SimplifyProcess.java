/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * A process simplifying the geometries in the input feature collection according to a specified
 * distance, and using either a topology preserving or a Douglas-Peuker algorithm
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
@DescribeProcess(title = "simplify", description = "Simplifies the geometry")
public class SimplifyProcess implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The simplified feature collection")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection to be simplified") SimpleFeatureCollection features,
            @DescribeParameter(name = "distance", description = "The simplification distance (should be positive)") double distance,
            @DescribeParameter(name = "preserveTopology", description = "Wheter a topology preserving simplification should be used", min = 0) Boolean preserveTopology)
            throws ProcessException {
        if (distance < 0) {
            throw new ProcessException("Invalid distance, it should be a positive number");
        }

        return new SimplifyingFeatureCollection(features, distance,
                preserveTopology == null ? Boolean.FALSE : preserveTopology);
    }

    static class SimplifyingFeatureCollection extends DecoratingSimpleFeatureCollection {
        double distance;

        boolean preserveTopology;

        public SimplifyingFeatureCollection(SimpleFeatureCollection delegate, double distance,
                boolean preserveTopology) {
            super(delegate);
            this.distance = distance;
            this.preserveTopology = preserveTopology;
        }

        @Override
        public SimpleFeatureIterator features() {
            return new SimplifyingFeatureIterator(delegate.features(), distance, preserveTopology,
                    getSchema());
        }

        @Override
        public Iterator<SimpleFeature> iterator() {
            return new WrappingIterator(features());
        }

        @Override
        public void close(Iterator<SimpleFeature> close) {
            if (close instanceof WrappingIterator) {
                ((WrappingIterator) close).close();
            }
        }
    }

    static class SimplifyingFeatureIterator implements SimpleFeatureIterator {
        SimpleFeatureIterator delegate;

        double distance;

        boolean preserveTopology;

        SimpleFeatureBuilder fb;

        public SimplifyingFeatureIterator(SimpleFeatureIterator delegate, double distance,
                boolean preserveTopology, SimpleFeatureType schema) {
            this.delegate = delegate;
            this.distance = distance;
            this.preserveTopology = preserveTopology;
            fb = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature f = delegate.next();
            for (Object attribute : f.getAttributes()) {
                if (attribute instanceof Geometry) {
                    if (preserveTopology) {
                        attribute = DouglasPeuckerSimplifier.simplify((Geometry) attribute,
                                distance);
                    } else {
                        attribute = TopologyPreservingSimplifier.simplify((Geometry) attribute,
                                distance);
                    }
                }
                fb.add(attribute);
            }
            return fb.buildFeature(f.getID());
        }

    }
}

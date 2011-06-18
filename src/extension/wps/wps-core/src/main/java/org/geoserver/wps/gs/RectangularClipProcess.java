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
import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A process clipping the geometries in the input feature collection to a specified area
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
@DescribeProcess(title = "rectangularClip", description = "Clips the features to the specified bounding box")
public class RectangularClipProcess implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The feature collection bounds")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection to be simplified") SimpleFeatureCollection features,
            @DescribeParameter(name = "clip", description = "The clipping area") ReferencedEnvelope clip)
            throws ProcessException {
        return new ClippingFeatureCollection(features, clip);
    }

    static class ClippingFeatureCollection extends DecoratingSimpleFeatureCollection {
        ReferencedEnvelope clip;

        public ClippingFeatureCollection(SimpleFeatureCollection delegate, ReferencedEnvelope clip) {
            super(delegate);
            this.clip = clip;
        }

        @Override
        public SimpleFeatureIterator features() {
            return new ClippingFeatureIterator(delegate.features(), clip, getSchema());
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

    static class ClippingFeatureIterator implements SimpleFeatureIterator {
        SimpleFeatureIterator delegate;

        GeometryClipper clipper;

        boolean preserveTopology;

        SimpleFeatureBuilder fb;

        SimpleFeature next;

        public ClippingFeatureIterator(SimpleFeatureIterator delegate, ReferencedEnvelope clip,
                SimpleFeatureType schema) {
            this.delegate = delegate;
            this.clipper = new GeometryClipper(clip);
            fb = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                // a flag telling us if any geometry of the feature survived the clip
                boolean clippedOut = true;
                SimpleFeature f = delegate.next();
                for (Object attribute : f.getAttributes()) {
                    if (attribute instanceof Geometry) {
                        attribute = clipper.clip((Geometry) attribute, true);
                        if (attribute != null) {
                            clippedOut = false;
                        }
                    }
                    fb.add(attribute);
                }
                if (!clippedOut) {
                    next = fb.buildFeature(f.getID());
                }
                fb.reset();
            }

            return next != null;
        }

        public SimpleFeature next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("hasNext() returned false!");
            }

            SimpleFeature result = next;
            next = null;
            return result;
        }

    }
}

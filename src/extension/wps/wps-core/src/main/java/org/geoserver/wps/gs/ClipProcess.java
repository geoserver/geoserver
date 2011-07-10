/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A process clipping the geometries in the input feature collection to a specified area
 * 
 * @author Andrea Aime - GeoSolutions
 */
@DescribeProcess(title = "rectangularClip", description = "Clips the features to the specified geometry")
public class ClipProcess implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The feature collection bounds")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection to be simplified") SimpleFeatureCollection features,
            @DescribeParameter(name = "clip", description = "The clipping area (in the same SRS as the feature collection") Geometry clip)
            throws ProcessException {
        // only get the geometries in the bbox of the clip
        Envelope box = clip.getEnvelopeInternal();
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        String srs = null;
        if(features.getSchema().getCoordinateReferenceSystem() != null) {
            srs = CRS.toSRS(features.getSchema().getCoordinateReferenceSystem());
        }
        BBOX bboxFilter = ff.bbox("", box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY(), srs);
        
        // return dynamic collection clipping geometries on the fly
        return new ClippingFeatureCollection(features.subCollection(bboxFilter), clip);
    }

    static class ClippingFeatureCollection extends DecoratingSimpleFeatureCollection {
        Geometry clip;
        SimpleFeatureType targetSchema;

        public ClippingFeatureCollection(SimpleFeatureCollection delegate, Geometry clip) {
            super(delegate);
            this.clip = clip;
            this.targetSchema = buildTargetSchema(delegate.getSchema());
        }
        
        /**
         * When clipping lines and polygons can turn into multilines and multipolygons
         */
        private SimpleFeatureType buildTargetSchema(SimpleFeatureType schema) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
                if(ad instanceof GeometryDescriptor) {
                    GeometryDescriptor gd = (GeometryDescriptor) ad;
                    Class<?> binding = ad.getType().getBinding();
                    if(Point.class.isAssignableFrom(binding) || GeometryCollection.class.isAssignableFrom(binding)) {
                        tb.add(ad);
                    } else {
                        Class target;
                        if(LineString.class.isAssignableFrom(binding)) {
                            target = MultiLineString.class;
                        } else if(Polygon.class.isAssignableFrom(binding)) {
                            target = MultiPolygon.class;
                        } else {
                            throw new RuntimeException("Don't know how to handle geometries of type " 
                                    + binding.getCanonicalName());
                        }
                        tb.minOccurs(ad.getMinOccurs());
                        tb.maxOccurs(ad.getMaxOccurs());
                        tb.nillable(ad.isNillable());
                        tb.add(ad.getLocalName(), target, gd.getCoordinateReferenceSystem());
                    }
                } else {
                    tb.add(ad);
                }
            }
            tb.setName(schema.getName());
            return tb.buildFeatureType();
        }
        
        @Override
        public SimpleFeatureType getSchema() {
            return targetSchema;
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

        Geometry clip;

        public ClippingFeatureIterator(SimpleFeatureIterator delegate, Geometry clip,
                SimpleFeatureType schema) {
            this.delegate = delegate;
            
            // can we use the fast clipper?
            if(clip.getEnvelope().equals(clip)) {
                this.clipper = new GeometryClipper(clip.getEnvelopeInternal());
            } else {
                this.clip = clip;
            }
                
            fb = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            while (next == null && delegate.hasNext()) {
                boolean clippedOut = false;
                
                // try building the clipped feature out of the original feature, if the
                // default geometry is clipped out, skip it
                SimpleFeature f = delegate.next();
                for (AttributeDescriptor ad : f.getFeatureType().getAttributeDescriptors()) {
                    Object attribute = f.getAttribute(ad.getName());
                    if (ad instanceof GeometryDescriptor) {
                        Class target = ad.getType().getBinding();
                        attribute = clipGeometry((Geometry) attribute, target);
                        if (attribute == null && f.getFeatureType().getGeometryDescriptor() == ad) {
                            // the feature has been clipped out
                            fb.reset();
                            clippedOut = true;
                            break;
                        }
                    } 
                    fb.add(attribute);
                }
                
                if(!clippedOut) {
                    // build the next feature
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
        
        private Object clipGeometry(Geometry geom, Class target) {
            // first off, clip
            Geometry clipped = null;
            if(clipper != null) {
                clipped = clipper.clip(geom, true);
            } else {
                if(geom.getEnvelopeInternal().intersects(clip.getEnvelopeInternal())) {
                    clipped = clip.intersection(geom);
                }
            }
            
            // empty intersection?
            if(clipped == null || clipped.getNumGeometries() == 0) {
                return null;
            }
            
            // map the result to the target output type, removing the spurious lower dimensional
            // elements that might result out of the intersection
            if(Point.class.isAssignableFrom(target) || MultiPoint.class.isAssignableFrom(target) 
                    || GeometryCollection.class.equals(target)) {
                return clipped;
            } else if(MultiLineString.class.isAssignableFrom(target)) {
                final List<LineString> geoms = new ArrayList<LineString>();
                clipped.apply(new GeometryComponentFilter() {
                    
                    @Override
                    public void filter(Geometry geom) {
                        if(geom instanceof LineString) {
                            geoms.add((LineString) geom);
                        }
                    }
                });
                LineString[] lsArray = (LineString[]) geoms.toArray(new LineString[geoms.size()]);
                return geom.getFactory().createMultiLineString(lsArray);
            } else if(MultiPolygon.class.isAssignableFrom(target)) {
                final List<Polygon> geoms = new ArrayList<Polygon>();
                clipped.apply(new GeometryComponentFilter() {
                    
                    @Override
                    public void filter(Geometry geom) {
                        if(geom instanceof Polygon) {
                            geoms.add((Polygon) geom);
                        }
                    }
                });
                Polygon[] lsArray = (Polygon[]) geoms.toArray(new Polygon[geoms.size()]);
                return geom.getFactory().createMultiPolygon(lsArray);

            } else {
                throw new RuntimeException("Unrecognized target type " + target.getCanonicalName());
            }
        }

    }
}

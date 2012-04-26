package org.geoserver.data.geogit;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.WrappedSerialisingFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.Hints;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.BoundedSpatialOperator;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GeoGitSimpleFeatureCollection implements SimpleFeatureCollection {

    private final SimpleFeatureType type;

    private final Filter filter;

    private final ObjectDatabase odb;

    private final FeatureReprojector reprojector;

    private Integer cachedSize;

    private ReferencedEnvelope cachedBounds;

    private GeometryFactory geometryFactory;

    private final RevTree typeTree;

    private Integer maxFeatures;

    public GeoGitSimpleFeatureCollection(final SimpleFeatureType type, final Filter filter,
            final ObjectDatabase odb, final RevTree typeTree) {
        this.type = type;
        this.filter = filter;
        this.odb = odb;
        this.typeTree = typeTree;
        this.reprojector = new FeatureReprojector(type);
    }

    public void setGeometryFactory(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    public void setMaxFeatures(Integer maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    private static class FeatureReprojector {

        private final GeometryDescriptor[] geometryDescriptors;

        private GeometryDescriptor mainGeometry;

        public FeatureReprojector(final SimpleFeatureType type) {
            this.mainGeometry = type.getGeometryDescriptor();

            List<GeometryDescriptor> list = new ArrayList<GeometryDescriptor>(2);
            for (AttributeDescriptor att : type.getAttributeDescriptors()) {
                if (att instanceof GeometryDescriptor) {
                    list.add((GeometryDescriptor) att);
                }
            }
            this.geometryDescriptors = list.toArray(new GeometryDescriptor[list.size()]);
        }

        /**
         * Expands {@code bounds} to include {@code featureBounds}
         * 
         * @throws TransformException
         */
        public void expandToInclude(ReferencedEnvelope target, final BoundingBox featureBounds)
                throws TransformException {
            final CoordinateReferenceSystem crs = target.getCoordinateReferenceSystem();
            final BoundingBox reprojected = ensureCompatibleCrs(featureBounds, crs);
            target.include(reprojected);
        }

        public BoundingBox ensureCompatibleCrs(final BoundingBox featureBounds,
                final CoordinateReferenceSystem targetCrs) throws TransformException {

            final CoordinateReferenceSystem featureCrs = featureBounds
                    .getCoordinateReferenceSystem();
            if (CRS.equalsIgnoreMetadata(targetCrs, featureCrs)) {
                return featureBounds;
            }

            return featureBounds.toBounds(targetCrs);
        }

        public SimpleFeature reproject(SimpleFeature feature) throws Exception {
            GeometryDescriptor geometryDescriptor;
            CoordinateReferenceSystem sourceCrs;
            CoordinateReferenceSystem targetCrs;
            String name;
            for (int i = 0; i < geometryDescriptors.length; i++) {
                geometryDescriptor = geometryDescriptors[i];
                targetCrs = geometryDescriptor.getCoordinateReferenceSystem();
                name = geometryDescriptor.getLocalName();
                Geometry geometry = (Geometry) feature.getAttribute(name);
                if (geometry != null) {
                    sourceCrs = (CoordinateReferenceSystem) geometry.getUserData();
                    if (sourceCrs != null && !CRS.equalsIgnoreMetadata(sourceCrs, targetCrs)) {
                        MathTransform mathTransform = CRS.findMathTransform(sourceCrs, targetCrs,
                                true);
                        geometry = JTS.transform((Geometry) geometry, mathTransform);
                        geometry.setUserData(targetCrs);
                        feature.setAttribute(name, geometry);
                    }
                }
            }
            return feature;
        }
    }

    /**
     * @see org.geotools.feature.FeatureCollection#getSchema()
     */
    @Override
    public SimpleFeatureType getSchema() {
        return type;
    }

    /**
     * @see org.geotools.feature.FeatureCollection#getID()
     */
    @Override
    public String getID() {
        return null;
    }

    /**
     * @see org.geotools.feature.FeatureCollection#purge()
     */
    @Override
    public void purge() {
    }

    /**
     * @see org.geotools.feature.FeatureCollection#getBounds()
     */
    @Override
    public ReferencedEnvelope getBounds() {
        if (this.cachedBounds != null) {
            return this.cachedBounds;
        }

        final CoordinateReferenceSystem crs = type.getCoordinateReferenceSystem();
        ReferencedEnvelope bounds = new ReferencedEnvelope(crs);

        if (BigInteger.ZERO.equals(typeTree.size())) {
            return bounds;
        }

        final FeatureRefIterator refs = new FeatureRefIterator(typeTree, filter);

        BoundingBox featureBounds;
        try {
            if (refs.isFullySupported()) {
                while (refs.hasNext()) {
                    Ref ref = refs.next();
                    if (ref instanceof SpatialRef) {
                        SpatialRef sp = (SpatialRef) ref;
                        featureBounds = sp.getBounds();
                        reprojector.expandToInclude(bounds, featureBounds);
                    }
                }
            } else {
                Iterator<SimpleFeature> features = new GeoGitFeatureIterator(refs, type, filter,
                        odb);
                while (features.hasNext()) {
                    featureBounds = features.next().getBounds();
                    reprojector.expandToInclude(bounds, featureBounds);
                }
            }
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }

        this.cachedBounds = bounds;
        return bounds;
    }

    /**
     * @see org.geotools.feature.FeatureCollection#size()
     */
    @Override
    public int size() {
        if (this.cachedSize != null) {
            return this.cachedSize.intValue();
        }

        if (Filter.INCLUDE.equals(filter)) {
            final BigInteger size = typeTree.size();
            return size.intValue();
        }

        final FeatureRefIterator refs = new FeatureRefIterator(typeTree, filter);
        int size;
        if (refs.isFullySupported()) {
            size = Iterators.size(refs);
        } else {
            Iterator<SimpleFeature> features = new GeoGitFeatureIterator(refs, type, filter, odb);
            size = Iterators.size(features);
        }

        this.cachedSize = Integer.valueOf(size);
        return size;
    }

    /**
     * @see org.geotools.feature.FeatureCollection#iterator()
     */
    @Override
    public Iterator<SimpleFeature> iterator() {
        final FeatureRefIterator refs = new FeatureRefIterator(typeTree, filter);
        Iterator<SimpleFeature> features = new GeoGitFeatureIterator(refs, type, filter, odb);
        if (maxFeatures != null) {
            features = Iterators.limit(features, maxFeatures.intValue());
        }
        return features;
    }

    /**
     * @see org.geotools.data.simple.SimpleFeatureCollection#features()
     * @see #iterator()
     */
    @Override
    public SimpleFeatureIterator features() {
        final Iterator<SimpleFeature> iterator = iterator();
        return new SimpleFeatureIterator() {

            @Override
            public SimpleFeature next() throws NoSuchElementException {
                return iterator.next();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public void close() {
                // nothing to do?
            }
        };
    }

    /**
     * @author groldan
     * 
     */
    private class BBOXPredicate implements Predicate<Ref> {

        private BoundingBox filter;

        public BBOXPredicate(final BinarySpatialOperator boundedFilter) {

            Expression right = boundedFilter.getExpression2();
            if (right instanceof Literal) {
                Object literal = right.evaluate(null);
                if (literal instanceof BoundingBox) {
                    this.filter = (BoundingBox) literal;
                } else if (literal instanceof Geometry) {
                    Geometry geom = (Geometry) literal;
                    CoordinateReferenceSystem crs = null;
                    if (geom.getUserData() instanceof CoordinateReferenceSystem) {
                        crs = (CoordinateReferenceSystem) geom.getUserData();
                    } else if (type.getGeometryDescriptor() != null) {
                        crs = type.getGeometryDescriptor().getCoordinateReferenceSystem();
                    } else {
                        throw new IllegalStateException("Can't determine CRS of filter geometry");
                    }
                    this.filter = new ReferencedEnvelope(geom.getEnvelopeInternal(), crs);
                } else {
                    throw new IllegalArgumentException(
                            "Right operand of BBOX filter can't be resolved to a BoundingBox: "
                                    + right);
                }
            }
        }

        @Override
        public boolean apply(final Ref featureRef) {
            if (!(featureRef instanceof SpatialRef)) {
                return false;
            }
            final CoordinateReferenceSystem targetCrs = filter.getCoordinateReferenceSystem();
            BoundingBox bounds;
            try {
                bounds = reprojector.ensureCompatibleCrs(((SpatialRef) featureRef).getBounds(),
                        targetCrs);
                final boolean apply = filter.intersects(bounds);
                return apply;
            } catch (TransformException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @author groldan
     * 
     */
    private class FeatureRefIterator extends AbstractIterator<Ref> {

        private final Iterator<Ref> refs;

        private final Filter filter;

        public FeatureRefIterator(final RevTree typeTree, final Filter filter) {
            this.filter = filter;

            if (Filter.INCLUDE.equals(filter)) {
                refs = typeTree.iterator(null);
                return;
            }
            if (filter instanceof BoundedSpatialOperator && filter instanceof BinarySpatialOperator) {
                refs = typeTree.iterator(new BBOXPredicate((BinarySpatialOperator) filter));
                return;
            }

            // can't optimize here
            refs = typeTree.iterator(null);
        }

        public boolean isFullySupported() {
            return Filter.INCLUDE.equals(filter) || filter instanceof BBOX;
        }

        @Override
        protected Ref computeNext() {
            if (!refs.hasNext()) {
                return endOfData();
            }
            return refs.next();
        }

    }

    private class GeoGitFeatureIterator extends AbstractIterator<SimpleFeature> {

        private final Iterator<Ref> featureRefs;

        private final SimpleFeatureType type;

        private final Filter filter;

        private final ObjectDatabase odb;

        final WrappedSerialisingFactory serialisingFactory;

        public GeoGitFeatureIterator(final Iterator<Ref> featureRefs, final SimpleFeatureType type,
                final Filter filter, final ObjectDatabase odb) {
            this.featureRefs = featureRefs;
            this.type = type;
            this.filter = filter;
            this.odb = odb;
            this.serialisingFactory = WrappedSerialisingFactory.getInstance();
        }

        @Override
        protected SimpleFeature computeNext() {
            Hints hints = new Hints();
            if (null != geometryFactory) {
                hints.put(Hints.GEOMETRY_FACTORY, geometryFactory);
            }
            try {
                while (featureRefs.hasNext()) {
                    Ref featureRef = featureRefs.next();
                    String featureId = featureRef.getName();
                    ObjectId contentId = featureRef.getObjectId();

                    SimpleFeature feature;
                    ObjectReader<Feature> featureReader = serialisingFactory.createFeatureReader(
                            type, featureId, hints);

                    feature = (SimpleFeature) odb.get(contentId, featureReader);
                    feature = reprojector.reproject(feature);
                    if (filter.evaluate(feature)) {
                        return feature;
                    }
                }
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            return endOfData();
        }

    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void close(FeatureIterator<SimpleFeature> close) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close(Iterator<SimpleFeature> close) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.geotools.feature.FeatureCollection#addListener(org.geotools.feature.CollectionListener)
     */
    @Override
    public void addListener(CollectionListener listener) throws NullPointerException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.geotools.feature.FeatureCollection#removeListener(org.geotools.feature.CollectionListener)
     */
    @Override
    public void removeListener(CollectionListener listener) throws NullPointerException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.geotools.feature.FeatureCollection#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean add(SimpleFeature obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends SimpleFeature> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(
            FeatureCollection<? extends SimpleFeatureType, ? extends SimpleFeature> resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <O> O[] toArray(O[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleFeatureCollection sort(SortBy order) {
        throw new UnsupportedOperationException();
    }

}

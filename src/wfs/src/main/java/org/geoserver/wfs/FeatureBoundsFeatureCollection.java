/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DecoratingFeature;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * A feature collection wrapping a base collection, returning features that do conform to the
 * specified type (which has have a subset of the attributes in the original schema), and that do
 * use the wrapped features to compute their bounds (so that the SimpleFeature bounds can be
 * computed even if the visible attributes do not include geometries)
 *
 * @author Andrea Aime - TOPP
 */
class FeatureBoundsFeatureCollection extends AbstractFeatureCollection {
    SimpleFeatureCollection wrapped;

    /**
     * Builds a new BoundsFeatureCollection
     *
     * @param wrapped the wrapped feature collection
     * @param targetSchema the target schema
     */
    public FeatureBoundsFeatureCollection(
            final SimpleFeatureCollection wrapped, final SimpleFeatureType targetSchema) {
        super(targetSchema);
        this.wrapped = wrapped;
    }

    /** @author Andrea Aime - TOPP */
    private static class BoundsIterator implements Iterator<SimpleFeature>, Closeable {
        SimpleFeatureIterator wrapped;
        SimpleFeatureType targetSchema;

        public BoundsIterator(SimpleFeatureIterator wrapped, SimpleFeatureType targetSchema) {
            this.wrapped = wrapped;
            this.targetSchema = targetSchema;
        }

        public void close() {
            wrapped.close();
        }

        public boolean hasNext() {
            return wrapped.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature base = wrapped.next();
            return new BoundedFeature(base, targetSchema);
        }

        public void remove() {
            throw new UnsupportedOperationException("Removal is not supported");
        }
    }

    protected Iterator openIterator() {
        return new BoundsIterator(wrapped.features(), schema);
    }

    protected void closeIterator(Iterator close) {
        ((BoundsIterator) close).close();
    }

    public int size() {
        return wrapped.size();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return wrapped.getBounds();
    }

    /**
     * Wraps a SimpleFeature shaving off all attributes not included in the original type, but
     * delegates bounds computation to the original feature.
     *
     * @author Andrea Aime - TOPP
     */
    private static class BoundedFeature extends DecoratingFeature {

        private SimpleFeatureType type;

        public BoundedFeature(SimpleFeature wrapped, SimpleFeatureType type) {
            super(wrapped);

            this.type = type;
        }

        public Object getAttribute(int index) {
            return delegate.getAttribute(type.getDescriptor(index).getName());
        }

        @Override
        public int getAttributeCount() {
            return type.getAttributeCount();
        }

        public Object getAttribute(String path) {
            if (type.getDescriptor(path) == null) return null;
            return delegate.getAttribute(path);
        }

        public Object[] getAttributes(Object[] attributes) {
            Object[] retval =
                    attributes != null ? attributes : new Object[type.getAttributeCount()];
            for (int i = 0; i < retval.length; i++) {
                retval[i] = delegate.getAttribute(type.getDescriptor(i).getName());
            }
            return retval;
        }

        public ReferencedEnvelope getBounds() {
            // we may not have the default geometry around in the reduced feature type,
            // so let's output a referenced envelope if possible
            return new ReferencedEnvelope(delegate.getBounds());
        }

        public Geometry getDefaultGeometry() {
            return getPrimaryGeometry();
        }

        public Geometry getPrimaryGeometry() {
            GeometryDescriptor defaultGeometry = type.getGeometryDescriptor();
            if (defaultGeometry == null) return null;
            return (Geometry) delegate.getAttribute(defaultGeometry.getName());
        }

        public SimpleFeatureType getFeatureType() {
            return type;
        }

        @Override
        public SimpleFeatureType getType() {
            return type;
        }

        public String getID() {
            return delegate.getID();
        }

        public int getNumberOfAttributes() {
            return type.getAttributeCount();
        }

        public void setAttribute(int position, Object val)
                throws IllegalAttributeException, ArrayIndexOutOfBoundsException {
            throw new UnsupportedOperationException("This feature wrapper is read only");
        }

        public void setAttribute(String path, Object attribute) throws IllegalAttributeException {
            throw new UnsupportedOperationException("This feature wrapper is read only");
        }

        public void setDefaultGeometry(Geometry geometry) throws IllegalAttributeException {
            setPrimaryGeometry(geometry);
        }

        public void setPrimaryGeometry(Geometry geometry) throws IllegalAttributeException {
            throw new UnsupportedOperationException("This feature wrapper is read only");
        }
    }
}

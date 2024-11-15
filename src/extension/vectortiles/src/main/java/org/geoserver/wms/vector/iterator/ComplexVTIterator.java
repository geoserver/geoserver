/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector.iterator;

import java.util.Map;
import java.util.TreeMap;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.ComplexAttribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

/** A generic {@link VTIterator} able to work against complex features */
class ComplexVTIterator implements VTIterator {

    FeatureIterator<?> delegate;

    public ComplexVTIterator(FeatureIterator<?> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        this.delegate.close();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public VTFeature next() {
        Feature f = delegate.next();
        return new VTFeature(
                String.valueOf(f.getIdentifier()),
                (Geometry) f.getDefaultGeometryProperty().getValue(),
                getProperties(f));
    }

    private Map<String, Object> getProperties(ComplexAttribute feature) {
        Map<String, Object> props = new TreeMap<>();
        for (Property p : feature.getProperties()) {
            if (!(p instanceof Attribute) || (p instanceof GeometryAttribute)) {
                continue;
            }
            String name = p.getName().getLocalPart();
            Object value;
            if (p instanceof ComplexAttribute) {
                value = getProperties((ComplexAttribute) p);
            } else {
                value = p.getValue();
            }
            if (value != null) {
                props.put(name, value);
            }
        }
        return props;
    }
}

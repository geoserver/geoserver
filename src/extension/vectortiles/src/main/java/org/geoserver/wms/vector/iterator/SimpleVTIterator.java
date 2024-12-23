/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector.iterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;

/**
 * A {@link VTIterator} taking advantage of the leaner {@link SimpleFeature} to avoid extra object allocations (e.g.,
 * {@link SimpleFeature#getProperties()} generates the Property wrappers on the fly).
 */
class SimpleVTIterator implements VTIterator {

    SimpleFeatureIterator delegate;
    VTFeature curr;
    private List<AttributeDescriptor> descriptors;

    public SimpleVTIterator(SimpleFeatureIterator delegate) {
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
        SimpleFeature f = delegate.next();
        return new VTFeature(f.getID(), (Geometry) f.getDefaultGeometry(), getProperties(f));
    }

    private Map<String, Object> getProperties(SimpleFeature f) {
        if (this.descriptors == null) this.descriptors = f.getFeatureType().getAttributeDescriptors();
        List<Object> attributeValues = f.getAttributes();
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < descriptors.size(); i++) {
            AttributeDescriptor ad = descriptors.get(i);
            if (!(ad instanceof GeometryDescriptor)) {
                properties.put(ad.getLocalName(), attributeValues.get(i));
            }
        }

        return properties;
    }
}

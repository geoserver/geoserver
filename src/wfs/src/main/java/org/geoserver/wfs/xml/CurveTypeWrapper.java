/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geometry.jts.CurvedGeometry;
import org.geotools.geometry.jts.MultiCurvedGeometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

public class CurveTypeWrapper implements FeatureType {

    FeatureType delegate;

    public CurveTypeWrapper(FeatureType delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isIdentified() {
        return delegate.isIdentified();
    }

    @Override
    public GeometryDescriptor getGeometryDescriptor() {
        GeometryDescriptor gd = delegate.getGeometryDescriptor();
        return wrapGeometryDescriptor(gd);
    }

    private GeometryDescriptor wrapGeometryDescriptor(GeometryDescriptor gd) {
        GeometryType type = gd.getType();
        Class<?> binding = type.getBinding();
        if (MultiLineString.class.isAssignableFrom(binding)) {
            GeometryType curvedType = new GeometryTypeImpl(
                    type.getName(),
                    MultiCurvedGeometry.class,
                    type.getCoordinateReferenceSystem(),
                    type.isIdentified(),
                    type.isAbstract(),
                    type.getRestrictions(),
                    type.getSuper(),
                    type.getDescription());
            return new GeometryDescriptorImpl(
                    curvedType,
                    gd.getName(),
                    gd.getMinOccurs(),
                    gd.getMaxOccurs(),
                    gd.isNillable(),
                    gd.getDefaultValue());
        } else if (LineString.class.isAssignableFrom(binding)) {
            GeometryType curvedType = new GeometryTypeImpl(
                    type.getName(),
                    CurvedGeometry.class,
                    type.getCoordinateReferenceSystem(),
                    type.isIdentified(),
                    type.isAbstract(),
                    type.getRestrictions(),
                    type.getSuper(),
                    type.getDescription());
            return new GeometryDescriptorImpl(
                    curvedType,
                    gd.getName(),
                    gd.getMinOccurs(),
                    gd.getMaxOccurs(),
                    gd.isNillable(),
                    gd.getDefaultValue());
        } else {
            return gd;
        }
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem();
    }

    @Override
    public AttributeType getSuper() {
        return delegate.getSuper();
    }

    @Override
    public Class<Collection<Property>> getBinding() {
        return delegate.getBinding();
    }

    @Override
    public Collection<PropertyDescriptor> getDescriptors() {
        List<PropertyDescriptor> result = new ArrayList<>();
        Collection<PropertyDescriptor> descriptors = delegate.getDescriptors();
        for (PropertyDescriptor pd : descriptors) {
            if (pd instanceof GeometryDescriptor descriptor) {
                pd = wrapGeometryDescriptor(descriptor);
            }
            result.add(pd);
        }

        return result;
    }

    @Override
    public PropertyDescriptor getDescriptor(Name name) {
        return delegate.getDescriptor(name);
    }

    @Override
    public PropertyDescriptor getDescriptor(String name) {
        return delegate.getDescriptor(name);
    }

    @Override
    public Name getName() {
        return delegate.getName();
    }

    @Override
    public boolean isInline() {
        return delegate.isInline();
    }

    @Override
    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    @Override
    public List<Filter> getRestrictions() {
        return delegate.getRestrictions();
    }

    @Override
    public InternationalString getDescription() {
        return delegate.getDescription();
    }

    @Override
    public Map<Object, Object> getUserData() {
        return delegate.getUserData();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CurveTypeWrapper other = (CurveTypeWrapper) obj;
        if (delegate == null) {
            if (other.delegate != null) return false;
        } else if (!delegate.equals(other.delegate)) return false;
        return true;
    }
}

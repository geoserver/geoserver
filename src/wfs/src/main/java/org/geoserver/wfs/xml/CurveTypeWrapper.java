/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geometry.jts.CurvedGeometry;
import org.geotools.geometry.jts.MultiCurvedGeometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

public class CurveTypeWrapper implements FeatureType {

    FeatureType delegate;

    public CurveTypeWrapper(FeatureType delegate) {
        this.delegate = delegate;
    }

    public boolean isIdentified() {
        return delegate.isIdentified();
    }

    public GeometryDescriptor getGeometryDescriptor() {
        GeometryDescriptor gd = delegate.getGeometryDescriptor();
        return wrapGeometryDescriptor(gd);
    }

    private GeometryDescriptor wrapGeometryDescriptor(GeometryDescriptor gd) {
        GeometryType type = gd.getType();
        Class<?> binding = type.getBinding();
        if (MultiLineString.class.isAssignableFrom(binding)) {
            GeometryType curvedType =
                    new GeometryTypeImpl(
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
            GeometryType curvedType =
                    new GeometryTypeImpl(
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

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem();
    }

    public AttributeType getSuper() {
        return delegate.getSuper();
    }

    public Class<Collection<Property>> getBinding() {
        return delegate.getBinding();
    }

    public Collection<PropertyDescriptor> getDescriptors() {
        List<PropertyDescriptor> result = new ArrayList<>();
        Collection<PropertyDescriptor> descriptors = delegate.getDescriptors();
        for (PropertyDescriptor pd : descriptors) {
            if (pd instanceof GeometryDescriptor) {
                pd = wrapGeometryDescriptor((GeometryDescriptor) pd);
            }
            result.add(pd);
        }

        return result;
    }

    public PropertyDescriptor getDescriptor(Name name) {
        return delegate.getDescriptor(name);
    }

    public PropertyDescriptor getDescriptor(String name) {
        return delegate.getDescriptor(name);
    }

    public Name getName() {
        return delegate.getName();
    }

    public boolean isInline() {
        return delegate.isInline();
    }

    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    public List<Filter> getRestrictions() {
        return delegate.getRestrictions();
    }

    public InternationalString getDescription() {
        return delegate.getDescription();
    }

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

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.type;

import java.util.Collection;
import java.util.List;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;

/** A FeatureType that allows new PropertyDescriptor to be added */
public class DynamicFeatureType extends DynamicComplexTypeImpl implements FeatureType {

    private GeometryDescriptor defaultGeometry;
    private CoordinateReferenceSystem crs;

    public DynamicFeatureType(
            Name name,
            Collection<PropertyDescriptor> schema,
            GeometryDescriptor defaultGeometry,
            boolean isAbstract,
            List<Filter> restrictions,
            AttributeType superType,
            InternationalString description) {
        super(name, schema, true, isAbstract, restrictions, superType, description);

        this.defaultGeometry = defaultGeometry;

        if (defaultGeometry != null && !(defaultGeometry.getType() instanceof GeometryType)) {
            throw new IllegalArgumentException("defaultGeometry must have a GeometryType");
        }
    }

    @Override
    public GeometryDescriptor getGeometryDescriptor() {
        return defaultGeometry;
    }

    public void setGeometryDescriptor(GeometryDescriptor geometryDescriptor) {
        this.defaultGeometry = geometryDescriptor;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        if (crs == null) {
            if (getGeometryDescriptor() != null
                    && getGeometryDescriptor().getType().getCoordinateReferenceSystem() != null) {
                crs = defaultGeometry.getType().getCoordinateReferenceSystem();
            }
            if (crs == null) {
                for (PropertyDescriptor property : getDescriptors()) {
                    if (property instanceof GeometryDescriptor) {
                        GeometryDescriptor geometry = (GeometryDescriptor) property;
                        if (geometry.getType().getCoordinateReferenceSystem() != null) {
                            crs = geometry.getType().getCoordinateReferenceSystem();
                            break;
                        }
                    }
                }
            }
        }

        return crs;
    }
}

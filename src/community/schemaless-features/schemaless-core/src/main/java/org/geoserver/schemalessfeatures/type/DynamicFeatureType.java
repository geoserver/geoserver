/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.type;

import java.util.Collection;
import java.util.List;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

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

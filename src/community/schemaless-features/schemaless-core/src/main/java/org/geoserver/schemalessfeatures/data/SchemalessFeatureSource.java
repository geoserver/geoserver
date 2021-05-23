/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.schemalessfeatures.type.DynamicFeatureType;
import org.geotools.gml3.v3_2.GMLSchema;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * A FeatureSource relying on a DynamicFeatureType. The getSchema() method will provide by default
 * an empty schema. Subclasses might implement the getGeometryDescriptor to provide at least the
 * geometry definition
 */
public abstract class SchemalessFeatureSource extends ComplexFeatureSource {

    public SchemalessFeatureSource(Name name, ComplexContentDataAccess store) {
        super(name, store);
    }

    @Override
    public FeatureType getSchema() {
        FeatureType featureType = null;
        if (featureType == null) {
            GeometryDescriptor descriptor = getGeometryDescriptor();
            List<PropertyDescriptor> descriptorList = new ArrayList<>();
            descriptorList.add(descriptor);
            featureType =
                    new DynamicFeatureType(
                            name,
                            descriptorList,
                            descriptor,
                            false,
                            Collections.emptyList(),
                            GMLSchema.ABSTRACTFEATURETYPE_TYPE,
                            null);
        }
        return featureType;
    }

    /**
     * Get the GeometryDescriptor to be added as the default geometry to the DynamicFeatureType. By
     * default returns null.
     *
     * @return the GeometryDescriptor
     */
    protected abstract GeometryDescriptor getGeometryDescriptor();
}

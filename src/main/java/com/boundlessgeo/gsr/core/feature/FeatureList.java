package com.boundlessgeo.gsr.core.feature;

import com.boundlessgeo.gsr.core.GSRModel;
import com.boundlessgeo.gsr.core.geometry.GeometryEncoder;
import com.boundlessgeo.gsr.core.geometry.GeometryTypeEnum;
import com.boundlessgeo.gsr.core.geometry.SpatialReference;
import com.boundlessgeo.gsr.core.geometry.SpatialReferences;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * List of {@link Feature}
 */
public class FeatureList implements GSRModel {

    public final String objectIdFieldName = "objectid";
    public final String globalIdFieldName = "";
    public final String geometryType;
    public final SpatialReference spatialReference;

    public final ArrayList<Field> fields = new ArrayList<>();
    public final ArrayList<Feature> features = new ArrayList<>();

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(FeatureCollection<T, F> collection, boolean returnGeometry) throws IOException {
        FeatureIterator<F> iterator = collection.features();

        T schema = collection.getSchema();

        if (returnGeometry) {
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            if (geometryDescriptor == null) {
                throw new RuntimeException("No geometry descriptor for type " + schema + "; " + schema.getDescriptors());
            }
            GeometryType geometryType = geometryDescriptor.getType();
            if (geometryType == null) {
                throw new RuntimeException("No geometry type for type " + schema);
            }
            Class<?> binding = geometryType.getBinding();
            if (binding == null) {
                throw new RuntimeException("No binding for geometry type " + schema);
            }
            GeometryTypeEnum geometryTypeEnum = GeometryTypeEnum.forJTSClass(binding);
            this.geometryType = geometryTypeEnum.getGeometryType();
        } else {
            this.geometryType = null;
        }

        if (schema.getCoordinateReferenceSystem() != null) {
            try {
                spatialReference = SpatialReferences.fromCRS(schema.getCoordinateReferenceSystem());
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            spatialReference = null;
        }

        for (PropertyDescriptor desc : schema.getDescriptors()) {
            if (schema.getGeometryDescriptor() != null && !desc.getName().equals(schema.getGeometryDescriptor().getName())) {
                fields.add(FeatureEncoder.field(desc));
            }
        }

        try {
            while (iterator.hasNext()) {
                org.opengis.feature.Feature feature = iterator.next();
                features.add(FeatureEncoder.feature(feature, returnGeometry));
            }
        } finally {
            iterator.close();
        }
    }
}

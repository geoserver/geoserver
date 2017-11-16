package com.boundlessgeo.gsr.core.feature;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;

import com.boundlessgeo.gsr.core.GSRModel;
import com.boundlessgeo.gsr.core.geometry.GeometryTypeEnum;
import com.boundlessgeo.gsr.core.geometry.SpatialReference;
import com.boundlessgeo.gsr.core.geometry.SpatialReferenceWKID;
import com.boundlessgeo.gsr.core.geometry.SpatialReferences;

/**
 * List of {@link Feature}
 */
public class FeatureList implements GSRModel {

    public final String objectIdFieldName = "FID";

    public final String globalIdFieldName = "";

    public final String geometryType;

    public final SpatialReference spatialReference;

    public final ArrayList<Field> fields = new ArrayList<>();

    public final ArrayList<Feature> features = new ArrayList<>();

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(FeatureCollection<T, F> collection, boolean returnGeometry) throws IOException {
        this(collection, returnGeometry, null);
    }

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(
        FeatureCollection<T, F> collection, boolean returnGeometry, String outputSR) throws IOException {

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

        if (StringUtils.isNotEmpty(outputSR)) {
            spatialReference = new SpatialReferenceWKID(Integer.parseInt(outputSR));
        } else if (schema.getCoordinateReferenceSystem() != null) {
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

        fields.add(FeatureEncoder.syntheticObjectIdField(objectIdFieldName));

        try (FeatureIterator<F> iterator = collection.features()) {
            while (iterator.hasNext()) {
                org.opengis.feature.Feature feature = iterator.next();
                features.add(FeatureEncoder.feature(feature, returnGeometry, spatialReference, objectIdFieldName));
            }
        }
    }
}

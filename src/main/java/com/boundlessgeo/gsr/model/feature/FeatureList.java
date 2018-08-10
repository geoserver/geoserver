package com.boundlessgeo.gsr.model.feature;

import java.io.IOException;
import java.util.ArrayList;

import com.boundlessgeo.gsr.Utils;
import com.boundlessgeo.gsr.model.geometry.*;
import com.boundlessgeo.gsr.translate.geometry.SpatialReferenceEncoder;
import com.boundlessgeo.gsr.translate.geometry.SpatialReferences;
import com.boundlessgeo.gsr.translate.geometry.AbstractGeometryEncoder;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import com.boundlessgeo.gsr.translate.geometry.GeometryEncoder;
import com.boundlessgeo.gsr.translate.geometry.QuantizedGeometryEncoder;
import com.vividsolutions.jts.geom.Envelope;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;

import com.boundlessgeo.gsr.model.GSRModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * List of {@link Feature}, that can be serialized as JSON
 *
 * See https://developers.arcgis.com/documentation/common-data-types/featureset-object.htm
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureList implements GSRModel {

    public final String objectIdFieldName = FeatureEncoder.OBJECTID_FIELD_NAME;

    public final String globalIdFieldName = "";

    public final String geometryType;

    public final SpatialReference spatialReference;

    public final Transform transform;

    public final ArrayList<Field> fields = new ArrayList<>();

    public final ArrayList<Feature> features = new ArrayList<>();

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(FeatureCollection<T, F> collection, boolean returnGeometry) throws IOException {
        this(collection, returnGeometry, null);
    }

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(
            FeatureCollection<T, F> collection, boolean returnGeometry, String outputSR) throws IOException {
        this(collection, returnGeometry, outputSR, null);
    }

    public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureList(
        FeatureCollection<T, F> collection, boolean returnGeometry, String outputSR, String quantizationParameters) throws IOException {

        T schema = collection.getSchema();

        //determine geometry type
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

        //determine crs
        CoordinateReferenceSystem outCrs = null;
        if (StringUtils.isNotEmpty(outputSR)) {
            outCrs = Utils.parseSpatialReference(outputSR);
        } else if (schema.getCoordinateReferenceSystem() != null) {
            outCrs = schema.getCoordinateReferenceSystem();
        }
        if (outCrs == null) {
            spatialReference = null;
        } else {
            try {
                spatialReference = SpatialReferences.fromCRS(outCrs);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        }

        AbstractGeometryEncoder geometryEncoder;
        //Parse quantizationParameters
        if (null == quantizationParameters || quantizationParameters.isEmpty()) {
            transform = null;
            geometryEncoder = new GeometryEncoder();
        } else {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(quantizationParameters);

            QuantizedGeometryEncoder.Mode mode = QuantizedGeometryEncoder.Mode.valueOf(json.getString("mode"));
            QuantizedGeometryEncoder.OriginPosition originPosition = QuantizedGeometryEncoder.OriginPosition.valueOf(json.getString("originPosition"));
            Double tolerance = json.getDouble("tolerance");
            Envelope extent = GeometryEncoder.jsonToEnvelope(json.getJSONObject("extent"));
            CoordinateReferenceSystem envelopeCrs = SpatialReferenceEncoder.coordinateReferenceSystemFromJSON(
                    json.getJSONObject("extent").getJSONObject("spatialReference"));

            MathTransform mathTx;
            try {
                mathTx = CRS.findMathTransform(envelopeCrs, outCrs, true);
            } catch (FactoryException e) {
                throw new IllegalArgumentException(
                        "Unable to translate between input and native coordinate reference systems", e);
            }
            Envelope transformedExtent;
            try {
                transformedExtent = JTS.transform(extent, mathTx);
            } catch (TransformException e) {
                throw new IllegalArgumentException(
                        "Error while converting envelope from input to native coordinate system", e);
            }

            //TODO: Transform extent to outSR before determining translate
            //default to upperLeft
            double[] translate = new double[]{transformedExtent.getMinX(), transformedExtent.getMaxY()};
            if (originPosition == QuantizedGeometryEncoder.OriginPosition.bottomRight) {
                translate = new double[]{transformedExtent.getMaxX(), transformedExtent.getMinY()};
            }
            transform = new Transform(originPosition, new double[]{tolerance, tolerance},
                    translate);

            geometryEncoder = new QuantizedGeometryEncoder(
                    mode,
                    originPosition,
                    tolerance,
                    transformedExtent);
        }

        for (PropertyDescriptor desc : schema.getDescriptors()) {
            if (schema.getGeometryDescriptor() != null && !desc.getName().equals(schema.getGeometryDescriptor().getName())) {
                fields.add(FeatureEncoder.field(desc, null));
            }
        }

        fields.add(FeatureEncoder.syntheticObjectIdField(objectIdFieldName));

        try (FeatureIterator<F> iterator = collection.features()) {
            while (iterator.hasNext()) {
                org.opengis.feature.Feature feature = iterator.next();
                features.add(FeatureEncoder.feature(feature, returnGeometry, spatialReference, objectIdFieldName, geometryEncoder));
            }
        }
    }
}

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.util.Date;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.data.util.TemporalUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A GetFeatureInfo response handler specialized in producing JSON and JSONP data in <em>Slim
 * GeoJSON</em> format for a GetFeature request.
 *
 * @author Carsten Klein, DataGis
 */
public class SlimGeoJSONGetFeatureResponse extends GeoJSONGetFeatureResponse {

    // currently no logger required
    // private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    private static String parseMimeType(String format) {
        int pos = format.indexOf(';');
        return pos != -1 ? format.substring(0, pos).trim() : format;
    }

    public SlimGeoJSONGetFeatureResponse(GeoServer gs, String format) {
        super(gs, format, JSONType.isJsonpMimeType(parseMimeType(format)));
    }

    /** capabilities output format string. */
    @Override
    public String getCapabilitiesElementName() {
        return getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next();
    }

    /** Returns the mime type */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return getOutputFormats().isEmpty() ? null : getOutputFormats().iterator().next();
    }

    /**
     * Modified version of {@link GeoJSONGetFeatureResponse#encodeSimpleFeatures} writing a
     * feature's property values only as an array.
     */
    @Override
    protected FeaturesInfo encodeSimpleFeatures(GeoJSONBuilder jsonWriter,
            List<FeatureCollection> resultsList, boolean featureBounding, Operation operation) {
        String id_option = getIdOption();

        CoordinateReferenceSystem crs = null;
        boolean hasGeom = false;
        long featureCount = 0;
        for (FeatureCollection collection : resultsList) {
            try (FeatureIterator iterator = collection.features()) {
                SimpleFeatureType fType;
                List<AttributeDescriptor> types = null;
                GeometryDescriptor defaultGeomType = null;
                // encode each simple feature
                while (iterator.hasNext()) {
                    // get next simple feature
                    SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
                    featureCount++;
                    // start writing the JSON feature object
                    jsonWriter.object();
                    jsonWriter.key("type").value("Feature");
                    fType = simpleFeature.getFeatureType();
                    types = fType.getAttributeDescriptors();
                    // write the simple feature id
                    if (id_option == null) {
                        // no specific attribute nominated, use the simple feature id
                        jsonWriter.key("id").value(simpleFeature.getID());
                    } else if (id_option.length() != 0) {
                        // a specific attribute was nominated to be used as id
                        Object value = simpleFeature.getAttribute(id_option);
                        jsonWriter.key("id").value(value);
                    }
                    // set that axis order that should be used to write geometries
                    defaultGeomType = fType.getGeometryDescriptor();
                    if (defaultGeomType != null) {
                        CoordinateReferenceSystem featureCrs =
                                defaultGeomType.getCoordinateReferenceSystem();
                        jsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));
                        if (crs == null) {
                            crs = featureCrs;
                        }
                    } else {
                        // If we don't know, assume EAST_NORTH so that no swapping occurs
                        jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
                    }
                    // start writing the simple feature geometry JSON object
                    Geometry aGeom = (Geometry) simpleFeature.getDefaultGeometry();
                    if (aGeom != null || writeNullGeometries()) {
                        jsonWriter.key("geometry");
                        // Write the geometry, whether it is a null or not
                        if (aGeom != null) {
                            jsonWriter.writeGeom(aGeom);
                            hasGeom = true;
                        } else {
                            jsonWriter.value(null);
                        }
                    }
                    // start writing feature properties JSON object
                    jsonWriter.key("properties");
                    jsonWriter.array();
                    for (int j = 0; j < types.size(); j++) {
                        Object value = simpleFeature.getAttribute(j);
                        AttributeDescriptor ad = types.get(j);
                        if (id_option != null && id_option.equals(ad.getLocalName())) {
                            continue; // skip this value as it is used as the id
                        }
                        if (ad instanceof GeometryDescriptor) {
                            // This is an area of the spec where they
                            // decided to 'let convention evolve',
                            // that is how to handle multiple
                            // geometries. My take is to print the
                            // geometry here if it's not the default.
                            // If it's the default that you already
                            // printed above, so you don't need it here.
                            if (!ad.equals(defaultGeomType)) {
                                if (value == null) {
                                    jsonWriter.value(null);
                                } else {
                                    // if it was the default geometry, it has been written above
                                    // already
                                    jsonWriter.writeGeom((Geometry) value);
                                }
                            }
                        } else if (Date.class.isAssignableFrom(ad.getType().getBinding())
                                && TemporalUtils.isDateTimeFormatEnabled()) {
                            // Temporal types print handling
                            jsonWriter.value(TemporalUtils.printDate((Date) value));
                        } else {
                            if ((value instanceof Double && Double.isNaN((Double) value))
                                    || value instanceof Float && Float.isNaN((Float) value)) {
                                jsonWriter.value(null);
                            } else if ((value instanceof Double
                                    && ((Double) value) == Double.POSITIVE_INFINITY)
                                    || value instanceof Float
                                            && ((Float) value) == Float.POSITIVE_INFINITY) {
                                jsonWriter.value("Infinity");
                            } else if ((value instanceof Double
                                    && ((Double) value) == Double.NEGATIVE_INFINITY)
                                    || value instanceof Float
                                            && ((Float) value) == Float.NEGATIVE_INFINITY) {
                                jsonWriter.value("-Infinity");
                            } else {
                                jsonWriter.value(value);
                            }
                        }
                    }
                    jsonWriter.endArray(); // end the properties

                    // Bounding box for feature in properties
                    ReferencedEnvelope refenv =
                            ReferencedEnvelope.reference(simpleFeature.getBounds());
                    if (featureBounding && !refenv.isEmpty()) {
                        jsonWriter.writeBoundingBox(refenv);
                    }

                    writeExtraFeatureProperties(simpleFeature, operation, jsonWriter);

                    jsonWriter.endObject(); // end the feature
                }
            }
        }
        return new FeaturesInfo(crs, hasGeom, featureCount);
    }

    /** Writes schema information */
    @Override
    protected void writeExtraCollectionProperties(FeatureCollectionResponse response,
            Operation operation, GeoJSONBuilder jw) {
        String id_option = getIdOption();

        String geometryName = null;
        List<FeatureCollection> resultsList = response.getFeature();
        FeatureCollection collection = resultsList.get(0);
        try (FeatureIterator iterator = collection.features()) {

            if (iterator.hasNext()) {
                jw.key("schema").object();
                jw.key("properties").array();

                SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
                SimpleFeatureType fType = simpleFeature.getFeatureType();
                GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();
                List<AttributeDescriptor> types = fType.getAttributeDescriptors();

                for (int j = 0; j < types.size(); j++) {
                    AttributeDescriptor ad = types.get(j);
                    if (id_option != null && id_option.equals(ad.getLocalName())) {
                        continue; // skip this attribute as it it used as the id
                    }
                    if (ad.equals(defaultGeomType)) {
                        geometryName = defaultGeomType.getLocalName();
                        continue; // skip this attribute as it is used as default geometry
                    }
                    jw.value(ad.getLocalName());
                }
                jw.endArray();

                if (geometryName != null) {
                    jw.key("geometry_name").value(geometryName);
                }
                jw.endObject();
            }
        }

        super.writeExtraCollectionProperties(response, operation, jw);
    }
}

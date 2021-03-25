/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.translate.feature;

import static java.util.logging.Level.SEVERE;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gsr.Utils;
import org.geoserver.gsr.api.ServiceException;
import org.geoserver.gsr.model.exception.FeatureServiceErrors;
import org.geoserver.gsr.model.exception.ServiceError;
import org.geoserver.gsr.model.feature.EditResult;
import org.geoserver.gsr.model.feature.EditResults;
import org.geoserver.gsr.model.geometry.SpatialReference;
import org.geoserver.gsr.model.geometry.SpatialRelationship;
import org.geoserver.gsr.model.map.LayerOrTable;
import org.geoserver.gsr.model.map.LayersAndTables;
import org.geoserver.gsr.translate.geometry.GeometryEncoder;
import org.geoserver.gsr.translate.geometry.SpatialReferenceEncoder;
import org.geoserver.gsr.translate.geometry.SpatialReferences;
import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.*;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

public class FeatureDAO {
    protected static final FilterFactory2 FILTERS = CommonFactoryFinder.getFilterFactory2();

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(FeatureDAO.class);

    /**
     * Create new features
     *
     * @see #editFeatures(FeatureTypeInfo, List, List, List, boolean, boolean)
     * @see #createFeature(FeatureTypeInfo, FeatureStore, org.geoserver.gsr.model.feature.Feature)
     */
    public static EditResults createFeatures(
            FeatureTypeInfo featureType,
            List<org.geoserver.gsr.model.feature.Feature> sourceFeatures,
            boolean returnEditMoment,
            boolean rollbackOnFailure)
            throws ServiceException {
        return editFeatures(
                featureType, sourceFeatures, null, null, returnEditMoment, rollbackOnFailure);
    }

    /**
     * Update existing features
     *
     * @see #editFeatures(FeatureTypeInfo, List, List, List, boolean, boolean)
     * @see #updateFeature(FeatureTypeInfo, FeatureStore, org.geoserver.gsr.model.feature.Feature)
     */
    public static EditResults updateFeatures(
            FeatureTypeInfo featureType,
            List<org.geoserver.gsr.model.feature.Feature> sourceFeatures,
            boolean returnEditMoment,
            boolean rollbackOnFailure)
            throws ServiceException {
        return editFeatures(
                featureType, null, sourceFeatures, null, returnEditMoment, rollbackOnFailure);
    }

    /**
     * Delete existing features
     *
     * @see #editFeatures(FeatureTypeInfo, List, List, List, boolean, boolean)
     * @see #deleteFeature(FeatureTypeInfo, FeatureStore, Long)
     */
    public static EditResults deleteFeatures(
            FeatureTypeInfo featureType,
            List<Long> ids,
            boolean returnEditMoment,
            boolean rollbackOnFailure)
            throws ServiceException {
        return editFeatures(featureType, null, null, ids, returnEditMoment, rollbackOnFailure);
    }

    /**
     * Add, Update, and/or Delete features within a single FeatureType Handles acquiring a
     * connection to the store, and (optionally) rolling back upon failure.
     *
     * <p>TODO: Consolidate rollback logic to support multiple featureTypes for Apply Edits endpoint
     *
     * @param featureType The featureType
     * @param createFeatures Features to add
     * @param updateFeatures Features to update
     * @param deleteIds Feature Ids to delete
     * @param returnEditMoment Whether or not to incude the modification time in the result
     * @param rollbackOnFailure Whether or not to rollback all edits if one edit fails
     * @return Results of the operation
     * @throws ServiceException if there was an unresolvable error, or if edits were rolled back
     */
    public static EditResults editFeatures(
            FeatureTypeInfo featureType,
            List<org.geoserver.gsr.model.feature.Feature> createFeatures,
            List<org.geoserver.gsr.model.feature.Feature> updateFeatures,
            List<Long> deleteIds,
            boolean returnEditMoment,
            boolean rollbackOnFailure)
            throws ServiceException {

        FeatureStore featureStore;
        EditResults results;
        try {
            featureStore = featureStore(featureType);
        } catch (IOException e) {
            LOGGER.log(
                    Level.INFO,
                    "Error reading feature: "
                            + featureType.getNamespace().getPrefix()
                            + ":"
                            + featureType.getName(),
                    e);
            throw new ServiceException(
                    e, FeatureServiceErrors.nonSpecific(Collections.singletonList(e.getMessage())));
        }
        // Default is Transaction.AUTOCOMMIT
        Transaction transaction = featureStore.getTransaction();
        if (rollbackOnFailure) {
            transaction = new DefaultTransaction();
            featureStore.setTransaction(transaction);
        }

        try {
            results =
                    editFeatures(
                            featureType,
                            featureStore,
                            createFeatures,
                            updateFeatures,
                            deleteIds,
                            rollbackOnFailure);

            if (rollbackOnFailure) {
                transaction.commit();
            }
            if (returnEditMoment) {
                results.setEditMoment(new Date().getTime());
            }

        } catch (ServiceException | IOException e) {
            // Feature error
            if (rollbackOnFailure) {
                try {
                    transaction.rollback();
                    throw new ServiceException(
                            e,
                            FeatureServiceErrors.rolledBack1(
                                    Collections.singletonList(e.getMessage())));
                } catch (IOException e1) {
                    LOGGER.log(Level.WARNING, "", e1);
                    throw new ServiceException(
                            e1,
                            FeatureServiceErrors.nonSpecific(
                                    Arrays.asList(
                                            "Error rolling back after " + e.getMessage(),
                                            e1.getMessage())));
                }
            }
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                // We should only get a ServiceException if rollbackOnFailure is true, so this
                // should never happen...
                throw new ServiceException(
                        e,
                        FeatureServiceErrors.nonSpecific(
                                Collections.singletonList(e.getMessage())));
            }
        } finally {
            try {
                transaction.close();
            } catch (IOException e) {
                LOGGER.log(SEVERE, "Error closing transaction", e);
            }
        }
        return results;
    }

    /**
     * Add, Update, and/or Delete features within a single FeatureType. Wraps create, update, and
     * delete features methods, and aggregates the results into a {@link EditResults}
     *
     * @param exceptionOnFailure Whether to throw a {@link ServiceException} when an edit fails
     * @return Results of the edits; if any of createFeatures, updateFeatures, or deleteIds are
     *     null, the corresponding results list will also be null.
     */
    private static EditResults editFeatures(
            FeatureTypeInfo featureType,
            FeatureStore featureStore,
            List<org.geoserver.gsr.model.feature.Feature> createFeatures,
            List<org.geoserver.gsr.model.feature.Feature> updateFeatures,
            List<Long> deleteIds,
            boolean exceptionOnFailure)
            throws ServiceException {

        List<EditResult> createResults = null;
        if (createFeatures != null) {
            createResults =
                    createFeatures(featureType, featureStore, createFeatures, exceptionOnFailure);
        }

        List<EditResult> updateResults = null;
        if (updateFeatures != null) {
            updateResults =
                    updateFeatures(featureType, featureStore, updateFeatures, exceptionOnFailure);
        }

        List<EditResult> deleteResults = null;
        if (deleteIds != null) {
            deleteResults =
                    deleteFeatures(featureType, featureStore, deleteIds, exceptionOnFailure);
        }

        return new EditResults(createResults, updateResults, deleteResults);
    }

    /**
     * Create new features
     *
     * @see #createFeature(FeatureTypeInfo, FeatureStore, org.geoserver.gsr.model.feature.Feature)
     */
    private static List<EditResult> createFeatures(
            FeatureTypeInfo featureType,
            FeatureStore featureStore,
            List<org.geoserver.gsr.model.feature.Feature> sourceFeatures,
            boolean exceptionOnFailure)
            throws ServiceException {
        List<EditResult> results = new ArrayList<>();
        for (org.geoserver.gsr.model.feature.Feature sourceFeature : sourceFeatures) {
            results.add(
                    validateResult(
                            createFeature(featureType, featureStore, sourceFeature),
                            exceptionOnFailure));
        }
        return results;
    }

    /**
     * Update existing features
     *
     * @see #updateFeature(FeatureTypeInfo, FeatureStore, org.geoserver.gsr.model.feature.Feature)
     */
    private static List<EditResult> updateFeatures(
            FeatureTypeInfo featureType,
            FeatureStore featureStore,
            List<org.geoserver.gsr.model.feature.Feature> sourceFeatures,
            boolean exceptionOnFailure)
            throws ServiceException {
        List<EditResult> results = new ArrayList<>();
        for (org.geoserver.gsr.model.feature.Feature sourceFeature : sourceFeatures) {
            results.add(
                    validateResult(
                            updateFeature(featureType, featureStore, sourceFeature),
                            exceptionOnFailure));
        }
        return results;
    }

    /**
     * Delete existing features
     *
     * @see #deleteFeature(FeatureTypeInfo, FeatureStore, Long)
     */
    private static List<EditResult> deleteFeatures(
            FeatureTypeInfo featureType,
            FeatureStore featureStore,
            List<Long> ids,
            boolean exceptionOnFailure)
            throws ServiceException {
        List<EditResult> results = new ArrayList<>();
        for (Long id : ids) {
            results.add(
                    validateResult(
                            deleteFeature(featureType, featureStore, id), exceptionOnFailure));
        }
        return results;
    }

    private static EditResult validateResult(EditResult result, boolean exceptionOnFailure)
            throws ServiceException {
        if (!result.getSuccess() && exceptionOnFailure) {
            throw new ServiceException(result.getError());
        }
        return result;
    }

    /**
     * Create a new feature
     *
     * @param featureType The feature type in GeoServer
     * @param sourceFeature The GSR feature model to add
     * @return the result of the add
     */
    public static EditResult createFeature(
            FeatureTypeInfo featureType,
            FeatureStore featureStore,
            org.geoserver.gsr.model.feature.Feature sourceFeature) {
        try {
            if (sourceFeature == null) {
                return new EditResult(
                        null,
                        false,
                        FeatureServiceErrors.nonSpecific(
                                Collections.singletonList("Error parsing feature")));
            }

            SimpleFeatureType schema = (SimpleFeatureType) featureStore.getSchema();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

            Set<String> attributeNames = sourceFeature.getAttributes().keySet();
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();

            ServiceError validationResult = validateSchema(schema, sourceFeature);
            if (validationResult != null) {
                return new EditResult(null, false, validationResult);
            }

            for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                if (descriptor.equals(geometryDescriptor)) {

                    Geometry geom =
                            transformGeometry(
                                    geometryDescriptor.getCoordinateReferenceSystem(),
                                    sourceFeature.getGeometry().getSpatialReference(),
                                    GeometryEncoder.toJts(sourceFeature.getGeometry()));
                    builder.add(geom);
                } else if (attributeNames.contains(descriptor.getLocalName())) {
                    builder.add(sourceFeature.getAttributes().get(descriptor.getLocalName()));
                } else {
                    builder.add(null);
                }
            }
            SimpleFeature destFeature = builder.buildFeature(null);
            List<FeatureId> fid =
                    featureStore.addFeatures(
                            new ListFeatureCollection(
                                    schema, Collections.singletonList(destFeature)));
            if (fid.size() < 1) {
                return new EditResult(
                        null,
                        false,
                        FeatureServiceErrors.insertError(
                                Collections.singletonList(
                                        "Could not create feature: " + sourceFeature.toString())));
            } else if (fid.size() > 1) {
                return new EditResult(
                        null,
                        false,
                        FeatureServiceErrors.insertError(
                                Collections.singletonList(
                                        "Multiple features created for: "
                                                + sourceFeature.toString())));
            }
            return new EditResult(FeatureEncoder.toGSRObjectId(fid.get(0).getID()));
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Error creating object in "
                            + featureType.getNamespace().getPrefix()
                            + ":"
                            + featureType.getName(),
                    e);
            return new EditResult(
                    null,
                    false,
                    FeatureServiceErrors.nonSpecific(Collections.singletonList(e.getMessage())));
        }
    }

    /**
     * Update an existing feature
     *
     * @param featureType The feature type in GeoServer
     * @param sourceFeature The GSR feature model to add. Must include the id
     * @return the result of the edit
     */
    public static EditResult updateFeature(
            FeatureTypeInfo featureType,
            FeatureStore featureStore,
            org.geoserver.gsr.model.feature.Feature sourceFeature) {
        Long objectId = null;
        try {
            if (sourceFeature == null) {
                return new EditResult(
                        null,
                        false,
                        FeatureServiceErrors.nonSpecific(
                                Collections.singletonList("Error parsing feature")));
            }

            Object objectIdObject =
                    sourceFeature.getAttributes().get(FeatureEncoder.OBJECTID_FIELD_NAME);
            if (objectIdObject == null) {
                return new EditResult(
                        null,
                        false,
                        FeatureServiceErrors.updateError(
                                Collections.singletonList("Missing id field")));
            }
            if (objectIdObject instanceof Long) {
                objectId = (Long) objectIdObject;
            } else {
                objectId = Long.parseLong(objectIdObject.toString());
            }

            Filter idFilter =
                    FILTERS.id(
                            FILTERS.featureId(
                                    FeatureEncoder.toGeotoolsFeatureId(objectId, featureType)));
            SimpleFeatureType schema = (SimpleFeatureType) featureStore.getSchema();

            int featureCount = featureStore.getFeatures(idFilter).size();
            if (featureCount < 1) {
                return new EditResult(objectId, false, FeatureServiceErrors.objectMissing(null));
            } else if (featureCount > 1) {
                return new EditResult(
                        objectId,
                        false,
                        FeatureServiceErrors.updateError(
                                Collections.singletonList(
                                        "Multiple features found for id " + objectId)));
            }

            List<Name> names = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            Set<String> attributeNames = sourceFeature.getAttributes().keySet();

            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();

            ServiceError validationResult = validateSchema(schema, sourceFeature);
            if (validationResult != null) {
                return new EditResult(objectId, false, validationResult);
            }

            for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                if (descriptor.equals(geometryDescriptor)) {
                    names.add(descriptor.getName());
                    Geometry geom =
                            transformGeometry(
                                    geometryDescriptor.getCoordinateReferenceSystem(),
                                    sourceFeature.getGeometry().getSpatialReference(),
                                    GeometryEncoder.toJts(sourceFeature.getGeometry()));
                    values.add(geom);
                } else if (attributeNames.contains(descriptor.getLocalName())) {
                    names.add(descriptor.getName());
                    values.add(sourceFeature.getAttributes().get(descriptor.getLocalName()));
                }
            }

            featureStore.modifyFeatures(
                    names.toArray(new Name[names.size()]), values.toArray(), idFilter);
            return new EditResult(objectId);
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Error updating object "
                            + objectId
                            + " in "
                            + featureType.getNamespace().getPrefix()
                            + ":"
                            + featureType.getName(),
                    e);
            return new EditResult(
                    objectId,
                    false,
                    FeatureServiceErrors.nonSpecific(Collections.singletonList(e.getMessage())));
        }
    }

    /**
     * Delete an existing feature
     *
     * @param featureType The feature type in GeoServer
     * @param objectId The id of the feature to delete
     * @return the result of the delete
     */
    public static EditResult deleteFeature(
            FeatureTypeInfo featureType, FeatureStore featureStore, Long objectId) {
        try {
            Filter idFilter =
                    FILTERS.id(
                            FILTERS.featureId(
                                    FeatureEncoder.toGeotoolsFeatureId(objectId, featureType)));

            int featureCount = featureStore.getFeatures(idFilter).size();
            if (featureCount < 1) {
                return new EditResult(objectId, false, FeatureServiceErrors.objectMissing(null));
            } else if (featureCount > 1) {
                return new EditResult(
                        objectId,
                        false,
                        FeatureServiceErrors.deleteError(
                                Collections.singletonList(
                                        "Multiple features found for id " + objectId)));
            }

            featureStore.removeFeatures(idFilter);
            return new EditResult(objectId);
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Error deleting object "
                            + objectId
                            + " in "
                            + featureType.getNamespace().getPrefix()
                            + ":"
                            + featureType.getName(),
                    e);
            return new EditResult(
                    objectId,
                    false,
                    FeatureServiceErrors.nonSpecific(Collections.singletonList(e.getMessage())));
        }
    }

    private static ServiceError validateSchema(
            SimpleFeatureType schema, org.geoserver.gsr.model.feature.Feature feature) {
        List<String> objectNames = new ArrayList<>(feature.getAttributes().keySet());
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();

        // Verify geometry is consistent
        if (feature.getGeometry() == null && geometryDescriptor != null) {
            return FeatureServiceErrors.nonSpecific(
                    Collections.singletonList("No geometry provided"));
        }
        if (feature.getGeometry() != null && geometryDescriptor == null) {
            return FeatureServiceErrors.geometryNotSet(
                    Collections.singletonList("No geometry descriptor in schema"));
        }
        // ignore objectid
        if (objectNames.contains(FeatureEncoder.OBJECTID_FIELD_NAME)) {
            objectNames.remove(FeatureEncoder.OBJECTID_FIELD_NAME);
        }

        List<String> errors = new ArrayList<>();

        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (!descriptor.equals(geometryDescriptor)) {
                if (objectNames.contains(descriptor.getLocalName())) {
                    objectNames.remove(descriptor.getLocalName());
                } else {
                    errors.add("Missing field: " + descriptor.getLocalName());
                }
            }
        }
        for (String extraField : objectNames) {
            errors.add("Invalid field name: " + extraField);
        }
        if (errors.size() > 0) {
            return FeatureServiceErrors.nonSpecific(errors);
        }
        return null;
    }

    private static Geometry transformGeometry(
            CoordinateReferenceSystem nativeCrs, SpatialReference inSr, Geometry geometry)
            throws FactoryException, TransformException {
        CoordinateReferenceSystem inCrs =
                SpatialReferenceEncoder.coordinateReferenceSystemFromSpatialReference(inSr);
        if (inCrs != null) {
            MathTransform mathTx = CRS.findMathTransform(inCrs, nativeCrs, true);
            return JTS.transform(geometry, mathTx);
        }
        return geometry;
    }

    protected static FeatureStore featureStore(FeatureTypeInfo featureType)
            throws IOException, ServiceException {
        FeatureSource featureSource = featureType.getFeatureSource(null, null);

        if (!(featureSource instanceof FeatureStore)) {
            throw new ServiceException(
                    FeatureServiceErrors.permissionDenied(
                            Collections.singletonList(
                                    "Error creating object in "
                                            + featureType.getNamespace().getPrefix()
                                            + ":"
                                            + featureType.getName())));
        }

        return (FeatureStore) featureSource;
    }

    /**
     * Searches the provided list of layersAndTables for layerId, then returns the result of {@link
     * #getFeatureCollectionForLayer(String, Integer, String, String, String, String, String,
     * String, String, String, String, String, String, Boolean, String, LayerInfo)}
     *
     * <p>If no matching layer is found, throws a {@link NoSuchElementException}
     *
     * @see #getFeatureCollectionForLayer(String, Integer, String, String, String, String, String,
     *     String, String, String, String, String, String, Boolean, String, LayerInfo)
     * @return The features in the layer that match the provided parameters
     * @throws IOException, NoSuchElementException
     */
    public static FeatureCollection<? extends FeatureType, ? extends Feature>
            getFeatureCollectionForLayerWithId(
                    String workspaceName,
                    Integer layerId,
                    String geometryTypeName,
                    String geometryText,
                    String inSRText,
                    String outSRText,
                    String spatialRelText,
                    String objectIdsText,
                    String relatePattern,
                    String time,
                    String text,
                    String maxAllowableOffsets,
                    String whereClause,
                    Boolean returnGeometry,
                    String outFieldsText,
                    LayersAndTables layersAndTables)
                    throws IOException {

        LayerInfo l = null;
        for (LayerOrTable layerOrTable : layersAndTables.layers) {
            if (Objects.equals(layerOrTable.getId(), layerId)) {
                l = layerOrTable.layer;
                break;
            }
        }

        if (l == null) {
            for (LayerOrTable layerOrTable : layersAndTables.tables) {
                if (Objects.equals(layerOrTable.getId(), layerId)) {
                    l = layerOrTable.layer;
                    break;
                }
            }
        }

        if (null == l) {
            throw new NoSuchElementException(
                    "No table or layer in workspace \"" + workspaceName + " for id " + layerId);
        }

        return getFeatureCollectionForLayer(
                workspaceName,
                layerId,
                geometryTypeName,
                geometryText,
                inSRText,
                outSRText,
                spatialRelText,
                objectIdsText,
                relatePattern,
                time,
                text,
                maxAllowableOffsets,
                whereClause,
                returnGeometry,
                outFieldsText,
                l);
    }

    /**
     * Returns a list of features from a single layer, matching the provided criteria
     *
     * @param workspaceName The name of the workspace that contains the layer
     * @param layerId The integer {@link LayerOrTable#getId() id} of the layer within GSR
     * @param geometryTypeName The type of geometry specified by the geometry parameter. Values:
     *     esriGeometryPoint | esriGeometryMultipoint | esriGeometryPolyline | esriGeometryPolygon |
     *     esriGeometryEnvelope
     * @param geometryText A geometry representing a spatial filter to filter the features by
     * @param inSRText The spatial reference of the input geometry. If the inSR is not specified,
     *     the geometry is assumed to be in the spatial reference of the map. TODO Currently
     *     defaults to 4326
     * @param outSRText The spatial reference of the returned geometry (If the returnGeometry
     *     parameter is true). TODO Currently defaults to 4326
     * @param spatialRelText The spatial relationship to be applied on the input geometry while
     *     performing the query. Values: esriSpatialRelIntersects | esriSpatialRelContains |
     *     esriSpatialRelCrosses | esriSpatialRelEnvelopeIntersects | esriSpatialRelIndexIntersects
     *     | esriSpatialRelOverlaps | esriSpatialRelTouches | esriSpatialRelWithin
     * @param objectIdsText A comma-separated list of feature ids used to filter the features
     * @param relatePattern The spatial relate function that can be applied while performing the
     *     query operation. An example for this spatial relate function is "FFFTTT***"
     * @param time The time instant or the time extent to query (In UNIX Epoch time).
     * @param text Text to filter the features by. TODO: Not implemented
     * @param maxAllowableOffsets This option can be used to specify the maxAllowableOffset (In the
     *     units of outSR) to be used for generalizing geometries returned by the query operation.
     *     TODO: Not implemented
     * @param whereClause A where clause used to filter the features, using CQL where syntax. TODO:
     *     Should use SQL-92 instead of CQL
     * @param returnGeometry If true, the result includes the geometry associated with each feature
     *     returned.
     * @param outFieldsText The list of fields to be included in the returned result set. This list
     *     is a comma delimited list of field names.
     * @param l The {@link LayerInfo} that corresponds to the layer
     * @return List of features for the layer, filtered by the provided pararameters.
     * @throws IOException
     */
    public static FeatureCollection<? extends FeatureType, ? extends Feature>
            getFeatureCollectionForLayer(
                    String workspaceName,
                    Integer layerId,
                    String geometryTypeName,
                    String geometryText,
                    String inSRText,
                    String outSRText,
                    String spatialRelText,
                    String objectIdsText,
                    String relatePattern,
                    String time,
                    String text,
                    String maxAllowableOffsets,
                    String whereClause,
                    Boolean returnGeometry,
                    String outFieldsText,
                    LayerInfo l)
                    throws IOException {
        FeatureTypeInfo featureType = (FeatureTypeInfo) l.getResource();
        if (null == featureType) {
            throw new NoSuchElementException(
                    "No table or layer in workspace \"" + workspaceName + " for id " + layerId);
        }

        Filter filter =
                filter(
                        featureType,
                        geometryTypeName,
                        geometryText,
                        inSRText,
                        spatialRelText,
                        objectIdsText,
                        relatePattern,
                        time,
                        text,
                        maxAllowableOffsets,
                        whereClause);

        // TODO update this to match outSR spec
        // "If outSR is not specified, the geometry is returned in the spatial reference of the
        // map."
        final CoordinateReferenceSystem outSR =
                Utils.parseSpatialReference(
                        StringUtils.isNotEmpty(outSRText)
                                ? outSRText
                                : String.valueOf(SpatialReferences.DEFAULT_WKID));

        String[] properties = parseOutFields(outFieldsText);

        FeatureSource<? extends FeatureType, ? extends Feature> source =
                featureType.getFeatureSource(null, null);
        final String[] effectiveProperties =
                adjustProperties(returnGeometry, properties, source.getSchema());

        final Query query;
        if (effectiveProperties == null) {
            query = new Query(featureType.getName(), filter);
        } else {
            query = new Query(featureType.getName(), filter, effectiveProperties);
        }
        query.setCoordinateSystemReproject(outSR);

        return source.getFeatures(query);
    }

    /**
     * Converts a set of GSR query parameters into a {@link Filter}
     *
     * @see #getFeatureCollectionForLayerWithId(String, Integer, String, String, String, String,
     *     String, String, String, String, String, String, String, Boolean, String, LayersAndTables)
     *     for a description of all the parameters
     */
    public static Filter filter(
            FeatureTypeInfo featureType,
            String geometryTypeName,
            String geometryText,
            String inSRText,
            String spatialRelText,
            String objectIdsText,
            String relatePattern,
            String time,
            String text,
            String maxAllowableOffsets,
            String whereClause)
            throws IOException {

        Filter filter = Filter.INCLUDE;

        final String geometryProperty;
        final String temporalProperty;
        final CoordinateReferenceSystem nativeCRS;
        try {
            GeometryDescriptor geometryDescriptor =
                    featureType.getFeatureType().getGeometryDescriptor();
            nativeCRS = geometryDescriptor.getCoordinateReferenceSystem();
            geometryProperty = geometryDescriptor.getName().getLocalPart();
            DimensionInfo timeInfo =
                    featureType.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if (timeInfo == null || !timeInfo.isEnabled()) {
                temporalProperty = null;
            } else {
                temporalProperty = timeInfo.getAttribute();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Unable to determine geometry type for query request");
        }

        // Try to reverse-mask objectIds into featureIds
        String featureIdPrefix = FeatureEncoder.calculateFeatureIdPrefix(featureType);
        Filter objectIdFilter = parseObjectIdFilter(objectIdsText, featureIdPrefix);

        // Query Parameters

        SpatialRelationship spatialRel = null;
        if (StringUtils.isNotEmpty(spatialRelText)) {
            spatialRel = SpatialRelationship.fromRequestString(spatialRelText);
        }

        String inSRCode =
                StringUtils.isNotEmpty(inSRText)
                        ? inSRText
                        : String.valueOf(SpatialReferences.DEFAULT_WKID);
        final CoordinateReferenceSystem inSR = Utils.parseSpatialReference(inSRCode, geometryText);

        if (StringUtils.isNotEmpty(geometryText)) {
            filter =
                    Utils.buildGeometryFilter(
                            geometryTypeName,
                            geometryProperty,
                            geometryText,
                            spatialRel,
                            relatePattern,
                            inSR,
                            nativeCRS);
        }

        if (time != null) {
            filter = FILTERS.and(filter, Utils.parseTemporalFilter(temporalProperty, time));
        }
        if (text != null) {
            throw new UnsupportedOperationException("Text filter not implemented");
        }
        if (maxAllowableOffsets != null) {
            throw new UnsupportedOperationException(
                    "Generalization (via 'maxAllowableOffsets' parameter) not implemented");
        }
        if (whereClause != null) {
            Filter whereFilter;
            try {
                whereFilter = ECQL.toFilter(whereClause);
                whereFilter =
                        (Filter)
                                whereFilter.accept(
                                        new ObjectIdRemappingFilterVisitor(
                                                FeatureEncoder.OBJECTID_FIELD_NAME,
                                                featureIdPrefix),
                                        null);
            } catch (CQLException e) {
                // TODO Ignore for now. Some clients send basic queries that we can't handle right
                // now
                throw new IllegalArgumentException(
                        "'where' parameter must be valid CQL; was " + whereClause, e);
            }
            List<Filter> children = Arrays.asList(filter, whereFilter, objectIdFilter);
            filter = FILTERS.and(children);
        } else {
            filter = FILTERS.and(filter, objectIdFilter);
        }
        filter = SimplifyingFilterVisitor.simplify(filter);

        return filter;
    }

    /**
     * Converts a comma-separated list of field names into an array of field names.
     *
     * @param outFieldsText comma-separated list of field names.
     * @return An array of field names, or null if outFieldsText is empty or equal to "*".
     */
    public static String[] parseOutFields(String outFieldsText) {
        if (StringUtils.isEmpty(outFieldsText)) {
            return null;
        } else if ("*".equals(outFieldsText)) {
            return null;
        } else {
            return outFieldsText.split(",");
        }
    }

    /**
     * Converts a comma-seprated list of feature ids into an id {@link Filter}
     *
     * @param objectIdsText
     * @param prefix Optional prefix to prepend to each id
     * @return
     */
    public static Filter parseObjectIdFilter(String objectIdsText, String prefix) {
        if (null == objectIdsText) {
            return Filter.INCLUDE;
        } else {
            String[] parts = objectIdsText.split(",");
            Set<FeatureId> fids = new HashSet<>();
            for (String part : parts) {
                fids.add(FILTERS.featureId(prefix + part));
            }
            return FILTERS.id(fids);
        }
    }

    /**
     * Converts a comma-seprated list of feature ids into an id {@link Filter}
     *
     * @param objectIdsText
     * @return
     */
    public static Filter parseObjectIdFilter(String objectIdsText) {
        return parseObjectIdFilter(objectIdsText, "");
    }

    /**
     * Constructs an array of property names to return, based on the provided property names. Fixes
     * case to match the case used in the feature schema If addGeometry is true, adds the geometry
     * field.
     *
     * @see #adjustOneProperty(String, FeatureType)
     * @param addGeometry Whether to add the geometry property to the list of properties
     * @param originalProperties provided list of property names
     * @param schema schema with the actual property names
     * @return array of ammended proprty names
     */
    public static String[] adjustProperties(
            boolean addGeometry, String[] originalProperties, FeatureType schema) {
        if (originalProperties == null) {
            return null;
        }

        String[] effectiveProperties =
                new String[originalProperties.length + (addGeometry ? 1 : 0)];
        for (int i = 0; i < originalProperties.length; i++) {
            // todo skip synthetic id for now
            if (!originalProperties[i].equals(FeatureEncoder.OBJECTID_FIELD_NAME)) {
                effectiveProperties[i] = adjustOneProperty(originalProperties[i], schema);
            }
        }
        if (addGeometry) {
            effectiveProperties[effectiveProperties.length - 1] =
                    schema.getGeometryDescriptor().getLocalName();
        }

        return effectiveProperties;
    }

    /**
     * Searches for a matching property name in the provided schema, and returns the actual property
     * name from the schema, with the correct case. Throws an error if the name is ambiguous
     * properties
     *
     * @param name The name of the property to find
     * @param schema The schema containing the property
     * @return The matching property name in the schema
     */
    static String adjustOneProperty(String name, FeatureType schema) {
        List<String> candidates = new ArrayList<>();
        for (PropertyDescriptor d : schema.getDescriptors()) {
            String pname = d.getName().getLocalPart();
            if (pname.equals(name)) {
                return name;
            } else if (pname.equalsIgnoreCase(name)) {
                candidates.add(pname);
            }
        }
        if (candidates.size() == 1) return candidates.get(0);
        if (candidates.size() == 0)
            throw new NoSuchElementException("No property " + name + " in " + schema);
        throw new NoSuchElementException(
                "Ambiguous request: " + name + " corresponds to " + candidates);
    }
}

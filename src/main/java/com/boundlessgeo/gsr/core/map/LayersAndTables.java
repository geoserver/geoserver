/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.map;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.boundlessgeo.gsr.Utils;
import com.boundlessgeo.gsr.core.GSRModel;
import com.boundlessgeo.gsr.core.geometry.SpatialRelationship;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A list of {@link LayerOrTable}, that can be serialized as JSON
 *
 * Also provides a number of static utility methods for interacting with this list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) public class LayersAndTables implements GSRModel {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LayersAndTables.class);

    protected static final FilterFactory2 FILTERS = CommonFactoryFinder.getFilterFactory2();

    public final ArrayList<LayerOrTable> layers;

    public final ArrayList<LayerOrTable> tables;

    private LayersAndTables(ArrayList<LayerOrTable> layers, ArrayList<LayerOrTable> tables) {
        this.layers = layers;
        this.tables = tables;
    }

    /**
     * Look up a single GSR layer (with at least one geometry column) or table.
     *
     * @param catalog       GeoServer Catalog
     * @param workspaceName GeoServer workspace name
     * @param id            Index of Layer (based on sorting by layer name)
     * @return LayerOrTable from workspaceName identified by layerId
     * @throws IOException
     */
    public static LayerOrTable find(Catalog catalog, String workspaceName, Integer id) throws IOException {
        // short list all layers
        List<LayerInfo> layersInWorkspace = new ArrayList<>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.enabled() && l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace()
                .getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        // sort for "consistent" order
        layersInWorkspace.sort(LayerNameComparator.INSTANCE);

        // retrieve indicated layer as LayerOrTable
        if (id < layersInWorkspace.size()) {
            LayerInfo resource = layersInWorkspace.get(id);
            return entry(resource, id);
        }
        return null; // not found
    }

    public static ReferencedEnvelope sphericalMercator(LayerInfo layer, ReferencedEnvelope boundingBox) {
        if (boundingBox == null) {
            return null; // bounds not available
        }
        try {
            CoordinateReferenceSystem lonLat = CRS.decode("EPSG:4326");
            CoordinateReferenceSystem WEB_MERCATOR = CRS.decode("EPSG:3857");
            double minx = Math.max(boundingBox.getMinX(), -180);
            double maxx = Math.min(boundingBox.getMaxX(), 180);
            double miny = Math.max(boundingBox.getMinY(), -85);
            double maxy = Math.min(boundingBox.getMaxY(), 85);
            ReferencedEnvelope sphericalMercatorBoundingBox = new ReferencedEnvelope(minx, maxx, miny, maxy, lonLat);
            sphericalMercatorBoundingBox = sphericalMercatorBoundingBox.transform(WEB_MERCATOR, true);
            return sphericalMercatorBoundingBox;
        } catch (FactoryException factoryException) {
            LOGGER.log(Level.WARNING, "EPSG definition unavailable for transform to EPSG:3857:" + layer,
                factoryException);
        } catch (TransformException transformException) {
            LOGGER.log(Level.WARNING, "EPSG Database unable to transform to Spherical Mercator:" + layer,
                transformException);
        }
        return null; // bounds not available
    }

    /**
     * Create LayerOrTable entry for layer.
     * <p>
     * Will return null, and log a warning if layer could not be represented
     * as LayerOrTable.
     *
     * @param layer
     * @param idCounter
     * @return LayerOrTable, or null if layer could not be represented
     */
    public static LayerOrTable entry(LayerInfo layer, int idCounter) throws IOException {
        ResourceInfo resource = layer.getResource();

        if (resource instanceof CoverageInfo || resource instanceof FeatureTypeInfo) {
            return new LayerOrTable(layer, idCounter);
        }
        return null; // Skipping layer
    }

    /**
     * LayersAndTables lookup for GeoServer workspace.
     *
     * @param catalog
     * @param workspaceName
     * @return GeoServer Layers gathered into GSR layers (with at least one geometry column) or tables.
     */
    public static LayersAndTables find(Catalog catalog, String workspaceName) {
        List<LayerOrTable> layers = new ArrayList<>();
        List<LayerOrTable> tables = new ArrayList<>();
        int idCounter = 0;
        List<LayerInfo> layersInWorkspace = new ArrayList<>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.enabled() && l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace()
                .getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        layersInWorkspace.sort(LayerNameComparator.INSTANCE);
        for (LayerInfo l : layersInWorkspace) {
            try {
                LayerOrTable entry = entry(l, idCounter);
                if (entry != null) {
                    if (entry.geometryType != null) {
                        layers.add(entry);
                    } else {
                        tables.add(entry);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Skipping layer " + l, e);
            }
            idCounter++;
        }
        return new LayersAndTables(new ArrayList<>(layers), new ArrayList<>(tables));
    }

    /**
     * Searches the provided list of layersAndTables for layerId, then returns the result of
     * {@link #getFeatureCollectionForLayer(String, Integer, String, String, String, String, String, String, String, String, String, String, String, Boolean, String, LayerInfo)}
     *
     * If no matching layer is found, throws a {@link NoSuchElementException}
     *
     * @see #getFeatureCollectionForLayer(String, Integer, String, String, String, String, String, String, String, String, String, String, String, Boolean, String, LayerInfo)
     * @return The features in the layer that match the provided parameters
     * @throws IOException, NoSuchElementException
     */
    public static FeatureCollection<? extends FeatureType, ? extends Feature> getFeatureCollectionForLayerWithId(
        String workspaceName, Integer layerId, String geometryTypeName, String geometryText, String inSRText,
        String outSRText, String spatialRelText, String objectIdsText, String relatePattern, String time, String text,
        String maxAllowableOffsets, String whereClause, Boolean returnGeometry, String outFieldsText, LayersAndTables layersAndTables)
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

        return getFeatureCollectionForLayer(workspaceName, layerId, geometryTypeName, geometryText, inSRText, outSRText,
            spatialRelText, objectIdsText, relatePattern, time, text, maxAllowableOffsets, whereClause, returnGeometry,
            outFieldsText, l);
    }

    /**
     * Returns a list of features from a single layer, matching the provided criteria
     *
     * @param workspaceName The name of the workspace that contains the layer
     * @param layerId The integer {@link LayerOrTable#getId() id} of the layer within GSR
     * @param geometryTypeName The type of geometry specified by the geometry parameter. Values:
     *                         esriGeometryPoint | esriGeometryMultipoint | esriGeometryPolyline | esriGeometryPolygon |
     *                         esriGeometryEnvelope
     * @param geometryText A geometry representing a spatial filter to filter the features by
     * @param inSRText The spatial reference of the input geometry. If the inSR is not specified, the geometry is
     *                 assumed to be in the spatial reference of the map. TODO Currently defaults to 4326
     * @param outSRText The spatial reference of the returned geometry (If the returnGeometry parameter is true). TODO Currently defaults to 4326
     * @param spatialRelText The spatial relationship to be applied on the input geometry while performing the query.
     *                       Values: esriSpatialRelIntersects | esriSpatialRelContains | esriSpatialRelCrosses |
     *                       esriSpatialRelEnvelopeIntersects | esriSpatialRelIndexIntersects | esriSpatialRelOverlaps |
     *                       esriSpatialRelTouches | esriSpatialRelWithin
     * @param objectIdsText A comma-separated list of feature ids used to filter the features
     * @param relatePattern The spatial relate function that can be applied while performing the query operation. An
     *                      example for this spatial relate function is "FFFTTT***"
     * @param time The time instant or the time extent to query (In UNIX Epoch time).
     * @param text Text to filter the features by. TODO: Not implemented
     * @param maxAllowableOffsets This option can be used to specify the maxAllowableOffset (In the units of outSR) to
     *                            be used for generalizing geometries returned by the query operation. TODO: Not implemented
     * @param whereClause A where clause used to filter the features, using CQL where syntax. TODO: Should use SQL-92 instead of CQL
     * @param returnGeometry If true, the result includes the geometry associated with each feature returned.
     * @param outFieldsText The list of fields to be included in the returned result set. This list is a comma
     *                      delimited list of field names.
     * @param l The {@link LayerInfo} that corresponds to the layer
     *
     * @return List of features for the layer, filtered by the provided pararameters.
     * @throws IOException
     */
    public static FeatureCollection<? extends FeatureType, ? extends Feature> getFeatureCollectionForLayer(
        String workspaceName, Integer layerId, String geometryTypeName, String geometryText, String inSRText,
        String outSRText, String spatialRelText, String objectIdsText, String relatePattern, String time, String text,
        String maxAllowableOffsets, String whereClause, Boolean returnGeometry, String outFieldsText, LayerInfo l) throws IOException {
        FeatureTypeInfo featureType = (FeatureTypeInfo) l.getResource();
        if (null == featureType) {
            throw new NoSuchElementException(
                "No table or layer in workspace \"" + workspaceName + " for id " + layerId);
        }

        final String geometryProperty;
        final String temporalProperty;
        final CoordinateReferenceSystem nativeCRS;
        try {
            GeometryDescriptor geometryDescriptor = featureType.getFeatureType().getGeometryDescriptor();
            nativeCRS = geometryDescriptor.getCoordinateReferenceSystem();
            geometryProperty = geometryDescriptor.getName().getLocalPart();
            DimensionInfo timeInfo = featureType.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if (timeInfo == null || !timeInfo.isEnabled()) {
                temporalProperty = null;
            } else {
                temporalProperty = timeInfo.getAttribute();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to determine geometry type for query request");
        }

        //Query Parameters
        // TODO update this to match outSR spec
        // "If outSR is not specified, the geometry is returned in the spatial reference of the map."
        final CoordinateReferenceSystem outSR = Utils
            .parseSpatialReference(StringUtils.isNotEmpty(outSRText) ? outSRText : "4326");
        SpatialRelationship spatialRel = null;
        if (StringUtils.isNotEmpty(spatialRelText)) {
            spatialRel = SpatialRelationship.fromRequestString(spatialRelText);
        }
        Filter objectIdFilter = parseObjectIdFilter(objectIdsText);

        String inSRCode = StringUtils.isNotEmpty(inSRText) ? inSRText : "4326";
        final CoordinateReferenceSystem inSR = Utils.parseSpatialReference(inSRCode, geometryText);
        Filter filter = Filter.INCLUDE;

        if (StringUtils.isNotEmpty(geometryText)) {
            filter = Utils
                .buildGeometryFilter(geometryTypeName, geometryProperty, geometryText, spatialRel, relatePattern, inSR,
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
        //TODO: test with QGIS plugin
        if (whereClause != null) {
            Filter whereFilter = Filter.INCLUDE;
            try {
                whereFilter = ECQL.toFilter(whereClause);
            } catch (CQLException e) {
                //TODO Ignore for now. Some clients send basic queries that we can't handle right now
                throw new IllegalArgumentException("'where' parameter must be valid CQL; was " + whereClause, e);
            }
            List<Filter> children = Arrays.asList(filter, whereFilter, objectIdFilter);
            filter = FILTERS.and(children);
        }
        String[] properties = parseOutFields(outFieldsText);

        FeatureSource<? extends FeatureType, ? extends Feature> source = featureType.getFeatureSource(null, null);
        final String[] effectiveProperties = adjustProperties(returnGeometry, properties, source.getSchema());

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

    @Override
    public String toString() {
        return layers.toString() + ";" + tables.toString();
    }

    /**
     * Layer names are just integers IDs in Esri, but not in GeoServer. This method is basically a hack and really ought
     * to be rethought.
     * <p>
     * TODO
     *
     * @param catalog
     * @param layerName
     * @param workspaceName
     * @return
     */
    public static String integerIdToGeoserverLayerName(Catalog catalog, String layerName, String workspaceName) {
        String name = layerName;
        try {
            LayerOrTable layerOrTable = find(catalog, workspaceName, Integer.parseInt(layerName));
            name = layerOrTable.getName();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } catch (NumberFormatException e) {
            //Just use string layer name for now.
        }
        return workspaceName + ":" + name;
    }

    /**
     * Converts a comma-seprated list of feature ids into an id {@link Filter}
     *
     * @param objectIdsText
     * @return
     */
    public static Filter parseObjectIdFilter(String objectIdsText) {
        if (null == objectIdsText) {
            return Filter.INCLUDE;
        } else {
            String[] parts = objectIdsText.split(",");
            Set<FeatureId> fids = new HashSet<>();
            for (String part : parts) {
                fids.add(FILTERS.featureId(part));
            }
            return FILTERS.id(fids);
        }
    }

    /**
     * Constructs an array of property names to return, based on the provided property names.
     * Fixes case to match the case used in the feature schema
     * If addGeometry is true, adds the geometry field.
     *
     * @see #adjustOneProperty(String, FeatureType)
     * @param addGeometry Whether to add the geometry property to the list of properties
     * @param originalProperties provided list of property names
     * @param schema schema with the actual property names
     * @return array of ammended proprty names
     */
    public static String[] adjustProperties(boolean addGeometry, String[] originalProperties, FeatureType schema) {
        if (originalProperties == null) {
            return null;
        }

        String[] effectiveProperties = new String[originalProperties.length + (addGeometry ? 1 : 0)];
        for (int i = 0; i < originalProperties.length; i++) {
            //todo skip synthetic id for now
            if (!originalProperties[i].equals("objectid")) {
                effectiveProperties[i] = adjustOneProperty(originalProperties[i], schema);
            }
        }
        if (addGeometry) {
            effectiveProperties[effectiveProperties.length - 1] = schema.getGeometryDescriptor().getLocalName();
        }

        return effectiveProperties;
    }

    /**
     * Searches for a matching property name in the provided schema, and returns the actual property name from the
     * schema, with the correct case.
     * Throws an error if the name is ambiguous properties
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
        if (candidates.size() == 1)
            return candidates.get(0);
        if (candidates.size() == 0)
            throw new NoSuchElementException("No property " + name + " in " + schema);
        throw new NoSuchElementException("Ambiguous request: " + name + " corresponds to " + candidates);
    }
}

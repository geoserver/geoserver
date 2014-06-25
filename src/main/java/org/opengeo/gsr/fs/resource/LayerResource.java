/* Copyright (c) 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.fs.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import net.sf.json.util.JSONBuilder;

import org.apache.commons.lang.enums.EnumUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
// import org.geotools.feature.FeatureTypes;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.feature.FeatureEncoder;
import org.opengeo.gsr.core.feature.FieldTypeEnum;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.opengeo.gsr.core.geometry.GeometryEncoder;
import org.opengeo.gsr.core.renderer.StyleEncoder;
import org.opengeo.gsr.ms.resource.LayerOrTable;
import org.opengeo.gsr.ms.resource.LayersAndTables;
import org.opengeo.gsr.ms.resource.ScaleRange;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

/**
 * Single feature layer or a non-spatial table in a feature service.
 * <p>
 * Path:
 * <ul>
 * <li>host:port/geoserver/services/'serviceName'/FeatureService/'LayerId'</li>
 * <li>host:port/geoserver/services/workspace/'serviceName'/FeatureService/'LayerId'</li>
 * </li>
 * </p>
 * <p>
 * Parameters:
 * <ul>
 * <li>f: html (default), json, pjson</li>
 * <li>returnUpdates: true for updated time extent. Time to attribute mapping may be available from WMS configuration</li>
 * </p>
 * <p>
 * Available operations:
 * <ul>
 * <li>query</li>
 * <li>queryRelatedRecords</li>
 * <li>addFeatures</li>
 * <li>updateFeatures</li>
 * <li>deleteFeatures</li>
 * <li>applyEdits</li>
 * <li>generateRenderer</li>
 * <ul>
 * </p>
 * 
 * @author Jody Garnett (Boundless)
 */
public class LayerResource extends Resource {
    private final class JsonLayerRepresentation extends OutputRepresentation {
        private final LayerOrTable entry;
        private final LayerInfo layerInfo;
        private final FeatureTypeInfo featureTypeInfo;

        private JsonLayerRepresentation(LayerOrTable entry, LayerInfo layerInfo,
                FeatureTypeInfo featureTypeInfo) {
            super(MediaType.APPLICATION_JAVASCRIPT);
            this.entry = entry;
            this.layerInfo = layerInfo;
            this.featureTypeInfo = featureTypeInfo;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            try {
                JSONBuilder json = new JSONBuilder(writer);	
                FeatureType schema = featureTypeInfo.getFeatureType();
                json.object();

                // listed first for early exit 
                json.key("currentVersion").value(2.24);

                // id
                json.key("id").value(entry.id);
                // type: Feature Layer (if geometry column available) or Table
                if (schema.getGeometryDescriptor() != null) {
                    json.key("type").value("Feature Layer");
                } else {
                    json.key("type").value("Table");
                }
                // displayField - attribute name to use as a display name
                json.key("displayField").value(displayField(schema));

                // description - unsure if we need to encode the description
                json.key("description").value(layerInfo.getAbstract());

                // copyrightText
                json.key("copyrightText").value(copyrightText(layerInfo));

                // defaultVisibility - true or false
                json.key("defaultVisibility").value("true");

                // editFieldsInfo - skipped we are not editable
                // ownershipBasedAccessControlForFeatures - skipped we are not doing ownership
                // syncCanReturnChanges - skipped revision not supported until geogig
                json.key("syncCanReturnChanges").value("false");
                // relationships - only required for complex schema - will check associations
                // isDataVersioned - false unless geogig used
                json.key("isDataVersioned").value("false");

                // supportsRollbackOnFailureParameter - false as we cannot edit at all through GSR
                json.key("supportsRollbackOnFailureParameter").value("false");

                // supportsStatistics - may be able to implement with aggregate functions
                json.key("supportsStatistics").value("false");

                // supportsAdvancedQueries - not implemented yet (no queries at all.) implement using SortBy
                json.key("supportsAdvancedQueries").value("false");

                // for feature layers only
                if (schema.getGeometryDescriptor() != null) {
                    // geometryType

                    json.key("geometryType").value(entry.gtype.getGeometryType());

                    ScaleRange range = ScaleRange.extract(layerInfo.getDefaultStyle().getStyle());
                    json.key("minScale").value(range.minScale);
                    json.key("maxScale").value(range.maxScale);

                    // extent - layer extent (includes srs info)
                    json.key("extent");
                    GeometryEncoder.envelopeToJson(entry.boundingBox, json);

                    // drawingInfo (renderer, transparency, labelingInfo)
                    json.key("drawingInfo");
                    {	json.object();
                        json.key("renderer");
                        StyleEncoder.encodeRenderer(json, entry.renderer);

                        // transparency - not supported
                        json.key("transparency").value(0);
                        // labelingInfo - could read from style
                        json.key("labelingInfo").value(null);
                        json.endObject();
                    }
                    // hasM - unsupported
                    // hasZ - check CRS
                    CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
                    int dimension = crs.getCoordinateSystem().getDimension();
                    json.key("extent").value(dimension > 2 ? "true" : "false");					
                }
                // enableZDefaults - ignore
                // zDefault - ignore
                // allowGeometryUpdates - editing not supported at this time

                // Use time mapping from WMS if available
                DimensionInfo time = (DimensionInfo) layerInfo.getMetadata().get(ResourceInfo.TIME);
                if (time != null) {
                    // Use time mapping from WMS if available
                    json.key("timeInfo");
                    {
                        json.object();

                        // startTimeField
                        json.key("startTimeField").value(time.getAttribute());

                        // endTimeField
                        if (time.getEndAttribute() != null) {
                            json.key("endTimeField").value(
                                    time.getEndAttribute());
                        } else {
                            json.key("endTimeField").value(time.getAttribute());
                        }
                        // timeExtent - dimension range
                        if (time.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL) {
                            // skip for now - requires traverse
                            // See WMS.getFeatureTypeTimes for example

                            // timeReference
                            json.key("timeReference");
                            {
                                json.object();
                                json.key("timeZone").value("UTC");
                                json.key("respectsDaylightSaving").value(true);
                                json.endObject();
                            }

                            BigDecimal resolution = time.getResolution();
                            if (resolution != null) {
                                json.key("timeInterval").value(resolution);
                                json.key("timeIntervalUnits").value(
                                        "milliseconds");
                            }
                        }
                        json.endObject();
                    }
                }
                // hasAttachments - not supported
                json.key("hasAttachments").value("false");

                // htmlPopupType - could consider use of GetFeatureInfo
                json.key("hasAttachments").value("false");

                // objectIdField - placeholder for FeatureId
                json.key("objectIdField").value("objectid");

                // globalIdField - placeholder for FeatureId
                json.key("globalIdField").value("");

                // typeIdField - not applicable

                json.key("fields");
                {
                    json.array();
                    // generated field
                    {
                        json.object();
                        json.key("name").value("objectid");
                        json.key("type").value("");
                        json.key("alias").value("Feature Id");
                        json.key("editable").value("false");
                        json.key("nullable").value("false");
                        json.key("domain").value(null);
                        json.endObject();
                    }
                    // attributes
                    for (PropertyDescriptor field : schema.getDescriptors()) {
                        if (field == schema.getGeometryDescriptor()) {
                            continue; // continue skip default geometry
                        }
                        FeatureEncoder.descriptorToJson(field, json);
                    }
                    json.endArray();
                }
                // types - we do not use sub types
                json.key("types");
                json.array();
                json.endArray();

                // templates - we can list one template based on schema default values
                json.key("templates");
                json.array();
                json.endArray();

                // capabilities - (Create,Delete,Query,Update,Editing)
                json.key("capabilities").value("");

                json.endObject();
            }
            finally {
                writer.close();
                outputStream.close();
            }
        }

        /**
         * Copyright text from attribute title or service access rights.
         * @param layerInfo
         * @return copyright text from layer attribute
         */
        private Object copyrightText(LayerInfo layerInfo) {
            // check metadata
            List<MetadataLinkInfo> links = layerInfo.getResource().getMetadataLinks();
            if (links != null && !links.isEmpty()) {
                return links.get(0).getContent();
            }			
            // check attribution
            if (layerInfo.getAttribution() != null && layerInfo.getAttribution().getTitle() != null) {
                return layerInfo.getAttribution().getTitle();
            }
            return "";
        }

        /**
         * Recommend a suitable display field.
         * <ul>
         * <li>A <b>name</b> field will be used if it is available (as per GeoTools feature model AbstractFeature)</li>
         * <li>A String field ending in <b>id</b> (preferred) or <b>name</b></li>
         * <li>First available String field</li>
         * </ul>
         * 
         * @param schema
         * @return Suitable display field
         */
        private Object displayField(FeatureType schema) {
            PropertyDescriptor property = schema.getDescriptor("name");
            if (property != null && String.class.isAssignableFrom(property.getType().getBinding())) {
                return "name";					
            }
            for (PropertyDescriptor attribute : schema.getDescriptors()){
                String name = attribute.getName().getLocalPart();
                if (name.toLowerCase().endsWith("id") &&
                        String.class.isAssignableFrom(attribute.getType().getBinding())) {
                    return name;					
                }	
            }
            for (PropertyDescriptor attribute : schema.getDescriptors()) {
                String name = attribute.getName().getLocalPart();
                if (name.toLowerCase().endsWith("name") &&
                        String.class.isAssignableFrom(attribute.getType().getBinding())) {
                    return name;
                }
            }
            for (PropertyDescriptor attribute : schema.getDescriptors()){
                if (String.class.isAssignableFrom( attribute.getType().getBinding())) {
                    return attribute.getName();
                }
            }
            return null;
        }
    }

    private final static Variant JSON = new Variant(MediaType.APPLICATION_JSON);

    // catalog integration
    private final Catalog catalog;
    private final String format;
    private final String layerId;


    // data model
    //    public final int id;
    //    public final GeometryTypeEnum gtype;
    //    public final ReferencedEnvelope boundingBox;
    //    public final Renderer renderer;

    //
    // Property capabilities
    //  Query, Create, Delete, Update, Editing
    //	double relationships;
    //	
    //	double effectiveMinScale;
    //	double effectiveMaxScale;
    //	boolean returnUpdates;
    //	boolean nullable;
    //	boolean hasAttachments;
    //
    // Optional properties added over time
    //
    /** Require use of standardized queries */
    //	boolean useStandardizedQueries;

    /** rollbackOnFailure available */
    //	boolean supportsRollbackOnFailures;

    /** Server can return changes (using global id and version) */
    //	boolean syncCanReturnChanges;
    //	boolean isDataVersioned;
    //	boolean supportsStatistics;

    /** Query can use orderBy */
    //	boolean supportsAdvancedQueries;

    /** List of query formats */
    //	List<String> supportedQueryFormats;

    /** Coordinates include z values */
    //	boolean hasZ;

    /** Coordiantes include m measurements */
    //	boolean hasM;

    /** If geometry can be edited */
    //	boolean allowGeometryUpdates;

    public LayerResource(Context context, Request request, Response response, Catalog catalog, String format, String layerId) {
        super(context, request, response);
        this.catalog = catalog;
        this.format = format;
        this.layerId = layerId;
        getVariants().add(JSON);
    }	

    @Override
    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            try {
                return buildJsonRepresentation();
            } catch (IllegalArgumentException e) {
                return buildJsonError(new ServiceError(400, "Invalid arguments from client", Arrays.asList(e.getMessage())));
            }
        }
        return super.getRepresentation(variant);
    }

    private Representation buildJsonRepresentation() {
        if (!"json".equals(format)) {
            throw new IllegalStateException("f=json expected");
        }
        String workspaceName = (String) getRequest().getAttributes().get("workspace");
        Integer layerIndex = Integer.valueOf(layerId);

        LayerOrTable entry;
        try {
            entry = LayersAndTables.find(catalog, workspaceName, layerIndex);
        } catch (IOException e) {			
            throw new NoSuchElementException("Unavailable table or layer in workspace \"" + workspaceName + "\" for id " + layerId + ":" + e); 
        }
        if (entry == null) {
            throw new NoSuchElementException("No table or layer in workspace \"" + workspaceName + "\" for id " + layerId);
        }

        final LayerInfo layerInfo = entry.layer;
        final FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layerInfo.getResource();

        return new JsonLayerRepresentation(entry, layerInfo,featureTypeInfo);
    }

    private Representation buildJsonError(ServiceError error) {
        getResponse().setStatus(new Status(error.getCode()));
        GeoServicesJsonFormat format = new GeoServicesJsonFormat();
        return format.toRepresentation(error);
    }

    @Override
    public String toString() {
        return String.valueOf(layerId);
    }
}

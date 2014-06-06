/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package org.opengeo.gsr.ms.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.util.JSONBuilder;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.feature.FieldTypeEnum;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.opengeo.gsr.core.geometry.GeometryEncoder;
import org.opengeo.gsr.core.geometry.GeometryTypeEnum;
import org.opengeo.gsr.core.geometry.SpatialReferences;
import org.opengeo.gsr.core.renderer.Renderer;
import org.opengeo.gsr.core.renderer.StyleEncoder;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.vividsolutions.jts.geom.Geometry;

public class LayerListResource extends Resource {
    private static class ScaleRange {
        public final Double minScale;
        public final Double maxScale;

        public ScaleRange(Double minScale, Double maxScale) {
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
    }

    public static final Variant JSON = new Variant(new MediaType("application/json"));
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LayerListResource.class);
    public LayerListResource(Context context, Request request, Response response, Catalog catalog, String format) {
        super(context, request, response);
        this.catalog = catalog;
        this.format = format;
        getVariants().add(JSON);
    }
    
    private final Catalog catalog;
    private final String format;

    @Override
    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            try {
                return buildJsonRepresentation();
            } catch (IllegalArgumentException e) {
                return buildJsonError(new ServiceError(400, "Invalid arguments from client", Arrays.asList(e.getMessage())));
            } catch (UnsupportedOperationException e) {
                return buildJsonError(new ServiceError(500, "Requested operation is not implemented", Arrays.asList(e.getMessage())));
            } catch (NoSuchElementException e) {
                return buildJsonError(new ServiceError(404, "Requested element not found", Arrays.asList(e.getMessage())));
            }
        }
        return super.getRepresentation(variant);
    }
    
    private Representation buildJsonError(ServiceError error) {
        getResponse().setStatus(new Status(error.getCode()));
        
        GeoServicesJsonFormat format = new GeoServicesJsonFormat();
        return format.toRepresentation(error);
    }

    private Representation buildJsonRepresentation() {
        if (!"json".equals(format)) throw new IllegalArgumentException("json is the only supported format");
        String workspaceName = (String) getRequest().getAttributes().get("workspace");
        WorkspaceInfo workspace = catalog.getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("No workspace known by name: " + workspaceName);
        }
        LayersAndTables layersAndTables = LayersAndTables.find(catalog, workspaceName);
        return new JsonLayersRepresentation(layersAndTables.layers, layersAndTables.tables);
    }

    private static class JsonLayersRepresentation extends OutputRepresentation {
        private final List<LayerOrTable> layers;
        private final List<LayerOrTable> tables;
        
        public JsonLayersRepresentation(List<LayerOrTable> layers, List<LayerOrTable> tables) {
            super(MediaType.APPLICATION_JAVASCRIPT);
            this.layers = layers;
            this.tables = tables;
        }
        
        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            json.object();
            json.key("layers");
            encodeLayersOrTables(layers, json);
            json.key("tables");
            encodeLayersOrTables(tables, json);
            json.endObject();
            writer.flush();
            writer.close();
        }
    }
    
    private static void encodeLayersOrTables(List<LayerOrTable> layers, JSONBuilder json) throws IOException {
        json.array();
        for (int i = 0; i < layers.size(); i++) {
            final LayerOrTable layerOrTable = layers.get(i);
            final LayerInfo layer = layerOrTable.layer;
            if (!layer.isAdvertised()) {
                continue;
            }
            json.object();
            json.key("id").value(layerOrTable.id);
            json.key("type").value("Feature Layer");
            json.key("name").value(layer.getName());
            json.key("currentVersion").value(2.24);
            json.key("defaultVisibility").value(false);
            json.key("definitionExpression").value("");
            final String description = layer.getResource().getAbstract();
            json.key("description").value(description != null ? description : "");
            json.key("displayField").value(""); // TODO: Extract displayField from the Style

            final String copyrightText;
            List<MetadataLinkInfo> metadataLinks = layer.getResource().getMetadataLinks();
            if (metadataLinks != null && metadataLinks.size() > 0) {
                copyrightText = metadataLinks.get(0).getContent();
            } else {
                copyrightText = "";
            }
            json.key("copyrightText").value(copyrightText);
            json.key("relationships").array().endArray();
            json.key("parentLayer").value(null);
            json.key("subLayers").array().endArray();
            json.key("typeIdField").value(null);
            json.key("types").value(null);
            if (layerOrTable.gtype != null) {
                json.key("geometryType").value(layerOrTable.gtype.getGeometryType());
                ScaleRange range = extractScaleRange(layerOrTable.layer.getDefaultStyle().getStyle());
                Double minScale = Double.isInfinite(range.minScale) ? null : range.minScale;
                Double maxScale = Double.isInfinite(range.maxScale) ? null : range.maxScale;
                json.key("minScale").value(minScale);
                json.key("maxScale").value(maxScale);
                if (layerOrTable.boundingBox != null) {
                    json.key("extent");
                    try {
                        CoordinateReferenceSystem WEB_MERCATOR = CRS.decode("EPSG:3857");
                        GeometryEncoder.referencedEnvelopeToJson(layerOrTable.boundingBox, SpatialReferences.fromCRS(WEB_MERCATOR), json);
                    } catch (FactoryException e) {
                        LOGGER.log(Level.WARNING, "Omitting bbox because we couldn't find EPSG:3857", e);
                    }
                }
                json.key("drawingInfo");
                json.object().key("renderer");
                StyleEncoder.encodeRenderer(json, layerOrTable.renderer);
                json.endObject();
            }
            DimensionInfo time = (DimensionInfo) layer.getResource().getMetadata().get(ResourceInfo.TIME);
            if (time != null) {
                json.key("timeInfo");
                json.object();
                json.key("startTimeField").value(time.getAttribute());
                json.key("endTimeField").value(time.getEndAttribute());
                json.key("trackIdField").value(null);
//                json.key("timeExtent").value(...); // TODO: This should share some code with the Map resource
                json.key("timeReference").object();
                json.endObject();
                if (time.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL) {
                    json.key("timeInterval").value(time.getResolution());
                    json.key("timeIntervalUnits").value("ms");
                }
                json.endObject();
            }
            json.key("hasAttachments").value(false);
            json.key("htmlPopupType").value("esriServerHTMLPopupTypeNone");
            if (layer.getResource() instanceof FeatureTypeInfo) {
                try {
                    FeatureType ftype = ((FeatureTypeInfo) layer.getResource()).getFeatureType();
                    json.key("fields");
                    encodeSchemaProperties(json, ftype);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Omitting fields for layer " + layer + " because we were unable to connect to the underlying resource.", e);
                }
            }
            json.key("capabilities").value("Query,Time,Data");
            json.endObject();
        }
        json.endArray();
    }
    
    private static ScaleRange extractScaleRange(Style style) {
        Double minScale = null, maxScale = null;
        for (FeatureTypeStyle ft : style.featureTypeStyles()) {
            for (Rule r : ft.rules()) {
                double minS = r.getMinScaleDenominator();
                double maxS = r.getMaxScaleDenominator();
                if (minScale == null || minS > minScale) {
                    minScale = minS;
                }
                if (maxScale == null || maxS < maxScale) {
                    maxScale = maxS;
                }
            }
        }
        return new ScaleRange(minScale, maxScale);
    }

    private static void encodeSchemaProperties(JSONBuilder json, FeatureType ftype) {
        json.array();
        for (PropertyDescriptor desc : ftype.getDescriptors()) {
            try {
                if (!Geometry.class.isAssignableFrom(desc.getType().getBinding())) {
                    String name = desc.getName().getLocalPart();
                    String type = FieldTypeEnum.forClass(desc.getType().getBinding()).getFieldType();

                    json.object();
                    json.key("name").value(name);
                    json.key("type").value(type);
                    json.key("editable").value(false);
                    if (String.class.equals(desc.getType().getBinding())) {
                        json.key("length").value(4000);
                    }
                    json.endObject();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Omitting fields for PropertyDescriptor: " + desc, e);
                continue;
            }
        }
        json.endArray();
    }
}

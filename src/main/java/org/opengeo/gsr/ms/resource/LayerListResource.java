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
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.feature.FieldTypeEnum;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.opengeo.gsr.core.geometry.GeometryEncoder;
import org.opengeo.gsr.core.geometry.GeometryTypeEnum;
import org.opengeo.gsr.core.geometry.SpatialReferences;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

public class LayerListResource extends Resource {
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
        List<LayerOrTable> layersInWorkspace = new ArrayList<LayerOrTable>();
        List<LayerOrTable> tablesInWorkspace = new ArrayList<LayerOrTable>();
        int idCounter = 0;
        for (FeatureTypeInfo featureType : catalog.getResourcesByNamespace(workspaceName, FeatureTypeInfo.class)) {
            List<LayerInfo> layersForType = catalog.getLayers(featureType);
            try {
                final GeometryTypeEnum gtype;
                GeometryDescriptor gDesc = featureType.getFeatureType().getGeometryDescriptor();
                if (gDesc == null) {
                    gtype = null;
                } else { 
                    gtype = GeometryTypeEnum.forJTSClass(gDesc.getType().getBinding());
                }
                for (LayerInfo layer : layersForType) {
                    if (gtype == null) {
                        tablesInWorkspace.add(new LayerOrTable(layer, idCounter, gtype));
                    } else {
                        layersInWorkspace.add(new LayerOrTable(layer, idCounter, gtype));
                    }
                    idCounter++;
                }
            } catch (IOException e) {
                idCounter += layersForType.size();
            }
        }
        return new JsonLayersRepresentation(Collections.unmodifiableList(layersInWorkspace), Collections.unmodifiableList(tablesInWorkspace));
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
    
    private static class LayerOrTable {
        public final LayerInfo layer;
        public final int id;
        public final GeometryTypeEnum gtype;

        LayerOrTable(LayerInfo layer, int id, GeometryTypeEnum gtype) {
            this.layer = layer;
            this.id = id;
            this.gtype = gtype;
        }
    }
    
    private static void encodeLayersOrTables(List<LayerOrTable> layers, JSONBuilder json) {
        json.array();
        for (int i = 0; i < layers.size(); i++) {
            final LayerOrTable layerOrTable = layers.get(i);
            final LayerInfo layer = layerOrTable.layer;
            json.object();
            json.key("id").value(layerOrTable.id);
            json.key("type").value("Feature Layer");
            json.key("name").value(layer.getName());
            json.key("description").value(layer.getResource().getAbstract());
            json.key("copyrightText").value(null);
            json.key("relationships").array().endArray();
            if (layerOrTable.gtype != null) {
                json.key("geometryType").value(layerOrTable.gtype.getGeometryType());
                json.key("minScale").value(null);
                json.key("maxScale").value(null);
                try {
                    ReferencedEnvelope boundingBox = layer.getResource().getLatLonBoundingBox();
                    if (boundingBox != null) {
                        json.key("extent");
                        GeometryEncoder.referencedEnvelopeToJson(boundingBox, SpatialReferences.fromCRS(boundingBox.getCoordinateReferenceSystem()), json);
                    }
                } catch (FactoryException e) {
                    LOGGER.log(Level.WARNING, "Unable to convert CRS to SpatialReference for layer " + layer, e);
                }
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
            json.key("htmlPopupType").value("ServerHTMLPopupTypeNone");
            try {
                FeatureType ftype = ((FeatureTypeInfo) layer.getResource()).getFeatureType();
                json.key("fields");
                encodeSchemaProperties(json, ftype);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Omitting fields for layer " + layer + " because we were unable to connect to the underlying resource.", e);
            }
            json.key("capabilities").value("Query,Time");
            json.endObject();
        }
        json.endArray();
    }
    
    private static void encodeSchemaProperties(JSONBuilder json, FeatureType ftype) {
        json.array();
        for (PropertyDescriptor desc : ftype.getDescriptors()) {
            json.object();
            json.key("name").value(desc.getName().getLocalPart());
            json.key("type").value(FieldTypeEnum.forClass(desc.getType().getBinding()));
            json.key("editable").value(false);
            json.endObject();
        }
        json.endArray();
    }
}

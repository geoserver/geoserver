/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package com.boundlessgeo.gsr.model.map;

import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import com.boundlessgeo.gsr.model.feature.Field;
import com.boundlessgeo.gsr.model.feature.FieldTypeEnum;
import com.boundlessgeo.gsr.model.geometry.Envelope;
import com.boundlessgeo.gsr.model.geometry.GeometryTypeEnum;
import com.boundlessgeo.gsr.model.renderer.DrawingInfo;
import com.boundlessgeo.gsr.model.renderer.Renderer;
import com.boundlessgeo.gsr.model.GSRModel;
import com.boundlessgeo.gsr.translate.renderer.StyleEncoder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.Geometry;
import org.geoserver.catalog.*;
import org.geotools.data.FeatureStore;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.boundlessgeo.gsr.GSRConfig.CURRENT_VERSION;

/**
 * Abstract layer model used by both {@link com.boundlessgeo.gsr.model.feature.FeatureLayer} and {@link LayerOrTable}
 */
public abstract class AbstractLayerOrTable  implements GSRModel {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(AbstractLayerOrTable.class);

    @JsonIgnore
    public final LayerInfo layer;

    protected Double currentVersion = CURRENT_VERSION;
    protected Integer id;
    protected String type = "Feature Layer";
    protected String name;

    protected Boolean defaultVisibility = false;
    protected String definitionExpression = "";
    protected String description;
    protected String displayField;

    protected String copyrightText;
    protected List relationships = new ArrayList();
    protected String parentLayer = null;
    protected List subLayers = new ArrayList();
    // typeIdField - not applicable
    protected String typeIdField = null;
    // types - we do not use sub types
    protected List types = new ArrayList();

    protected GeometryTypeEnum geometryType;

    protected Double minScale;
    protected Double maxScale;

    protected Envelope extent;
    protected DrawingInfo drawingInfo;
    protected TimeInfo timeInfo;

    protected Boolean hasM = false; //unsupported
    protected Boolean hasZ = false;

    private Boolean hasAttachments = false;
    private String htmlPopupType = "esriServerHTMLPopupTypeNone";

    private List<Field> fields = new ArrayList<>();

    private String capabilities = "Query,Time,Data";

    public AbstractLayerOrTable(LayerInfo layer, int id) throws IOException {
        this(layer, id, new Envelope(layer.getResource().getLatLonBoundingBox()), StyleEncoder.effectiveRenderer(layer));
    }

    protected AbstractLayerOrTable(AbstractLayerOrTable layerOrTable) throws IOException {
        this(layerOrTable.layer, layerOrTable.getId(), layerOrTable.getExtent(),
                (layerOrTable.getDrawingInfo() == null ? null : layerOrTable.getDrawingInfo().renderer));
    }


    AbstractLayerOrTable(LayerInfo layer, int id, Envelope extent, Renderer renderer) throws IOException {
        this.layer = layer;
        this.id = id;
        this.name = layer.getName();
        this.description = layer.getAbstract() == null ? "" : layer.getAbstract();

        List<MetadataLinkInfo> metadataLinks = layer.getResource().getMetadataLinks();
        copyrightText = copyrightText(layer);

        ScaleRange range = ScaleRange.extract(layer.getDefaultStyle().getStyle());
        minScale = range.minScale;
        maxScale = range.maxScale;

        this.geometryType = geometryDescriptor(layer);
        this.extent = extent;
        if (renderer == null) {
            //tables should not have a drawingInfo field
            this.drawingInfo = null;
        } else {
            this.drawingInfo = new DrawingInfo(renderer);
        }

        DimensionInfo timeDimensionInfo = (DimensionInfo) layer.getResource().getMetadata().get(ResourceInfo.TIME);
        this.timeInfo = timeDimensionInfo == null ? null : new TimeInfo(timeDimensionInfo);

        if (layer.getResource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layer.getResource();

            try {
                // generated field
                FeatureType schema = featureTypeInfo.getFeatureType();
                boolean editable = featureTypeInfo.getFeatureSource(null, null) instanceof FeatureStore;
                fields.add(new Field(FeatureEncoder.OBJECTID_FIELD_NAME, FieldTypeEnum.OID, "Feature Id", 4000, false, false));


                for (PropertyDescriptor desc : schema.getDescriptors()) {
                    try {
                        if (!Geometry.class.isAssignableFrom(desc.getType().getBinding())) {
                            fields.add(FeatureEncoder.field(desc, editable));
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Omitting fields for PropertyDescriptor: " + desc, e);
                    }
                }

                // TODO: Extract displayField from the Style?
                displayField = displayField(schema);

                if (schema.getGeometryDescriptor() != null) {
                    // hasZ - check CRS
                    CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
                    int dimension = crs.getCoordinateSystem().getDimension();
                    hasZ = dimension > 2;
                } else {
                    type = "Table";
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Omitting fields for layer " + layer + " because we were unable to connect to the underlying controller.", e);
            }
        }
    }

    /**
     * Geometry type from layer controller
     * @param layer
     * @return
     */
    protected static GeometryTypeEnum geometryDescriptor(LayerInfo layer) throws IOException {
        ResourceInfo resource = layer.getResource();
        if (resource instanceof CoverageInfo) {
            return GeometryTypeEnum.POLYGON;
        } else if (resource instanceof FeatureTypeInfo) {
            final GeometryTypeEnum gtype;
            GeometryDescriptor gDesc = ((FeatureTypeInfo)resource).getFeatureType().getGeometryDescriptor();

            if (gDesc == null) {
                gtype = null;
            } else {
                gtype = GeometryTypeEnum.forJTSClass(gDesc.getType().getBinding());
            }

            return gtype;
        } else {
            throw new IllegalArgumentException("Layer controller not a valid type: " + resource.getClass());
        }
    }

    /**
     * Copyright text from attribute title or service access rights.
     *
     * @param layerInfo
     * @return copyright text from layer attribute
     */
    protected static String copyrightText(LayerInfo layerInfo) {
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
    protected static String displayField(FeatureType schema) {
        PropertyDescriptor property = schema.getDescriptor("name");
        if (property != null && String.class.isAssignableFrom(property.getType().getBinding())) {
            return "name";
        }
        for (PropertyDescriptor attribute : schema.getDescriptors()) {
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
        for (PropertyDescriptor attribute : schema.getDescriptors()) {
            if (String.class.isAssignableFrom(attribute.getType().getBinding())) {
                return attribute.getName().getLocalPart();
            }
        }
        return null;
    }

    public Double getCurrentVersion() {
        return currentVersion;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Boolean getDefaultVisibility() {
        return defaultVisibility;
    }

    public String getDefinitionExpression() {
        return definitionExpression;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayField() {
        return displayField;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public List getRelationships() {
        return relationships;
    }

    public String getParentLayer() {
        return parentLayer;
    }

    public List getSubLayers() {
        return subLayers;
    }

    public String getTypeIdField() {
        return typeIdField;
    }

    public List getTypes() {
        return types;
    }

    public GeometryTypeEnum getGeometryType() {
        return geometryType;
    }

    public Double getMinScale() {
        return minScale;
    }

    public Double getMaxScale() {
        return maxScale;
    }

    public Envelope getExtent() {
        return extent;
    }

    public DrawingInfo getDrawingInfo() {
        return drawingInfo;
    }

    public TimeInfo getTimeInfo() {
        return timeInfo;
    }

    public Boolean getHasAttachments() {
        return hasAttachments;
    }

    public String getHtmlPopupType() {
        return htmlPopupType;
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public Boolean getHasM() {
        return hasM;
    }

    public Boolean getHasZ() {
        return hasZ;
    }
}

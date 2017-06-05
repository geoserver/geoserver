/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package com.boundlessgeo.gsr.core.map;

import com.boundlessgeo.gsr.core.feature.FieldTypeEnum;
import com.boundlessgeo.gsr.core.geometry.GeometryTypeEnum;
import com.boundlessgeo.gsr.core.renderer.Renderer;
import com.boundlessgeo.gsr.core.GSRModel;
import com.vividsolutions.jts.geom.Geometry;
import org.geoserver.catalog.*;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.boundlessgeo.gsr.GSRConfig.CURRENT_VERSION;

/**
 * A layer or table, used in the {@link LayersAndTables} listing.
 */
public class LayerOrTable  implements GSRModel {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LayerOrTable.class);

    public final LayerInfo layer;
    public final int id;
    public final String type = "Feature Layer";
    public final String name;
    public final double currentVersion = CURRENT_VERSION;
    public final boolean defaultVisibility = false;
    public final String definitionExpression = "";
    public String description;
    public final String displayField = ""; // TODO: Extract displayField from the Style

    final String copyrightText;
    public List relationships = new ArrayList();
    public String parentLayer = null;
    public List subLayers = new ArrayList();
    public String typeIdField = null;
    public String types = null;

    public final GeometryTypeEnum geometryType;

    public Double minScale;
    public Double maxScale;

    public final ReferencedEnvelope extent;
    public final DrawingInfo drawingInfo;
    public TimeInfo timeInfo;

    Boolean hasAttachments = false;
    String htmlPopupType = "esriServerHTMLPopupTypeNone";

    List<SchemaProperty> fields = new ArrayList<SchemaProperty>();

    String capabilities = "Query,Time,Data";

    LayerOrTable(LayerInfo layer, int id, GeometryTypeEnum gtype, ReferencedEnvelope boundingBox, Renderer renderer) throws IOException {
        this.layer = layer;
        this.id = id;
        this.name = layer.getName();
        this.description = layer.getAbstract() == null ? "" : layer.getAbstract();

        List<MetadataLinkInfo> metadataLinks = layer.getResource().getMetadataLinks();
        if (metadataLinks != null && metadataLinks.size() > 0) {
            copyrightText = metadataLinks.get(0).getContent();
        } else {
            copyrightText = "";
        }

        this.geometryType = gtype;

        ScaleRange range = ScaleRange.extract(layer.getDefaultStyle().getStyle());
        minScale = range.minScale;
        maxScale = range.maxScale;


        this.extent = boundingBox;
        this.drawingInfo = new DrawingInfo(renderer);
        this.timeInfo = new TimeInfo((DimensionInfo) layer.getResource().getMetadata().get(ResourceInfo.TIME));

        if (layer.getResource() instanceof FeatureTypeInfo) {
            try {
                FeatureType ftype = ((FeatureTypeInfo) layer.getResource()).getFeatureType();
                for (PropertyDescriptor desc : ftype.getDescriptors()) {
                    try {
                        if (!Geometry.class.isAssignableFrom(desc.getType().getBinding())) {
                            fields.add(new SchemaProperty(desc));
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Omitting fields for PropertyDescriptor: " + desc, e);
                        continue;
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Omitting fields for layer " + layer + " because we were unable to connect to the underlying resource.", e);
            }
        }
    }

    public static class DrawingInfo {
        public Renderer renderer;
        DrawingInfo(Renderer renderer) {
            this.renderer = renderer;
        }
    }

    private static class TimeInfo {

        String startTimeField;
        String endTimeField;
        Object trackIdField = new Object();

        BigDecimal timeInterval;
        String timeIntervalUnits;


        public TimeInfo(DimensionInfo time) {
            startTimeField = time.getAttribute();
            endTimeField = time.getEndAttribute();

            if (time.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL) {
                timeInterval = time.getResolution();
                timeIntervalUnits = "ms";
            }
        }
    }

    private static class SchemaProperty {
        String name;
        String type;
        Boolean editable = false;
        Integer length;

        SchemaProperty(PropertyDescriptor desc) {
            name = desc.getName().getLocalPart();
            type = FieldTypeEnum.forClass(desc.getType().getBinding()).getFieldType();

            if (String.class.equals(desc.getType().getBinding())) {
                length = 4000;
            }
        }
    }
}

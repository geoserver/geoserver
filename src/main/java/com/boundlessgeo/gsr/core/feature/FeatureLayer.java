package com.boundlessgeo.gsr.core.feature;

import com.boundlessgeo.gsr.core.GSRModel;
import com.boundlessgeo.gsr.core.geometry.Envelope;
import com.boundlessgeo.gsr.core.geometry.GeometryTypeEnum;
import com.boundlessgeo.gsr.core.map.LayerOrTable;
import com.boundlessgeo.gsr.core.map.ScaleRange;
import com.boundlessgeo.gsr.core.map.TimeInfo;
import com.boundlessgeo.gsr.core.renderer.DrawingInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.boundlessgeo.gsr.GSRConfig.CURRENT_VERSION;

/**
 * Layer model for features
 *
 * TODO: Consolidate with LayerOrTable (Abstract base class?)
 */
public class FeatureLayer implements GSRModel {

    public final Double currentVersion = CURRENT_VERSION;
    public final Integer id;
    public final String type;
    public final String displayField;
    public final String description;
    public final String copyrightText;
    public final Boolean defaultVisibility = true;
    // editFieldsInfo - skipped we are not editable
    // ownershipBasedAccessControlForFeatures - skipped we are not doing ownership
    // syncCanReturnChanges - skipped revision not supported until geogig
    public final Boolean syncCanReturnChanges = false;
    // supportsRollbackOnFailureParameter - false as we cannot edit at all through GSR
    public final Boolean supportsRollbackOnFailureParameter = false;
    // supportsStatistics - may be able to implement with aggregate functions
    public final Boolean supportsStatistics = false;
    // supportsAdvancedQueries - not implemented yet (no queries at all.) implement using SortBy
    public final Boolean supportsAdvancedQueries = false;

    public final GeometryTypeEnum geometryType;
    public final Double minScale;
    public final Double maxScale;

    public final Envelope extent;

    public final DrawingInfo drawingInfo;

    // hasM - unsupported
    public final Boolean hasZ;

    // enableZDefaults - ignore
    // zDefault - ignore
    // allowGeometryUpdates - editing not supported at this time

    public final TimeInfo timeInfo;


    // hasAttachments - not supported
    public final Boolean hasAttachments = false;

    // htmlPopupType - could consider use of GetFeatureInfo

    // objectIdField - placeholder for FeatureId
    public final String objectIdField = "objectid";

    // globalIdField - placeholder for FeatureId
    public final String globalIdField = "";

    // typeIdField - not applicable

    public final List<FeatureEncoder.Descriptor> fields;
    // types - we do not use sub types
    public final List types = new ArrayList();
    // templates - we can list one template based on schema default values
    public final List templates = new ArrayList();

    // capabilities - (Create,Delete,Query,Update,Editing)
    public final String capabilities = "Query,Time,Data";


    public FeatureLayer(LayerOrTable entry, LayerInfo layerInfo,
                        FeatureTypeInfo featureTypeInfo) throws IOException {
        // id
        id = entry.id;

        FeatureType schema = featureTypeInfo.getFeatureType();
        // type: Feature Layer (if geometry column available) or Table
        if (schema.getGeometryDescriptor() != null) {
            type = "Feature Layer";
        } else {
            type = "Table";
        }
        // displayField - attribute name to use as a display name
        displayField = displayField(schema);

        // description - unsure if we need to encode the description
        description = layerInfo.getAbstract();

        // copyrightText
        copyrightText = copyrightText(layerInfo);

        // for feature layers only
        if (schema.getGeometryDescriptor() != null) {
            // geometryType
            geometryType = entry.geometryType;

            ScaleRange range = ScaleRange.extract(layerInfo.getDefaultStyle().getStyle());
            minScale = range.minScale;
            maxScale = range.maxScale;

            // extent - layer extent (includes srs info)
            this.extent = entry.extent;

            // drawingInfo (renderer, transparency, labelingInfo)
            drawingInfo = entry.drawingInfo;

            // hasZ - check CRS
            CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
            int dimension = crs.getCoordinateSystem().getDimension();
            hasZ = dimension > 2;
        } else {
            geometryType = null;
            minScale = null;
            maxScale = null;
            this.extent = null;
            drawingInfo = null;
            hasZ = null;
        }

        // Use time mapping from WMS if available
        timeInfo = entry.timeInfo;
        fields = entry.fields;
    }

    /**
     * Copyright text from attribute title or service access rights.
     *
     * @param layerInfo
     * @return copyright text from layer attribute
     */
    private String copyrightText(LayerInfo layerInfo) {
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
    private String displayField(FeatureType schema) {
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
}

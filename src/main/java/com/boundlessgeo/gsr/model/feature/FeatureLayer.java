package com.boundlessgeo.gsr.model.feature;

import com.boundlessgeo.gsr.model.map.AbstractLayerOrTable;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import com.vividsolutions.jts.geom.Geometry;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geotools.data.FeatureStore;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Layer model for features
 */
public class FeatureLayer extends AbstractLayerOrTable {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(FeatureLayer.class);

    // editFieldsInfo - skipped we are not editable
    // ownershipBasedAccessControlForFeatures - skipped we are not doing ownership
    // syncCanReturnChanges - skipped revision not supported until geogig
    protected Boolean syncCanReturnChanges = false;
    // supportsRollbackOnFailureParameter - supported
    protected Boolean supportsRollbackOnFailureParameter = true;
    // supportsStatistics - may be able to implement with aggregate functions
    protected Boolean supportsStatistics = false;
    // supportsAdvancedQueries - not implemented yet (no queries at all.) implement using SortBy
    protected Boolean supportsAdvancedQueries = false;
    // supportsCoordinatesQuantization - Supported. See QuantizedGeometryEncoder
    protected Boolean supportsCoordinatesQuantization = true;



    // enableZDefaults - ignore
    // zDefault - ignore
    // allowGeometryUpdates - editing supported
    protected Boolean allowGeometryUpdates = true;

    // htmlPopupType - could consider use of GetFeatureInfo

    // objectIdField - placeholder for FeatureId
    public String objectIdField = FeatureEncoder.OBJECTID_FIELD_NAME;

    // globalIdField - placeholder for FeatureId
    protected String globalIdField = "";

    protected Field geometryField = null;

    // templates - we can list one template based on schema default values
    protected List templates = new ArrayList();

    public FeatureLayer(AbstractLayerOrTable entry) throws IOException {
        super(entry);
        defaultVisibility = true;

        if (layer.getResource() instanceof FeatureTypeInfo) {
            FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layer.getResource();
            try {
                FeatureType schema = featureTypeInfo.getFeatureType();
                if (schema.getGeometryDescriptor() != null) {
                    boolean editable = featureTypeInfo.getFeatureSource(null, null) instanceof FeatureStore;
                    geometryField = FeatureEncoder.field(schema.getGeometryDescriptor(), editable);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Omitting geometryField for layer " + layer + " because we were unable to connect to the underlying controller.", e);
            }
        }
    }

    public Boolean getSyncCanReturnChanges() {
        return syncCanReturnChanges;
    }

    public Boolean getSupportsRollbackOnFailureParameter() {
        return supportsRollbackOnFailureParameter;
    }

    public Boolean getSupportsStatistics() {
        return supportsStatistics;
    }

    public Boolean getSupportsAdvancedQueries() {
        return supportsAdvancedQueries;
    }

    public String getObjectIdField() {
        return objectIdField;
    }

    public String getGlobalIdField() {
        return globalIdField;
    }

    public List getTemplates() {
        return templates;
    }

    public Boolean getSupportsCoordinatesQuantization() {
        return supportsCoordinatesQuantization;
    }

    public Boolean getAllowGeometryUpdates() {
        return allowGeometryUpdates;
    }

    public Field getGeometryField() {
        return geometryField;
    }
}

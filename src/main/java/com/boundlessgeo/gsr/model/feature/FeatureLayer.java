package com.boundlessgeo.gsr.model.feature;

import com.boundlessgeo.gsr.model.map.AbstractLayerOrTable;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Layer model for features
 */
public class FeatureLayer extends AbstractLayerOrTable {

    // editFieldsInfo - skipped we are not editable
    // ownershipBasedAccessControlForFeatures - skipped we are not doing ownership
    // syncCanReturnChanges - skipped revision not supported until geogig
    protected Boolean syncCanReturnChanges = false;
    // supportsRollbackOnFailureParameter - false as we cannot edit at all through GSR
    protected Boolean supportsRollbackOnFailureParameter = false;
    // supportsStatistics - may be able to implement with aggregate functions
    protected Boolean supportsStatistics = false;
    // supportsAdvancedQueries - not implemented yet (no queries at all.) implement using SortBy
    protected Boolean supportsAdvancedQueries = false;
    // supportsCoordinatesQuantization - Supported. See QuantizedGeometryEncoder
    protected Boolean supportsCoordinatesQuantization = true;



    // enableZDefaults - ignore
    // zDefault - ignore
    // allowGeometryUpdates - editing not supported at this time

    // htmlPopupType - could consider use of GetFeatureInfo

    // objectIdField - placeholder for FeatureId
    public String objectIdField = FeatureEncoder.OBJECTID_FIELD_NAME;

    // globalIdField - placeholder for FeatureId
    protected String globalIdField = "";

    // templates - we can list one template based on schema default values
    protected List templates = new ArrayList();

    public FeatureLayer(AbstractLayerOrTable entry) throws IOException {
        super(entry);
        defaultVisibility = true;
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
}

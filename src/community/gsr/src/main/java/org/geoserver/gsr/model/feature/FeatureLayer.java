/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.gsr.model.map.AbstractLayerOrTable;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.feature.type.FeatureType;

/** Layer model for features */
public class FeatureLayer extends AbstractLayerOrTable {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(FeatureLayer.class);

    // editFieldsInfo - skipped we are not editable
    // ownershipBasedAccessControlForFeatures - skipped we are not doing ownership
    // syncCanReturnChanges - revision not supported
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
                LOGGER.log(
                        Level.WARNING,
                        "Omitting geometryField for layer "
                                + layer
                                + " because we were unable to connect to the underlying controller.",
                        e);
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

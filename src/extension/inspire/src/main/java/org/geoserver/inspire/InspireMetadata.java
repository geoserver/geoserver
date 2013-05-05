/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

public enum InspireMetadata {
    LANGUAGE("inspire.language"), SERVICE_METADATA_URL("inspire.metadataURL"), SERVICE_METADATA_TYPE(
            "inspire.metadataURLType"), SPATIAL_DATASET_IDENTIFIER_TYPE("inspire.spatialDatasetIdentifier");

    public String key;

    private InspireMetadata(String key) {
        this.key = key;
    }
}

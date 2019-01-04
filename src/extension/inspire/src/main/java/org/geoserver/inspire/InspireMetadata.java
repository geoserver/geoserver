/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

public enum InspireMetadata {
    CREATE_EXTENDED_CAPABILITIES("inspire.createExtendedCapabilities"),
    LANGUAGE("inspire.language"),
    SERVICE_METADATA_URL("inspire.metadataURL"),
    SERVICE_METADATA_TYPE("inspire.metadataURLType"),
    SPATIAL_DATASET_IDENTIFIER_TYPE("inspire.spatialDatasetIdentifier");

    public String key;

    private InspireMetadata(String key) {
        this.key = key;
    }
}

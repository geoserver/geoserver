package org.geoserver.inspire.wms;

public enum InspireMetadata {
    LANGUAGE("inspire.language"), SERVICE_METADATA_URL("inspire.metadataURL"), SERVICE_METADATA_TYPE(
            "inspire.metadataURLType");

    public String key;

    private InspireMetadata(String key) {
        this.key = key;
    }
}

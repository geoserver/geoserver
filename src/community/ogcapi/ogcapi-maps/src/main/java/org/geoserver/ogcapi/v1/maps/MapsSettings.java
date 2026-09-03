/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import org.geoserver.wms.WMSInfo;

/**
 * The OGC API - Maps tuning knobs, persisted per server in the {@link WMSInfo} metadata, next to the WMS ones such as
 * the SVG renderer choice. They are not conformance classes, so they do not belong to {@link MapsConformance}.
 */
public final class MapsSettings {

    /** The {@link WMSInfo} metadata key holding the {@link #defaultCollections(WMSInfo)} value, an {@code Integer}. */
    public static final String DEFAULT_COLLECTIONS_KEY = "ogcapiMapsDefaultCollections";

    /** Used when {@link #DEFAULT_COLLECTIONS_KEY} is not configured. */
    public static final int DEFAULT_COLLECTIONS = 10;

    private MapsSettings() {}

    /**
     * @return how many collections a dataset map draws when the request selects none; which ones they are is up to
     *     {@code DatasetCollections.mappable}
     */
    public static int defaultCollections(WMSInfo wmsInfo) {
        Integer configured = wmsInfo.getMetadata().get(DEFAULT_COLLECTIONS_KEY, Integer.class);
        return configured != null ? configured : DEFAULT_COLLECTIONS;
    }
}

/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.json.GeoJSONBuilder;
import org.geoserver.json.GeoJSONFeatureWriter;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;

/** GeoJSON feature writer for WFS that adds paging links to the output based on the FeatureCollectionResponse. */
public class WFSGeoJSONFeatureWriter<T extends FeatureType, F extends Feature> extends GeoJSONFeatureWriter<T, F> {
    protected final FeatureCollectionResponse response;
    private final String mimeType;

    public WFSGeoJSONFeatureWriter(GeoServer gs, String mimeType, FeatureCollectionResponse response) {
        super(gs);
        this.response = response;
        this.mimeType = mimeType;
    }

    @Override
    protected boolean isFeatureBounding() {
        return gs.getService(WFSInfo.class).isFeatureBounding();
    }

    @Override
    protected void writeExtraCollectionProperties(List<FeatureCollection<T, F>> featureCollections, GeoJSONBuilder jb) {
        if (response.getPrevious() != null || response.getNext() != null) {
            jb.key("links");
            jb.array();
            jb.writeLink(jb, "previous page", mimeType, "previous", response.getPrevious());
            jb.writeLink(jb, "next page", mimeType, "next", response.getNext());
            jb.endArray();
        }
    }
}

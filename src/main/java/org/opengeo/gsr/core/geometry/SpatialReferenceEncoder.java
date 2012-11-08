package org.opengeo.gsr.core.geometry;

import net.sf.json.util.JSONBuilder;

public class SpatialReferenceEncoder {
    public static void toJson(SpatialReference sr, JSONBuilder json) {
        if (sr instanceof SpatialReferenceWKID) {
            SpatialReferenceWKID wkid = (SpatialReferenceWKID) sr;
            json.object()
              .key("wkid").value(wkid.getWkid())
            .endObject();
        } else if (sr instanceof SpatialReferenceWKT) {
            SpatialReferenceWKT wkt = (SpatialReferenceWKT) sr;
            json.object()
              .key("wkt").value(wkt.getWkt())
            .endObject();
        }
    }
}

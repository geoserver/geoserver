/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.geometry;

import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import org.geoserver.gsr.model.geometry.SpatialReference;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKID;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKT;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;

public class SpatialReferenceEncoder {
    /**
     * Converts a spatial reference to a json representation
     *
     * @param sr the spatial reference
     * @param json a JSONBuilder which the json representation is added to
     */
    public static void toJson(SpatialReference sr, JSONBuilder json) {
        if (sr instanceof SpatialReferenceWKID wkid) {
            json.object().key("wkid").value(wkid.getWkid()).endObject();
        } else if (sr instanceof SpatialReferenceWKT wkt) {
            json.object().key("wkt").value(wkt.getWkt()).endObject();
        }
    }

    /**
     * Convert a json representation to a {@link SpatialReference}
     *
     * @param json the json
     * @return the spatial reference
     */
    public static SpatialReference fromJson(JSONObject json) {
        if (json.containsKey("wkid")) {
            return new SpatialReferenceWKID(json.getInt("wkid"));
        } else if (json.containsKey("wkt")) {
            return new SpatialReferenceWKT(json.getString("wkt"));
        }
        if (json.containsKey("uri")) {
            // TODO: I'm not sure how to look these up - need to check out how GeoServer does this
            // for WFS requests.
            throw new RuntimeException("Spatial reference specified as URI - decoding these is not yet implemented.");
        }

        throw new JSONException("Could not determine spatial reference from JSON: " + json);
    }

    /**
     * Convert a json reference to a {@link CoordinateReferenceSystem}
     *
     * @see #fromJson(JSONObject)
     * @see #coordinateReferenceSystemFromSpatialReference(SpatialReference)
     */
    public static CoordinateReferenceSystem coordinateReferenceSystemFromJSON(JSON json) {
        if (!(json instanceof JSONObject)) {
            throw new JSONException("Spatial Reference must be encoded as JSON Object: was " + json);
        }
        JSONObject obj = (JSONObject) json;
        return coordinateReferenceSystemFromSpatialReference(fromJson(obj));
    }

    /**
     * Convert a {@link SpatialReference} to a {@link CoordinateReferenceSystem}
     *
     * @param sr the spatial reference
     * @return the coordinate reference system
     */
    public static CoordinateReferenceSystem coordinateReferenceSystemFromSpatialReference(SpatialReference sr) {

        if (sr instanceof SpatialReferenceWKID iD) {
            int wkid = iD.getWkid();

            try {
                return CRS.decode("EPSG:" + wkid);
            } catch (FactoryException e) {
                throw new IllegalArgumentException(
                        "SRID " + wkid + " does not correspond to any known spatial reference");
            }
        }

        if (sr instanceof SpatialReferenceWKT kT) {
            String wkt = kT.getWkt();
            try {
                return CRS.parseWKT(wkt);
            } catch (FactoryException e) {
                throw new IllegalArgumentException("wkt value (" + wkt + ") is not valid well-known text", e);
            }
        }
        throw new IllegalArgumentException(
                "Could not determine coordinate reference system from spatial reference: " + sr);
    }
}

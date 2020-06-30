/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfstemplating.writers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;

/** Implements its superclass methods to write a valid GeoJSON output */
public class GeoJsonWriter extends CommonJsonWriter {

    public GeoJsonWriter(JsonGenerator generator) {
        super(generator);
    }

    @Override
    protected void writeValue(Object value) throws IOException {
        if (value instanceof String) {
            writeString((String) value);
        } else if (value instanceof Integer) {
            writeNumber((Integer) value);
        } else if (value instanceof Double) {
            writeNumber((Double) value);
        } else if (value instanceof Float) {
            writeNumber((Float) value);
        } else if (value instanceof Long) {
            writeNumber((Long) value);
        } else if (value instanceof BigInteger) {
            writeNumber((BigInteger) value);
        } else if (value instanceof BigDecimal) {
            writeNumber((BigDecimal) value);
        } else if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
        }
    }

    @Override
    protected void writeGeometry(Object value) throws IOException {
        GeometryJSON geomJson = new GeometryJSON();
        String strGeom = geomJson.toString((Geometry) value);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(strGeom);
        writeObjectNode(null, actualObj);
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.Map;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;

public class STACGeoJSONWriter extends GeoJSONWriter {

    public STACGeoJSONWriter(JsonGenerator generator) {
        super(generator);
    }

    /**
     * Writes a OGC link object
     *
     * @param href
     * @param rel
     * @param mimeType
     * @param title
     * @param method
     * @throws IOException
     */
    public void writeLink(
            String href,
            String rel,
            String mimeType,
            String title,
            String method,
            Map<String, Object> body,
            boolean merge)
            throws IOException {
        if (href != null) {
            startObject();
            if (title != null) {
                writeFieldName("title");
                writeValue(title);
            }
            if (mimeType != null) {
                writeFieldName("type");
                writeValue(mimeType);
            }
            if (rel != null) {
                writeFieldName("rel");
                writeValue(rel);
            }
            if (method != null) {
                writeFieldName("method");
                writeValue(method);
            }
            if (body != null) {
                writeFieldName("body");
                startObject();
                for (Map.Entry<String, Object> e : body.entrySet()) {
                    writeFieldName(e.getKey());
                    writeObject(e.getValue());
                }
                endObject();
            }
            if (merge) {
                writeFieldName("merge");
                writeBoolean(true);
            }
            writeFieldName("href");
            writeValue(href);
            endObject();
        }
    }
}

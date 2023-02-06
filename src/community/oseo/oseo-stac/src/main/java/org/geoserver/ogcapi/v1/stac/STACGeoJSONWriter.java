/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.Map;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;

public class STACGeoJSONWriter extends GeoJSONWriter {

    public STACGeoJSONWriter(JsonGenerator generator) {
        super(generator);
    }

    public STACGeoJSONWriter(JsonGenerator generator, TemplateIdentifier identifier) {
        super(generator, identifier);
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
            writeStartObject();
            if (title != null) {
                generator.writeFieldName("title");
                writeValue(title);
            }
            if (mimeType != null) {
                generator.writeFieldName("type");
                writeValue(mimeType);
            }
            if (rel != null) {
                generator.writeFieldName("rel");
                writeValue(rel);
            }
            if (method != null) {
                generator.writeFieldName("method");
                writeValue(method);
            }
            if (body != null) {
                generator.writeFieldName("body");
                writeStartObject();
                for (Map.Entry<String, Object> e : body.entrySet()) {
                    generator.writeFieldName(e.getKey());
                    generator.writeObject(e.getValue());
                }
                writeEndObject();
            }
            if (merge) {
                generator.writeFieldName("merge");
                generator.writeBoolean(true);
            }
            generator.writeFieldName("href");
            writeValue(href);
            writeEndObject();
        }
    }
}

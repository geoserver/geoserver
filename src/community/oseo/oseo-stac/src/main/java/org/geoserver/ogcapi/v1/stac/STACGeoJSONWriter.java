/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.io.IOException;
import java.util.Map;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import tools.jackson.core.JsonGenerator;

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
                generator.writeName("title");
                writeValue(title);
            }
            if (mimeType != null) {
                generator.writeName("type");
                writeValue(mimeType);
            }
            if (rel != null) {
                generator.writeName("rel");
                writeValue(rel);
            }
            if (method != null) {
                generator.writeName("method");
                writeValue(method);
            }
            if (body != null) {
                generator.writeName("body");
                writeStartObject();
                for (Map.Entry<String, Object> e : body.entrySet()) {
                    generator.writeName(e.getKey());
                    generator.writePOJO(e.getValue());
                }
                writeEndObject();
            }
            if (merge) {
                generator.writeName("merge");
                generator.writeBoolean(true);
            }
            generator.writeName("href");
            writeValue(href);
            writeEndObject();
        }
    }
}

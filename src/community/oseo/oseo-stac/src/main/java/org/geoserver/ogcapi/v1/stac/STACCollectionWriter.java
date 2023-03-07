/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;

/** A writer for the collections */
public class STACCollectionWriter extends GeoJSONWriter {

    public STACCollectionWriter(JsonGenerator generator, TemplateIdentifier identifier) {
        super(generator, identifier);
    }

    public STACCollectionWriter(JsonGenerator generator) {
        super(generator);
    }

    @Override
    public void startTemplateOutput(EncodingHints encodingHints) throws IOException {
        writeStartObject();
        generator.writeFieldName("collections");
        writeStartArray();
    }

    @Override
    public void endTemplateOutput(EncodingHints encodingHints) throws IOException {
        writeEndObject();
    }
}

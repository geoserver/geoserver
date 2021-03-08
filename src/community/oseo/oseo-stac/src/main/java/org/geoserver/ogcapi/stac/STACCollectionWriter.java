/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;

/** A writer for the collections */
public class STACCollectionWriter extends GeoJSONWriter {

    public STACCollectionWriter(JsonGenerator generator) {
        super(generator);
    }

    @Override
    public void startTemplateOutput() throws IOException {
        writeStartObject();
        writeFieldName("collections");
        writeStartArray();
    }

    @Override
    public void endTemplateOutput() throws IOException {
        writeEndObject();
    }
}

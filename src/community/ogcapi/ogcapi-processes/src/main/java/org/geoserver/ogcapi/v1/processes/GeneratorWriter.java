/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.Writer;
import org.apache.commons.text.StringEscapeUtils;

/** Writer that writes characters to a {@link JsonGenerator}. */
class GeneratorWriter extends Writer {
    private final JsonGenerator generator;
    boolean escape;

    public GeneratorWriter(JsonGenerator generator, boolean escape) {
        this.generator = generator;
        this.escape = escape;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (escape) {
            String string = new String(cbuf, off, len);
            String escaped = StringEscapeUtils.escapeJson(string);
            generator.writeRaw(escaped);
        } else {
            generator.writeRaw(cbuf, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }
}

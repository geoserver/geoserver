/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import net.sf.json.JSONObject;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert {@link ImportContext} to/from {@link MediaType#APPLICATION_JSON}. */
@Component
public class ImportContextJSONMessageConverter extends BaseMessageConverter<ImportContext> {

    Importer importer;

    @Autowired
    public ImportContextJSONMessageConverter(Importer importer) {
        super(MediaType.APPLICATION_JSON, MediaTypeExtensions.TEXT_JSON);
        this.importer = importer;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ImportContext.class.isAssignableFrom(clazz);
    }

    //
    // Reading
    //
    @Override
    protected ImportContext readInternal(
            Class<? extends ImportContext> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try (InputStream in = inputMessage.getBody()) {
            ImportJSONReader reader = new ImportJSONReader(importer);
            JSONObject json = reader.parse(in);
            ImportContext context = reader.context(json);

            return context;
        }
    }

    //
    // writing
    //
    @Override
    protected void writeInternal(ImportContext context, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        try (OutputStreamWriter output = new OutputStreamWriter(outputMessage.getBody())) {
            FlushableJSONBuilder json = new FlushableJSONBuilder(output);
            ImportJSONWriter writer = new ImportJSONWriter(importer);

            writer.context(json, context, true, writer.expand(1));

            output.flush();
        }
    }
}

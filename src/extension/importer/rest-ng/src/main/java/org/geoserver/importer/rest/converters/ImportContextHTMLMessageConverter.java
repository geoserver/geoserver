package org.geoserver.importer.rest.converters;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * {@link BaseMessageConverter} implementation for writing {@link ImportTask} objects to JSON.
 */
@Component
public class ImportContextHTMLMessageConverter extends BaseMessageConverter<ImportContext> {

    Importer importer;

    @Autowired
    public ImportContextHTMLMessageConverter(Importer importer) {
        super(MediaType.TEXT_HTML);
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
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    //
    // writing
    //
    @Override
    protected void writeInternal(ImportContext context, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        try (OutputStreamWriter output = new OutputStreamWriter(outputMessage.getBody())) {
            output.write("<html><body><pre>");

            FlushableJSONBuilder json = new FlushableJSONBuilder(output);
            ImportJSONWriter writer = new ImportJSONWriter(
                    importer);

            writer.context(json, context, true, writer.expand(1));

            output.write("</pre></body></html>");
            output.flush();
        }
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.ImportWrapper;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * {@link BaseMessageConverter} implementation for writing {@link ImportWrapper} objects.
 *
 * <p>This converter is willing to write JSON directly, or output JSON as HTML for visual
 * inspection.
 */
@Component
public class ImportWrapperMessageConverter extends BaseMessageConverter<ImportWrapper> {

    Importer importer;

    @Autowired
    public ImportWrapperMessageConverter(Importer importer) {
        super(MediaType.APPLICATION_JSON, MediaTypeExtensions.TEXT_JSON, MediaType.TEXT_HTML);
        this.importer = importer;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ImportWrapper.class.isAssignableFrom(clazz);
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
    protected void writeInternal(ImportWrapper wrapper, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();
        try (OutputStreamWriter outputWriter = new OutputStreamWriter(outputMessage.getBody())) {
            if (MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
                writeHTML(wrapper, outputWriter);
            } else {
                writeJSON(wrapper, outputWriter);
            }
            outputWriter.flush();
        }
    }

    private void writeHTML(ImportWrapper wrapper, OutputStreamWriter outputWriter)
            throws IOException {
        outputWriter.write("<html><body><pre>");
        writeJSON(wrapper, outputWriter);
        outputWriter.write("</pre></body></html>");
    }

    private void writeJSON(ImportWrapper wrapper, OutputStreamWriter outputWriter)
            throws IOException {
        FlushableJSONBuilder json = new FlushableJSONBuilder(outputWriter);
        ImportJSONWriter writer = new ImportJSONWriter(importer);

        wrapper.write(outputWriter, json, writer);
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert {@link ImportTask} to HTML. */
@Component
public class ImportTaskHTMLMessageConverter extends BaseMessageConverter<ImportTask> {

    Importer importer;

    @Autowired
    public ImportTaskHTMLMessageConverter(Importer importer) {
        super(MediaType.TEXT_HTML);
        this.importer = importer;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ImportTask.class.isAssignableFrom(clazz);
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
    protected void writeInternal(ImportTask task, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        try (OutputStreamWriter outputStream = new OutputStreamWriter(outputMessage.getBody())) {

            outputStream.write("<html><body><pre>");

            FlushableJSONBuilder json = new FlushableJSONBuilder(outputStream);
            ImportJSONWriter writer = new ImportJSONWriter(importer);

            int expand = writer.expand(1);

            writer.task(json, task, true, expand);

            outputStream.write("</pre></body></html>");
            outputStream.flush();
        }
    }
}

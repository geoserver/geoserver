/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import net.sf.json.JSONObject;
import org.geoserver.importer.ImportData;
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

/** Convert {@link ImportData} to/from JSON. */
@Component
public class ImportDataJSONMessageConverter extends BaseMessageConverter<ImportData> {

    Importer importer;

    @Autowired
    public ImportDataJSONMessageConverter(Importer importer) {
        super(MediaType.APPLICATION_JSON, MediaTypeExtensions.TEXT_JSON);
        this.importer = importer;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ImportData.class.isAssignableFrom(clazz);
    }

    //
    // Reading
    //
    @Override
    protected ImportData readInternal(
            Class<? extends ImportData> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try (InputStream in = inputMessage.getBody()) {
            ImportJSONReader reader = new ImportJSONReader(importer);
            JSONObject json = reader.parse(in);
            ImportData data = reader.data(json);

            return data;
        }
    }

    //
    // writing
    //
    @Override
    protected void writeInternal(ImportData data, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        try (OutputStreamWriter outputStream = new OutputStreamWriter(outputMessage.getBody())) {

            FlushableJSONBuilder json = new FlushableJSONBuilder(outputStream);
            ImportJSONWriter writer = new ImportJSONWriter(importer);

            Object parent = data.getParent();
            int expand = writer.expand(1);

            writer.data(json, data, parent, expand);

            outputStream.flush();
        }
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import java.io.IOException;
import java.io.InputStream;
import net.sf.json.JSONObject;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.ImportLayer;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert {@link ImportTask} to/from JSON. */
@Component
public class ImportLayerJSONMessageConverter extends BaseMessageConverter<ImportLayer> {

    Importer importer;

    @Autowired
    public ImportLayerJSONMessageConverter(Importer importer) {
        super(MediaType.APPLICATION_JSON, MediaTypeExtensions.TEXT_JSON);
        this.importer = importer;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ImportLayer.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    //
    // Reading
    //
    @Override
    protected ImportLayer readInternal(
            Class<? extends ImportLayer> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try (InputStream in = inputMessage.getBody()) {
            ImportJSONReader reader = new ImportJSONReader(importer);
            JSONObject json = reader.parse(in);

            return reader.layer(json);
        }
    }

    //
    // writing
    //
    @Override
    protected void writeInternal(ImportLayer layer, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        throw new UnsupportedOperationException();
    }
}

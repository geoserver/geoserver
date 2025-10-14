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
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

/** Convert {@link ImportTask} to/from JSON. */
@Component
public class ImportTransformJSONMessageConverter extends BaseMessageConverter<ImportTransform> {

    Importer importer;

    @Autowired
    public ImportTransformJSONMessageConverter(Importer importer) {
        super(MediaType.APPLICATION_JSON, MediaTypeExtensions.TEXT_JSON);
        this.importer = importer;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ImportTransform.class.isAssignableFrom(clazz);
    }

    //
    // Reading
    //
    @Override
    protected ImportTransform readInternal(Class<? extends ImportTransform> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        try (InputStream in = inputMessage.getBody()) {
            ImportJSONReader reader = new ImportJSONReader(importer);
            JSONObject json = reader.parse(in);
            ImportTransform transform = reader.transform(json);

            return transform;
        }
    }

    //
    // writing
    //
    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }
}

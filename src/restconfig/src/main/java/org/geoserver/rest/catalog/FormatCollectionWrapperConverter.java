/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert FormatCollectionWrapper to JSON or GML as required. */
@Component
public class FormatCollectionWrapperConverter
        extends FeatureCollectionConverter<FormatCollectionWrapper> {

    public FormatCollectionWrapperConverter() {
        super(
                MediaType.APPLICATION_XML,
                MediaType.TEXT_XML,
                MediaTypeExtensions.TEXT_JSON,
                MediaType.APPLICATION_JSON);
    }

    @Override
    protected void writeInternal(FormatCollectionWrapper content, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType mediaType = outputMessage.getHeaders().getContentType();
        if (MediaType.APPLICATION_JSON.includes(mediaType)
                || MediaTypeExtensions.TEXT_JSON.includes(mediaType)) {
            writeGeoJsonl(content.getCollection(), outputMessage);
        } else if (MediaType.APPLICATION_XML.includes(mediaType)
                || MediaTypeExtensions.TEXT_JSON.includes(mediaType)) {
            writeGML(content.getCollection(), outputMessage);
        }
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return FormatCollectionWrapper.class.isAssignableFrom(clazz);
    }
}

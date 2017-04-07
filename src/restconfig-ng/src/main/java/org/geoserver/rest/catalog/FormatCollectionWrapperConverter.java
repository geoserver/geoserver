/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;

import org.geoserver.rest.catalog.FormatCollectionWrapper.JSONCollectionWrapper;
import org.geoserver.rest.catalog.FormatCollectionWrapper.XMLCollectionWrapper;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Convert FormatCollectionWrapper to JSON or GML as required.
 */
@Component
public class FormatCollectionWrapperConverter
        extends FeatureCollectionConverter<FormatCollectionWrapper> {

    public FormatCollectionWrapperConverter() {
        super(MediaType.APPLICATION_XML, CatalogController.MEDIATYPE_TEXT_XML,
                CatalogController.MEDIATYPE_TEXT_JSON, MediaType.APPLICATION_JSON);
    }

    @Override
    protected void writeInternal(FormatCollectionWrapper content, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType mediaType = outputMessage.getHeaders().getContentType();
        if (MediaType.APPLICATION_JSON.includes(mediaType) || CatalogController.MEDIATYPE_TEXT_JSON.includes(mediaType)) {
            writeGeoJsonl(content.getCollection(), outputMessage);
        } else if (MediaType.APPLICATION_XML.includes(mediaType) || CatalogController.MEDIATYPE_TEXT_JSON.includes(mediaType) ) {
            writeGML(content.getCollection(), outputMessage);
        }
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return FormatCollectionWrapper.class.isAssignableFrom(clazz);
    }

}

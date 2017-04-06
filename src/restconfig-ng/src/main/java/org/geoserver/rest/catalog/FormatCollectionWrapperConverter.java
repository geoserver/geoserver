/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;

import org.geoserver.rest.catalog.FormatCollectionWrapper.XMLCollectionWrapper;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class FormatCollectionWrapperConverter extends FeatureCollectionConverter<FormatCollectionWrapper> {

    public FormatCollectionWrapperConverter(){
        super(MediaType.APPLICATION_XML);
    }

    /**
     * Access features, unwrapping if necessary.
     * @param o
     * @return features
     */
    protected SimpleFeatureCollection getFeatures(FormatCollectionWrapper content){
        return content.getCollection();
    }
    
    @Override
    protected void writeInternal(FormatCollectionWrapper content, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType mediaType = outputMessage.getHeaders().getContentType();
        if (MediaType.APPLICATION_JSON.includes(mediaType)
                || CatalogController.MEDIATYPE_TEXT_JSON.includes(mediaType)) {
            writeGeoJsonl(content, outputMessage);
        }
        else if (MediaType.APPLICATION_XML.includes(mediaType)) {
            writeGML(content, outputMessage);
        } 
    }
    
    @Override
    protected boolean supports(Class<?> clazz) {
        return XMLCollectionWrapper.class.isAssignableFrom(clazz);
    }

}

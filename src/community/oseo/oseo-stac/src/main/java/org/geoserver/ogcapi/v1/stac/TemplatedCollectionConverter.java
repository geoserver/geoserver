/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.platform.ServiceException;
import org.geotools.api.feature.Feature;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Converter for the {@link CollectionResponse} that will encode STAC collections using a feature
 * template
 */
@Component
public class TemplatedCollectionConverter extends AbstractHttpMessageConverter<CollectionResponse> {

    private final STACTemplates templates;

    public TemplatedCollectionConverter(STACTemplates templates) {
        super(MediaType.APPLICATION_JSON);
        this.templates = templates;
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return CollectionResponse.class.isAssignableFrom(aClass);
    }

    @Override
    protected CollectionResponse readInternal(
            Class<? extends CollectionResponse> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("This converter is write only");
    }

    @Override
    protected void writeInternal(
            CollectionResponse collectionsResponse, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        Feature collection = collectionsResponse.getCollection();
        String collectionId = (String) collection.getProperty("identifier").getValue();
        RootBuilder builder = templates.getCollectionTemplate(collectionId);

        try (STACCollectionWriter writer =
                new STACCollectionWriter(
                        new JsonFactory()
                                .createGenerator(httpOutputMessage.getBody(), JsonEncoding.UTF8))) {
            // no collection wrapper
            builder.evaluate(writer, new TemplateBuilderContext(collection));
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}

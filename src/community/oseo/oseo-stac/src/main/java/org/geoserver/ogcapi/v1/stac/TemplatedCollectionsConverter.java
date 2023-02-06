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
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
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
public class TemplatedCollectionsConverter
        extends AbstractHttpMessageConverter<CollectionsResponse> {

    private final STACTemplates templates;

    public TemplatedCollectionsConverter(STACTemplates templates) {
        super(MediaType.APPLICATION_JSON);
        this.templates = templates;
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return CollectionsResponse.class.isAssignableFrom(aClass);
    }

    @Override
    protected CollectionsResponse readInternal(
            Class<? extends CollectionsResponse> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("This converter is write only");
    }

    @Override
    protected void writeInternal(
            CollectionsResponse collectionsResponse, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {

        try (STACCollectionWriter writer =
                new STACCollectionWriter(
                        new JsonFactory()
                                .createGenerator(httpOutputMessage.getBody(), JsonEncoding.UTF8))) {
            writer.startTemplateOutput(null);
            try (FeatureIterator features = collectionsResponse.getCollections().features()) {
                while (features.hasNext()) {
                    Feature collection = features.next();
                    String collectionId = (String) collection.getProperty("identifier").getValue();
                    RootBuilder builder = templates.getCollectionTemplate(collectionId);
                    builder.evaluate(writer, new TemplateBuilderContext(collection));
                }
            }
            writer.writeEndArray();
            // writeAdditionFields(writer, featureCollection, getFeature);
            writer.endTemplateOutput(null);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}

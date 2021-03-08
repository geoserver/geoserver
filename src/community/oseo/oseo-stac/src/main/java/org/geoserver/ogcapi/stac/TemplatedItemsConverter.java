/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.FeatureIterator;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Converter for the {@link CollectionsDocument} that will encode STAC collections using a feature
 * template
 */
@Component
public class TemplatedItemsConverter extends AbstractHttpMessageConverter<ItemsResponse> {

    private final STACTemplates templates;

    public TemplatedItemsConverter(STACTemplates templates) {
        super(OGCAPIMediaTypes.GEOJSON);
        this.templates = templates;
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return ItemsResponse.class.isAssignableFrom(aClass);
    }

    @Override
    protected ItemsResponse readInternal(
            Class<? extends ItemsResponse> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("This converter is write only");
    }

    @Override
    protected void writeInternal(ItemsResponse itemsResponse, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        RootBuilder builder = templates.getItemTemplate();

        try (GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory()
                                .createGenerator(httpOutputMessage.getBody(), JsonEncoding.UTF8))) {
            writer.startTemplateOutput();
            try (FeatureIterator features = itemsResponse.getItems().features()) {
                while (features.hasNext()) {
                    builder.evaluate(writer, new TemplateBuilderContext(features.next()));
                }
            }
            writer.endArray();
            // writeAdditionFields(writer, featureCollection, getFeature);
            writer.endTemplateOutput();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}

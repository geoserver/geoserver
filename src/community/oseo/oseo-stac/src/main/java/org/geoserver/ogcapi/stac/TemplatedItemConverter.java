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
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.platform.ServiceException;
import org.opengis.feature.Feature;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Converter for the {@link ItemsResponse} that will encode a single STAC item using a feature
 * template
 */
@Component
public class TemplatedItemConverter extends AbstractHttpMessageConverter<ItemResponse> {

    private final STACTemplates templates;

    public TemplatedItemConverter(STACTemplates templates) {
        super(OGCAPIMediaTypes.GEOJSON);
        this.templates = templates;
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return ItemResponse.class.isAssignableFrom(aClass);
    }

    @Override
    protected ItemResponse readInternal(
            Class<? extends ItemResponse> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("This converter is write only");
    }

    @Override
    protected void writeInternal(ItemResponse response, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        Feature item = response.getItem();
        RootBuilder builder = getRootBuilder(item, response);
        try (GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory()
                                .createGenerator(httpOutputMessage.getBody(), JsonEncoding.UTF8),
                        TemplateIdentifier.GEOJSON)) {
            // no collection wrapper
            builder.evaluate(writer, new TemplateBuilderContext(item));
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private RootBuilder getRootBuilder(Feature item, ItemResponse response) throws IOException {
        RootBuilder builder = response.getTemplate();
        if (builder == null)
            builder =
                    templates.getItemTemplate(
                            (String) item.getProperty("parentIdentifier").getValue());
        return builder;
    }
}

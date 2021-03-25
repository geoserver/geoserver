/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.geoserver.ogcapi.Link.REL_NEXT;
import static org.geoserver.ogcapi.Link.REL_PREV;
import static org.geoserver.ogcapi.Link.REL_SELF;
import static org.geoserver.ogcapi.OGCAPIMediaTypes.GEOJSON_VALUE;
import static org.springframework.http.HttpMethod.POST;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.FeatureIterator;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.stereotype.Component;

/** Converter for the {@link ItemsResponse} that will encode STAC items using a feature template */
@Component
public class TemplatedItemsConverter extends AbstractHttpMessageConverter<AbstractItemsResponse> {

    private final STACTemplates templates;

    public TemplatedItemsConverter(STACTemplates templates) {
        super(OGCAPIMediaTypes.GEOJSON);
        this.templates = templates;
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return AbstractItemsResponse.class.isAssignableFrom(aClass);
    }

    @Override
    protected ItemsResponse readInternal(
            Class<? extends AbstractItemsResponse> aClass, HttpInputMessage httpInputMessage) {
        throw new UnsupportedOperationException("This converter is write only");
    }

    @Override
    protected void writeInternal(
            AbstractItemsResponse itemsResponse, HttpOutputMessage httpOutputMessage)
            throws IOException {
        RootBuilder builder = templates.getItemTemplate();

        try (STACGeoJSONWriter writer =
                new STACGeoJSONWriter(
                        new JsonFactory()
                                .createGenerator(httpOutputMessage.getBody(), JsonEncoding.UTF8))) {
            writer.startTemplateOutput();
            try (FeatureIterator features = itemsResponse.getItems().features()) {
                while (features.hasNext()) {
                    builder.evaluate(writer, new TemplateBuilderContext(features.next()));
                }
            }
            writer.endArray();
            writeAdditionFields(writer, itemsResponse);
            writer.endTemplateOutput();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void writeAdditionFields(STACGeoJSONWriter w, AbstractItemsResponse ir)
            throws IOException {
        // number matched
        w.writeFieldName("numberMatched");
        w.writeNumber(ir.getNumberMatched());
        // number returned
        w.writeFieldName("numberReturned");
        int numberReturned = ir.getItems().size();
        w.writeNumber(numberReturned);
        // stac infos
        w.writeFieldName("stac_version");
        w.writeString(STACService.STAC_VERSION);

        // links
        w.writeFieldName("links");
        w.startArray();

        String type = GEOJSON_VALUE;
        if (ir.isPost()) {
            String post = POST.toString();
            w.writeLink(ir.getSelf(), REL_SELF, type, null, post, null, true);
            if (ir.getPreviousBody() != null) {
                w.writeLink(ir.getSelf(), REL_PREV, type, null, post, ir.getPreviousBody(), true);
            }
            if (ir.getNextBody() != null) {
                w.writeLink(ir.getSelf(), REL_NEXT, type, null, post, ir.getNextBody(), true);
            }
        } else {
            if (ir.getPrevious() != null) w.writeLink(ir.getPrevious(), REL_PREV, type, null, null);
            if (ir.getNext() != null) w.writeLink(ir.getNext(), REL_NEXT, type, null, null);
            if (ir.getSelf() != null) w.writeLink(ir.getSelf(), REL_SELF, type, null, null);
        }
        w.endArray();
    }
}

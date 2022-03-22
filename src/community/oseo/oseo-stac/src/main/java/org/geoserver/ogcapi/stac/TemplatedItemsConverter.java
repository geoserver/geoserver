/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.geoserver.ogcapi.Link.REL_NEXT;
import static org.geoserver.ogcapi.Link.REL_PREV;
import static org.geoserver.ogcapi.Link.REL_SELF;
import static org.geoserver.ogcapi.OGCAPIMediaTypes.GEOJSON_VALUE;
import static org.geoserver.ogcapi.stac.QueryResultBuilder.DEF_TEMPLATE;
import static org.springframework.http.HttpMethod.POST;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.stereotype.Component;

/** Converter for the {@link ItemsResponse} that will encode STAC items using a feature template */
@Component
public class TemplatedItemsConverter extends AbstractHttpMessageConverter<AbstractItemsResponse> {

    static final Logger LOGGER = Logging.getLogger(TemplatedItemsConverter.class);

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

        try (STACGeoJSONWriter writer =
                new STACGeoJSONWriter(
                        new JsonFactory()
                                .createGenerator(httpOutputMessage.getBody(), JsonEncoding.UTF8),
                        TemplateIdentifier.GEOJSON)) {
            writer.startTemplateOutput(null);
            FeatureCollection collection = itemsResponse.getItems();
            try (FeatureIterator features = collection.features()) {
                while (features.hasNext()) {
                    // lookup the builder, might be specific to the parent collection
                    Feature feature = features.next();
                    String collectionId =
                            (String) feature.getProperty("parentIdentifier").getValue();
                    RootBuilder builder = getRootBuilder(collectionId, itemsResponse);
                    builder.evaluate(writer, new TemplateBuilderContext(feature));
                }
            }
            writer.writeEndArray();
            writeAdditionFields(writer, itemsResponse);
            writer.endTemplateOutput(null);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private RootBuilder getRootBuilder(String collectionId, AbstractItemsResponse response)
            throws IOException {
        Map<String, RootBuilder> templateMap = response.getTemplateMap();
        RootBuilder rootBuilder = null;
        if (templateMap != null) {
            if (templateMap.containsKey(collectionId)) rootBuilder = templateMap.get(collectionId);
            else rootBuilder = templateMap.get(DEF_TEMPLATE);
        }
        if (rootBuilder == null) {
            rootBuilder = templates.getItemTemplate(collectionId);
        }
        return rootBuilder;
    }

    private void writeAdditionFields(STACGeoJSONWriter w, AbstractItemsResponse ir)
            throws IOException {
        // number matched
        w.writeElementName("numberMatched", null);
        w.writeElementValue(ir.getNumberMatched(), null);
        // number returned
        w.writeElementName("numberReturned", null);
        int numberReturned = ir.getReturned();
        w.writeElementValue(numberReturned, null);
        // stac infos
        w.writeElementName("stac_version", null);
        w.writeElementValue(STACService.STAC_VERSION, null);

        // links
        w.writeElementName("links", null);
        w.writeStartArray();

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
        w.writeEndArray();
    }
}

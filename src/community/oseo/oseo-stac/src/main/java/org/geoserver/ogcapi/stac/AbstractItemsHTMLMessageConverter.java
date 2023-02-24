/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static java.nio.charset.StandardCharsets.UTF_8;

import freemarker.template.Template;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractHTMLMessageConverter;
import org.geoserver.ogcapi.FreemarkerTemplateSupport;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;

/** Renders a list of items in HTML. */
public abstract class AbstractItemsHTMLMessageConverter<T extends AbstractItemsResponse>
        extends AbstractHTMLMessageConverter<T> {

    public AbstractItemsHTMLMessageConverter(
            Class<T> clazz, FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(clazz, OSEOInfo.class, templateSupport, geoServer);
    }

    @Override
    protected void writeInternal(T value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Template header = getHeaderTemplate();
        Template footer = getFooterTemplate();
        Template content = getContentTemplate();
        Template empty = getEmptyTemplate();

        Map<String, Object> model = new HashMap<>();
        model.put("baseURL", APIRequestInfo.get().getBaseURL());
        if (value instanceof ItemsResponse) {
            model.put("collection", ((ItemsResponse) value).getCollectionId());
        }
        addLinkFunctions(APIRequestInfo.get().getBaseURL(), model);

        try (OutputStreamWriter osw = new OutputStreamWriter(outputMessage.getBody())) {
            templateSupport.processTemplate(header, model, osw, UTF_8);

            // process content template for all feature collections found
            model.put("featureInfo", value.getItems().getSchema());
            model.put("data", value.getItems());
            model.put("previous", value.getPrevious());
            model.put("next", value.getNext());

            if (!value.getItems().isEmpty()) {
                templateSupport.processTemplate(content, model, osw, UTF_8);
            } else {
                templateSupport.processTemplate(empty, model, osw, UTF_8);
            }

            templateSupport.processTemplate(footer, model, osw, UTF_8);
            osw.flush();
        } finally {
            purgeIterators();
        }
    }

    protected abstract Template getEmptyTemplate() throws IOException;

    protected abstract Template getContentTemplate() throws IOException;

    protected abstract Template getFooterTemplate() throws IOException;

    protected abstract Template getHeaderTemplate() throws IOException;
}

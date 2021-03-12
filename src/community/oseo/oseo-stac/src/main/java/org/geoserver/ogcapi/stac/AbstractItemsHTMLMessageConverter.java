/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import freemarker.template.Template;
import freemarker.template.TemplateException;
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
            try {
                header.process(model, osw);
            } catch (TemplateException e) {
                String msg = "Error occurred processing header template.";
                throw new IOException(msg, e);
            }

            // process content template for all feature collections found
            model.put("featureInfo", value.getItems().getSchema());
            model.put("data", value.getItems());
            model.put("previous", value.getPrevious());
            model.put("next", value.getNext());

            if (!value.getItems().isEmpty()) {
                try {
                    content.process(model, osw);
                } catch (TemplateException e) {
                    throw new IOException("Error occurred processing content template", e);
                }
            } else {
                try {
                    empty.process(model, osw);
                } catch (TemplateException e) {
                    throw new IOException("Error occurred processing empty template", e);
                }
            }

            try {
                footer.process(model, osw);
            } catch (TemplateException e) {
                String msg = "Error occurred processing footer template.";
                throw new IOException(msg, e);
            }
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

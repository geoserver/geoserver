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
import org.springframework.stereotype.Component;

/** Renders a list of items in HTML. */
@Component
public class ItemsHTMLMessageConverter extends AbstractHTMLMessageConverter<ItemsResponse> {

    public ItemsHTMLMessageConverter(
            FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(ItemsResponse.class, OSEOInfo.class, templateSupport, geoServer);
    }

    @Override
    protected void writeInternal(ItemsResponse value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Template header = templateSupport.getTemplate(null, "items-header.ftl", this.getClass());
        Template footer = templateSupport.getTemplate(null, "items-footer.ftl", this.getClass());
        Template content = templateSupport.getTemplate(null, "items-content.ftl", this.getClass());
        Template empty = templateSupport.getTemplate(null, "items-empty.ftl", this.getClass());

        Map<String, Object> model = new HashMap<>();
        model.put("baseURL", APIRequestInfo.get().getBaseURL());
        model.put("collection", value.getCollectionId());
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
}

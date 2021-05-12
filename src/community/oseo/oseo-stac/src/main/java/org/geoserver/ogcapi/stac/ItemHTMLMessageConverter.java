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

/** Renders a single item in HTML. */
@Component
public class ItemHTMLMessageConverter extends AbstractHTMLMessageConverter<ItemResponse> {

    public ItemHTMLMessageConverter(
            FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(ItemResponse.class, OSEOInfo.class, templateSupport, geoServer);
    }

    @Override
    protected void writeInternal(ItemResponse value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Template template = templateSupport.getTemplate(null, "item.ftl", STACService.class);

        Map<String, Object> model = new HashMap<>();
        model.put("baseURL", APIRequestInfo.get().getBaseURL());
        model.put("collection", value.getCollectionId());
        addLinkFunctions(APIRequestInfo.get().getBaseURL(), model);

        try (OutputStreamWriter osw = new OutputStreamWriter(outputMessage.getBody())) {
            model.put("item", value.getItem());

            try {
                template.process(model, osw);
            } catch (TemplateException e) {
                throw new IOException("Error occurred processing content template", e);
            }

            osw.flush();
        } finally {
            purgeIterators();
        }
    }
}

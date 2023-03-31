/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import freemarker.template.Template;
import java.io.IOException;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.FreemarkerTemplateSupport;
import org.springframework.stereotype.Component;

/** Renders a list of items in HTML. */
@Component
public class ItemsHTMLMessageConverter extends AbstractItemsHTMLMessageConverter<ItemsResponse> {

    public ItemsHTMLMessageConverter(
            FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(ItemsResponse.class, templateSupport, geoServer);
    }

    protected Template getEmptyTemplate() throws IOException {
        return templateSupport.getTemplate(null, "items-empty.ftl", STACService.class);
    }

    protected Template getContentTemplate() throws IOException {
        return templateSupport.getTemplate(null, "items-content.ftl", STACService.class);
    }

    protected Template getFooterTemplate() throws IOException {
        return templateSupport.getTemplate(null, "items-footer.ftl", STACService.class);
    }

    protected Template getHeaderTemplate() throws IOException {
        return templateSupport.getTemplate(null, "items-header.ftl", STACService.class);
    }
}

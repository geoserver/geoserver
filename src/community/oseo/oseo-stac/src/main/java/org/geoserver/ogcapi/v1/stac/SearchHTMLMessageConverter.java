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
public class SearchHTMLMessageConverter extends AbstractItemsHTMLMessageConverter<SearchResponse> {

    public SearchHTMLMessageConverter(
            FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(SearchResponse.class, templateSupport, geoServer);
    }

    protected Template getEmptyTemplate() throws IOException {
        return templateSupport.getTemplate(null, "search-empty.ftl", STACService.class);
    }

    protected Template getContentTemplate() throws IOException {
        return templateSupport.getTemplate(null, "search-content.ftl", STACService.class);
    }

    protected Template getFooterTemplate() throws IOException {
        return templateSupport.getTemplate(null, "search-footer.ftl", STACService.class);
    }

    protected Template getHeaderTemplate() throws IOException {
        return templateSupport.getTemplate(null, "search-header.ftl", STACService.class);
    }
}

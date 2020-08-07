/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.dggs;

import freemarker.template.Template;
import java.io.IOException;
import org.geoserver.api.FreemarkerTemplateSupport;
import org.geoserver.api.features.GetFeatureHTMLMessageConverter;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * DGGS specific HTML output format. TODO: we should be linking back to the cell that generates the
 * parents, the children, neighbors, the link should probably be something added in DGGSService and
 * retrieved by role, or something like that, to make everything more extensible (so, to be done in
 * GetFeatureHTMLMessageConverter)
 */
@Component
public class DGGSFeatureHTMLMessageConverter extends GetFeatureHTMLMessageConverter
        implements Ordered {
    public DGGSFeatureHTMLMessageConverter(
            FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(templateSupport, geoServer);
    }

    @Override
    protected boolean canWrite(MediaType mediaType) {
        return super.canWrite(mediaType);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    protected Template getContentTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(
                typeInfo, "zones-content.ftl", DGGSFeatureHTMLMessageConverter.class);
    }

    protected Template getComplexContentTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(
                typeInfo, "getfeature-complex-content.ftl", GetFeatureHTMLMessageConverter.class);
    }

    protected Template getFooterTemplate(FeatureTypeInfo referenceFeatureType) throws IOException {
        return templateSupport.getTemplate(
                referenceFeatureType,
                "getfeature-footer.ftl",
                GetFeatureHTMLMessageConverter.class);
    }

    protected Template getHeaderTemplate(FeatureTypeInfo referenceFeatureType) throws IOException {
        return templateSupport.getTemplate(
                referenceFeatureType,
                "getfeature-header.ftl",
                GetFeatureHTMLMessageConverter.class);
    }
}

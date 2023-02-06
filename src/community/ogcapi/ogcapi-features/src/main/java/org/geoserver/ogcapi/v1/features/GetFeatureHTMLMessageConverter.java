/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi.v1.features;

import freemarker.template.Template;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractHTMLMessageConverter;
import org.geoserver.ogcapi.FreemarkerTemplateSupport;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Produces a Feature response in HTML. */
@Component
public class GetFeatureHTMLMessageConverter extends AbstractHTMLMessageConverter<FeaturesResponse> {
    static final Logger LOGGER = Logging.getLogger(GetFeatureHTMLMessageConverter.class);

    public GetFeatureHTMLMessageConverter(
            FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(FeaturesResponse.class, WFSInfo.class, templateSupport, geoServer);
    }

    private FeatureTypeInfo getResource(FeatureCollection collection) {
        FeatureTypeInfo info = null;
        if (collection instanceof TypeInfoCollectionWrapper) {
            info = ((TypeInfoCollectionWrapper) collection).getFeatureTypeInfo();
        }
        if (info == null && collection.getSchema() != null) {
            info = geoServer.getCatalog().getFeatureTypeByName(collection.getSchema().getName());
        }
        return info;
    }

    @Override
    protected void writeInternal(FeaturesResponse value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        FeatureCollectionResponse response = value.getResponse();
        GetFeatureRequest request = GetFeatureRequest.adapt(value.getRequest());

        // if there is only one feature type loaded, we allow for header/footer customization,
        // otherwise we stick with the generic ones
        FeatureTypeInfo referenceFeatureType = null;
        List<FeatureCollection> collections = response.getFeature();
        if (collections.size() == 1) {
            referenceFeatureType = getResource(collections.get(0));
        }

        Template header = getHeaderTemplate(referenceFeatureType);
        Template footer = getFooterTemplate(referenceFeatureType);

        Map<String, Object> model = new HashMap<>();
        model.put("baseURL", request.getBaseURL());
        model.put("request", request);
        model.put("response", response);
        addLinkFunctions(APIRequestInfo.get().getBaseURL(), model);

        Charset charset = getDefaultCharset();
        if (charset != null && outputMessage != null && outputMessage.getBody() != null) {
            try (OutputStreamWriter osw =
                    new OutputStreamWriter(outputMessage.getBody(), charset)) {
                templateSupport.processTemplate(header, model, osw, charset);

                // process content template for all feature collections found
                for (FeatureCollection fc : collections) {
                    if (fc != null) {
                        FeatureTypeInfo typeInfo = getResource(fc);
                        if (!fc.isEmpty()) {
                            Template content = null;
                            if (!(fc.getSchema() instanceof SimpleFeatureType)) {
                                // if there is a specific template for complex features, use that.
                                content = getComplexContentTemplate(typeInfo);
                            }
                            if (content == null) {
                                content = getContentTemplate(typeInfo);
                            }
                            model.put("featureInfo", typeInfo);
                            model.put("data", fc);
                            // allow building a collection backlink
                            if (fc instanceof TypeInfoCollectionWrapper
                                    && includeCollectionLink()) {
                                FeatureTypeInfo info =
                                        ((TypeInfoCollectionWrapper) fc).getFeatureTypeInfo();
                                if (info != null) {
                                    model.put("collection", info.prefixedName());
                                }
                            }
                            try {
                                templateSupport.processTemplate(
                                        content, model, osw, getDefaultCharset());
                            } finally {
                                model.remove("data");
                            }
                        } else {
                            Template template = getEmptyTemplate(typeInfo);
                            model.put("collection", typeInfo.prefixedName());
                            templateSupport.processTemplate(
                                    template, model, osw, getDefaultCharset());
                        }
                    }
                }

                // if a template footer was loaded (ie, there were only one feature
                // collection), process it
                if (footer != null) {
                    templateSupport.processTemplate(footer, model, osw, getDefaultCharset());
                }
                osw.flush();
            } finally {
                purgeIterators();
            }
        } else {
            LOGGER.warning(
                    "Either the default character set, output message or body was null, so the "
                            + "template could not be processed.");
        }
    }

    protected boolean includeCollectionLink() {
        // it's a API request, not a classic OGC one
        return APIRequestInfo.get() != null;
    }

    protected Template getContentTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(typeInfo, "getfeature-content.ftl", this.getClass());
    }

    protected Template getEmptyTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(typeInfo, "getfeature-empty.ftl", this.getClass());
    }

    protected Template getComplexContentTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(
                typeInfo, "getfeature-complex-content.ftl", this.getClass());
    }

    protected Template getFooterTemplate(FeatureTypeInfo referenceFeatureType) throws IOException {
        return templateSupport.getTemplate(
                referenceFeatureType, "getfeature-footer.ftl", this.getClass());
    }

    protected Template getHeaderTemplate(FeatureTypeInfo referenceFeatureType) throws IOException {
        return templateSupport.getTemplate(
                referenceFeatureType, "getfeature-header.ftl", this.getClass());
    }
}

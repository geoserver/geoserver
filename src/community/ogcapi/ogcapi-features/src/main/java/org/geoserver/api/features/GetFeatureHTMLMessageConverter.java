/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api.features;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractHTMLMessageConverter;
import org.geoserver.api.FreemarkerTemplateSupport;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Produces a Feature response in HTML. */
@Component
public class GetFeatureHTMLMessageConverter extends AbstractHTMLMessageConverter<FeaturesResponse> {

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

        try (OutputStreamWriter osw = new OutputStreamWriter(outputMessage.getBody())) {
            try {
                header.process(model, osw);
            } catch (TemplateException e) {
                String msg = "Error occurred processing header template.";
                throw (IOException) new IOException(msg).initCause(e);
            }

            // process content template for all feature collections found
            for (int i = 0; i < collections.size(); i++) {
                FeatureCollection fc = collections.get(i);
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
                        if (fc instanceof TypeInfoCollectionWrapper && includeCollectionLink()) {
                            FeatureTypeInfo info =
                                    ((TypeInfoCollectionWrapper) fc).getFeatureTypeInfo();
                            if (info != null) {
                                model.put("collection", info.prefixedName());
                            }
                        }
                        try {
                            content.process(model, osw);
                        } catch (TemplateException e) {
                            String msg =
                                    "Error occurred processing content template "
                                            + content.getName()
                                            + " for "
                                            + typeInfo.prefixedName();
                            throw (IOException) new IOException(msg).initCause(e);
                        } finally {
                            model.remove("data");
                        }
                    } else {
                        Template template = getEmptyTemplate(typeInfo);
                        model.put("collection", typeInfo.prefixedName());
                        try {
                            template.process(model, osw);
                        } catch (TemplateException e) {
                            String msg =
                                    "Error occurred processing empty template "
                                            + template.getName()
                                            + " for "
                                            + typeInfo.prefixedName();
                            throw (IOException) new IOException(msg).initCause(e);
                        }
                    }
                }
            }

            // if a template footer was loaded (ie, there were only one feature
            // collection), process it
            if (footer != null) {
                try {
                    footer.process(model, osw);
                } catch (TemplateException e) {
                    String msg = "Error occured processing footer template.";
                    throw (IOException) new IOException(msg).initCause(e);
                }
            }
            osw.flush();
        } finally {
            purgeIterators();
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

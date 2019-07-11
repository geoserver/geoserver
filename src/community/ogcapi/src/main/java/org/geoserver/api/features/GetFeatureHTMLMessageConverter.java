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
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
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

    public GetFeatureHTMLMessageConverter(GeoServerResourceLoader loader, GeoServer geoServer) {
        super(FeaturesResponse.class, WFSInfo.class, loader, geoServer);
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

        Template header =
                templateSupport.getTemplate(
                        referenceFeatureType, "getfeature-header.ftl", this.getClass());
        Template footer =
                templateSupport.getTemplate(
                        referenceFeatureType, "getfeature-footer.ftl", this.getClass());

        Map<String, Object> model = new HashMap<>();
        model.put("baseURL", request.getBaseURL());
        model.put("response", response);
        AbstractHTMLMessageConverter.addLinkFunctions(APIRequestInfo.get().getBaseURL(), model);

        try (OutputStreamWriter osw = new OutputStreamWriter(outputMessage.getBody())) {
            try {
                header.process(model, osw);
            } catch (TemplateException e) {
                String msg = "Error occurred processing header template.";
                throw (IOException) new IOException(msg).initCause(e);
            }

            // process content template for all feature collections found
            boolean version3 =
                    request.getVersion() != null && request.getVersion().startsWith("3.");
            for (int i = 0; i < collections.size(); i++) {
                FeatureCollection fc = collections.get(i);
                if (fc != null && fc.size() > 0) {
                    Template content = null;
                    FeatureTypeInfo typeInfo = getResource(fc);
                    if (!(fc.getSchema() instanceof SimpleFeatureType)) {
                        // if there is a specific template for complex features, use that.
                        content =
                                templateSupport.getTemplate(
                                        typeInfo,
                                        "getfeature-complex-content.ftl",
                                        this.getClass());
                    }
                    if (content == null) {
                        content =
                                templateSupport.getTemplate(
                                        typeInfo, "getfeature-content.ftl", this.getClass());
                    }
                    model.put("data", fc);
                    // allow building a collection backlink
                    if (version3 && fc instanceof TypeInfoCollectionWrapper) {
                        FeatureTypeInfo info =
                                ((TypeInfoCollectionWrapper) fc).getFeatureTypeInfo();
                        if (info != null) {
                            model.put("collection", NCNameResourceCodec.encode(info));
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
}

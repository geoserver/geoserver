/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import freemarker.ext.beans.BeanModel;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.FreemarkerTemplateSupport;
import org.geoserver.ogcapi.v1.features.GetFeatureHTMLMessageConverter;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.util.ISO8601Formatter;
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

    ISO8601Formatter dateFormatter = new ISO8601Formatter();

    public DGGSFeatureHTMLMessageConverter(
            FreemarkerTemplateSupport templateSupport, GeoServer geoServer) {
        super(templateSupport, geoServer);
    }

    @Override
    protected boolean canWrite(MediaType mediaType) {
        return Optional.of(APIRequestInfo.get())
                        .filter(r -> r.getRequestPath().startsWith("/ogc/dggs"))
                        .isPresent()
                && super.canWrite(mediaType);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected Template getContentTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(
                typeInfo, "zones-content.ftl", DGGSFeatureHTMLMessageConverter.class);
    }

    @Override
    protected Template getEmptyTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(
                typeInfo, "getfeature-empty.ftl", GetFeatureHTMLMessageConverter.class);
    }

    @Override
    protected Template getComplexContentTemplate(FeatureTypeInfo typeInfo) throws IOException {
        return templateSupport.getTemplate(
                typeInfo, "getfeature-complex-content.ftl", GetFeatureHTMLMessageConverter.class);
    }

    @Override
    protected Template getFooterTemplate(FeatureTypeInfo referenceFeatureType) throws IOException {
        return templateSupport.getTemplate(
                referenceFeatureType,
                "getfeature-footer.ftl",
                GetFeatureHTMLMessageConverter.class);
    }

    @Override
    protected Template getHeaderTemplate(FeatureTypeInfo referenceFeatureType) throws IOException {
        return templateSupport.getTemplate(
                referenceFeatureType,
                "getfeature-header.ftl",
                GetFeatureHTMLMessageConverter.class);
    }

    @Override
    protected void addLinkFunctions(String baseURL, Map<String, Object> model) {
        super.addLinkFunctions(baseURL, model);
        model.put("zoneLink", (TemplateMethodModelEx) this::getZoneLink);
    }

    private Object getZoneLink(List arguments) throws TemplateModelException {
        FeatureTypeInfo ft = (FeatureTypeInfo) ((BeanModel) arguments.get(0)).getWrappedObject();
        DimensionInfo time = ft.getMetadata().get(FeatureTypeInfo.TIME, DimensionInfo.class);
        SimpleHash feature = (SimpleHash) arguments.get(1);
        APIRequestInfo requestInfo = APIRequestInfo.get();
        Map<String, String> kvp = new HashMap<>();
        kvp.put("f", "html");
        Object zoneId = getAttribute(feature, "zoneId");
        if (zoneId == null) return null;
        kvp.put("zone_id", (String) zoneId);
        if (time != null) {
            if (time.getEndAttribute() == null) {
                Date date = getDateAttribute(feature, time.getAttribute());
                kvp.put("datetime", dateFormatter.format(date));
            } else {
                // Should we consider open ended ranges here? would be new
                // GeoServer wise
                Date start = getDateAttribute(feature, time.getAttribute());
                Date end = getDateAttribute(feature, time.getEndAttribute());
                kvp.put("datetime", dateFormatter.format(start) + "/" + dateFormatter.format(end));
            }
        }

        return ResponseUtils.buildURL(
                requestInfo.getBaseURL(),
                ResponseUtils.appendPath(
                        requestInfo.getServiceLandingPage(),
                        "/collections/" + feature.get("typeName") + "/zone/"),
                kvp,
                URLMangler.URLType.SERVICE);
    }

    private Date getDateAttribute(SimpleHash feature, String attribute)
            throws TemplateModelException {
        return ((SimpleDate) ((SimpleHash) feature.get(attribute)).get("rawValue")).getAsDate();
    }

    private Object getAttribute(SimpleHash feature, String attribute)
            throws TemplateModelException {
        SimpleHash hash = (SimpleHash) feature.get(attribute);
        if (hash == null) return null;
        return hash.get("rawValue").toString();
    }
}

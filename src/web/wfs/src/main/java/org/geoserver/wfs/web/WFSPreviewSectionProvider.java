/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Localizer;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.security.DisabledServiceResourceFilter;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.HomePagePreviewSectionProvider;
import org.geoserver.web.PreviewLink;
import org.geoserver.web.PreviewSectionLayout;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geotools.wfs.v1_0.WFS;

/** Contributes WFS sample/download links to the home page. */
public class WFSPreviewSectionProvider implements HomePagePreviewSectionProvider {

    @Override
    public boolean supports(PublishedInfo published) {
        return published instanceof LayerInfo layerInfo
                && layerInfo.getType() == PublishedType.VECTOR
                && hasServiceSupport(layerInfo, "WFS");
    }

    @Override
    public String getTitleKey() {
        return "vectorFormats";
    }

    @Override
    public List<PreviewLink> getLinks(PublishedInfo published) {
        String baseLink = buildWfsLink((LayerInfo) published);
        List<String> formats = new ArrayList<>();
        for (WFSGetFeatureOutputFormat producer :
                GeoServerApplication.get().getBeansOfType(WFSGetFeatureOutputFormat.class)) {
            formats.addAll(producer.getOutputFormats());
        }
        formats.sort(Comparator.comparing(format -> translate("format.wfs.", format)));

        List<PreviewLink> links = new ArrayList<>();
        String previous = null;
        for (String format : formats) {
            String label = translate("format.wfs.", format);
            if (label.equals(previous)) continue;
            previous = label;
            links.add(new PreviewLink(label, baseLink + "&outputFormat=" + format, format));
        }
        return links;
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public PreviewSectionLayout getLayout() {
        return PreviewSectionLayout.DROPDOWN;
    }

    /** Builds the base GetFeature request shared by the advertised output formats. */
    private String buildWfsLink(LayerInfo layerInfo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "WFS");
        params.put("version", WFS.getInstance().getVersion());
        params.put("request", "GetFeature");
        params.put("typeName", layerInfo.getResource().prefixedName());
        return ResponseUtils.buildURL(baseURL(), path(layerInfo, "ows"), params, URLType.SERVICE);
    }

    private boolean hasServiceSupport(LayerInfo layerInfo, String serviceName) {
        List<String> disabledServices = DisabledServiceResourceFilter.disabledServices(layerInfo.getResource());
        return disabledServices.stream().noneMatch(d -> d.equalsIgnoreCase(serviceName));
    }

    private String path(LayerInfo layerInfo, String service) {
        String workspace = layerInfo.getResource().getStore().getWorkspace().getName();
        return workspace == null ? service : workspace + "/" + service;
    }

    private String baseURL() {
        return ResponseUtils.baseURL(GeoServerApplication.get().servletRequest());
    }

    private String translate(String prefix, String format) {
        Localizer localizer = GeoServerApplication.get().getResourceSettings().getLocalizer();
        return localizer.getString(prefix + format, null, format);
    }
}

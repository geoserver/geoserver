/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.Localizer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.security.DisabledServiceResourceFilter;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.HomePagePreviewSectionProvider;
import org.geoserver.web.PreviewCatalogLinkSupport;
import org.geoserver.web.PreviewLink;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.locationtech.jts.geom.Envelope;

/** Contributes WMS format preview links to the home page. */
public class WMSPreviewSectionProvider implements HomePagePreviewSectionProvider {

    @Override
    public boolean supports(PublishedInfo published) {
        return published != null && hasServiceSupport(published, "WMS") && getWmsLink(published) != null;
    }

    @Override
    public String getTitleKey() {
        return PreviewCatalogLinkSupport.MAP_FORMATS;
    }

    @Override
    public List<PreviewLink> getLinks(PublishedInfo published) {
        String baseLink = getWmsLink(published);
        if (baseLink == null) return List.of();

        List<String> formats = new ArrayList<>();
        for (GetMapOutputFormat producer : GeoServerApplication.get().getBeansOfType(GetMapOutputFormat.class)) {
            Set<String> producerFormats = new HashSet<>(producer.getOutputFormatNames());
            producerFormats.add(producer.getMimeType());
            String knownFormat = producer.getMimeType();
            for (String formatName : producerFormats) {
                if (!formatName.equals(translate("format.wms.", formatName))) {
                    knownFormat = formatName;
                    break;
                }
            }
            formats.add(knownFormat);
        }
        formats.sort(Comparator.comparing(format -> translate("format.wms.", format)));

        List<PreviewLink> links = new ArrayList<>();
        String previous = null;
        for (String format : formats) {
            String label = translate("format.wms.", format);
            if (label.equals(previous)) continue;
            previous = label;
            links.add(new PreviewLink(label, baseLink + "&format=" + format, format));
        }
        return links;
    }

    @Override
    public int getPriority() {
        return PreviewCatalogLinkSupport.MAP_FORMATS_PRIORITY;
    }

    /** Builds the base GetMap request shared by the advertised output formats. */
    private String getWmsLink(PublishedInfo published) {
        GetMapRequest request = getRequest(published);
        Envelope bbox = request.getBbox();
        if (bbox == null) return null;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "WMS");
        params.put("version", "1.1.0");
        params.put("request", "GetMap");
        params.put("layers", getName(published));
        params.put("bbox", bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," + bbox.getMaxY());
        params.put("width", String.valueOf(request.getWidth()));
        params.put("height", String.valueOf(request.getHeight()));
        params.put("srs", String.valueOf(request.getSRS()));
        params.put(
                "styles",
                !request.getStyles().isEmpty() ? request.getStyles().get(0).getName() : "");
        return ResponseUtils.buildURL(baseURL(), path(published, "wms"), params, URLType.SERVICE);
    }

    private GetMapRequest getRequest(PublishedInfo published) {
        GetMapRequest request = new GetMapRequest();
        request.setLayers(expandLayers(GeoServerApplication.get().getCatalog(), published));
        request.setFormat("application/openlayers");
        if (published instanceof LayerGroupInfo groupInfo) {
            ReferencedEnvelope bounds = groupInfo.getBounds();
            request.setBbox(bounds);
            request.setCrs(bounds.getCoordinateReferenceSystem());
            request.setSRS(lookupSRSFromBounds(bounds));
        }
        try {
            DefaultWebMapService.autoSetBoundsAndSize(request);
        } catch (Exception e) {
            return request;
        }
        return request;
    }

    /** Expands a layer group into the individual map layers required by {@link GetMapRequest}. */
    private List<MapLayerInfo> expandLayers(Catalog catalog, PublishedInfo published) {
        List<MapLayerInfo> layers = new ArrayList<>();
        if (published instanceof LayerInfo layerInfo) {
            layers.add(new MapLayerInfo(layerInfo));
        } else if (published instanceof LayerGroupInfo groupInfo) {
            for (LayerInfo l : Iterables.filter(groupInfo.getLayers(), LayerInfo.class))
                layers.add(new MapLayerInfo(l));
        }
        return layers;
    }

    private static String lookupSRSFromBounds(ReferencedEnvelope bounds) {
        CoordinateReferenceSystem crs = bounds.getCoordinateReferenceSystem();
        if (crs == null) return null;
        try {
            return ResourcePool.lookupIdentifier(crs, false);
        } catch (FactoryException e) {
            return GML2EncodingUtils.toURI(crs, SrsSyntax.AUTH_CODE, false);
        }
    }

    private boolean hasServiceSupport(PublishedInfo published, String serviceName) {
        if (published instanceof LayerInfo layerInfo && layerInfo.getResource() != null) {
            List<String> disabledServices = DisabledServiceResourceFilter.disabledServices(layerInfo.getResource());
            return disabledServices.stream().noneMatch(d -> d.equalsIgnoreCase(serviceName));
        }
        return true;
    }

    private String getName(PublishedInfo published) {
        if (published instanceof LayerInfo layerInfo)
            return layerInfo.getResource().prefixedName();
        return published.prefixedName();
    }

    private String path(PublishedInfo published, String service) {
        String workspace = workspace(published);
        return workspace == null ? service : workspace + "/" + service;
    }

    private String workspace(PublishedInfo published) {
        if (published instanceof LayerInfo layerInfo)
            return layerInfo.getResource().getStore().getWorkspace().getName();
        if (published instanceof LayerGroupInfo groupInfo && groupInfo.getWorkspace() != null)
            return groupInfo.getWorkspace().getName();
        return null;
    }

    private String baseURL() {
        return ResponseUtils.baseURL(GeoServerApplication.get().servletRequest());
    }

    private String translate(String prefix, String format) {
        Localizer localizer = GeoServerApplication.get().getResourceSettings().getLocalizer();
        return localizer.getString(prefix + format, null, format);
    }
}

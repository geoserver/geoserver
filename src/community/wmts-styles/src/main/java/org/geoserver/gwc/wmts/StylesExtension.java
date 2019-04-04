/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.io.XMLBuilder;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.service.HttpErrorCodeException;
import org.geowebcache.service.wmts.WMTSExtensionImpl;
import org.geowebcache.storage.StorageBroker;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** WMTS extension adding support for */
public final class StylesExtension extends WMTSExtensionImpl implements ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(StylesExtension.class);

    private final Catalog catalog;

    private List<ResourceFactory> resourceFactories;

    public StylesExtension(
            Catalog catalog, Dispatcher gsDispatcher, GeoWebCacheDispatcher gwcDispatcher) {
        this.catalog = catalog;

        // Allow dispatchers to handle put and delete too
        // This should be done higher level, but it would require adding true support for
        // these methods in the associated dispatcher, which is going to require significant work,
        // at least in the case of the GeoServer one
        gsDispatcher.setSupportedMethods("GET", "POST", "DELETE", "PUT", "OPTIONS");
        gwcDispatcher.setSupportedMethods("GET", "POST", "DELETE", "PUT", "OPTIONS");
    }

    @Override
    public List<OperationMetadata> getExtraOperationsMetadata() {
        return Collections.emptyList();
    }

    @Override
    public Conveyor getConveyor(
            HttpServletRequest request, HttpServletResponse response, StorageBroker storageBroker) {
        for (ResourceFactory resourceFactory : resourceFactories) {
            final ResourceFactory.Resource resource = resourceFactory.getResourceFor(request);
            if (resource != null) {
                return new ResourceConveyor(request, response, resource);
            }
        }

        return null;
    }

    @Override
    public boolean handleRequest(Conveyor candidateConveyor) {
        if (candidateConveyor instanceof ResourceConveyor) {
            ResourceConveyor conveyor = (ResourceConveyor) candidateConveyor;

            try {
                conveyor.execute();
                return true;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not handle a style request", e);
                throw new HttpErrorCodeException(
                        INTERNAL_SERVER_ERROR.value(), "An internal error occurred");
            }
        }

        return false;
    }

    @Override
    public void encodeLayer(XMLBuilder xml, TileLayer tileLayer) throws IOException {
        final Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return;
        }
        final String baseURL = ResponseUtils.baseURL(request.getHttpRequest());
        PublishedInfo info = getPublishedInfo(tileLayer);
        if (info instanceof LayerInfo) {
            LayerInfo layer = (LayerInfo) info;

            // add ability to list and contribute new styles
            xml.startElement("ResourceURL");
            xml.attribute("resourceType", "layerStyles");
            xml.attribute("format", "text/json");
            final String template =
                    ResponseUtils.buildURL(
                            baseURL,
                            "gwc/service/wmts/reststyles/layers/"
                                    + tileLayer.getName()
                                    + "/styles/",
                            Collections.singletonMap("f", "text/json"),
                            URLMangler.URLType.SERVICE);
            xml.attribute("template", template);
            xml.endElement("ResourceURL");

            // list existing styles
            addStyleResource(tileLayer, layer.getDefaultStyle(), true, baseURL, xml);
            for (StyleInfo style : layer.getStyles()) {
                addStyleResource(tileLayer, style, false, baseURL, xml);
            }
        } else if (info instanceof LayerGroupInfo) {
            LayerGroupInfo group = (LayerGroupInfo) info;
            // it it a simple style group case?
            if (group.getLayers().size() == 1 && group.getLayers().get(0) == null) {
                final StyleInfo style = group.getStyles().get(0);
                addStyleResource(tileLayer, style, true, baseURL, xml);
            }
        }
    }

    private void addStyleResource(
            TileLayer tileLayer,
            StyleInfo style,
            boolean defaultStyle,
            String baseURL,
            XMLBuilder xml)
            throws IOException {
        for (String format : getFormatsForStyle(style)) {
            xml.startElement("ResourceURL");
            xml.attribute("resourceType", defaultStyle ? "defaultStyle" : "style");
            xml.attribute("format", format);
            final String template =
                    ResponseUtils.buildURL(
                            baseURL,
                            "gwc/service/wmts/reststyles/layers/"
                                    + tileLayer.getName()
                                    + "/styles/"
                                    + style.getName(),
                            Collections.singletonMap("f", format),
                            URLMangler.URLType.SERVICE);
            xml.attribute("template", template);
            xml.endElement("ResourceURL");
        }
    }

    private Set<String> getFormatsForStyle(StyleInfo style) {
        Set<String> result = new LinkedHashSet<>();
        // add native mime type
        final StyleHandler handler = Styles.handler(style.getFormat());
        result.add(handler.mimeType(style.getFormatVersion()));
        // can always do SLD 1.0
        result.add(SLDHandler.MIMETYPE_10);
        return result;
    }

    private PublishedInfo getPublishedInfo(TileLayer tileLayer) {
        // let's see if we can get the layer info from the tile layer
        if (tileLayer != null && tileLayer instanceof GeoServerTileLayer) {
            PublishedInfo publishedInfo = ((GeoServerTileLayer) tileLayer).getPublishedInfo();
            if (publishedInfo != null) {
                return publishedInfo;
            }
        }
        // let's see if we are in the context of a virtual service
        WorkspaceInfo localWorkspace = LocalWorkspace.get();
        String layerName = tileLayer.getName();
        if (localWorkspace != null) {
            // we need to make sure that the layer name is prefixed with the local workspace
            layerName = CatalogConfiguration.removeWorkspacePrefix(layerName, catalog);
            layerName = localWorkspace.getName() + ":" + layerName;
        }
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo != null) {
            return layerInfo;
        }
        final LayerGroupInfo layerGroup = catalog.getLayerGroupByName(layerName);
        if (layerGroup != null) {
            return layerGroup;
        }
        throw new ServiceException(String.format("Unknown layer '%s'.", layerName));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        resourceFactories = GeoServerExtensions.extensions(ResourceFactory.class);
    }
}

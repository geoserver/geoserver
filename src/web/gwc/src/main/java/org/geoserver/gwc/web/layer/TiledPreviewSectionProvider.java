/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.util.List;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.HomePagePreviewSectionProvider;
import org.geoserver.web.PreviewCatalogLinkSupport;
import org.geoserver.web.PreviewLink;
import org.geotools.util.logging.Logging;
import org.geowebcache.layer.TileLayer;

/** Contributes tiled preview links for cached layers. */
public class TiledPreviewSectionProvider implements HomePagePreviewSectionProvider {

    @Override
    public boolean supports(PublishedInfo published) {
        return published != null && GWC.get().hasTileLayer(published);
    }

    @Override
    public String getTitleKey() {
        return PreviewCatalogLinkSupport.TILED_FORMATS;
    }

    @Override
    public List<PreviewLink> getLinks(PublishedInfo published) {
        TileLayer layer;
        try {
            layer = GWC.get().getTileLayer(published);
        } catch (RuntimeException e) {
            Logging.getLogger(getClass()).fine(e.getMessage());
            return List.of();
        }
        String baseURL = ResponseUtils.baseURL(GeoServerApplication.get().servletRequest());
        return CachedLayersPage.computePreviewTargets(layer, baseURL);
    }

    @Override
    public int getPriority() {
        return PreviewCatalogLinkSupport.TILED_FORMATS_PRIORITY;
    }
}

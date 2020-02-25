/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.Link;
import org.geoserver.api.SampleDataProvider;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.mime.MimeType;
import org.springframework.stereotype.Component;

@Component
public class TilesSampleDataProvider implements SampleDataProvider {

    private final GWC gwc;
    private final TilesService service;

    TilesSampleDataProvider(GWC gwc, TilesService service) {
        this.gwc = gwc;
        this.service = service;
    }

    @Override
    public List<Link> getSampleData(LayerInfo layer) {
        if (layer.getResource() instanceof ResourceInfo && service.getService().isEnabled()) {
            GeoServerTileLayer tileLayer = gwc.getTileLayer(layer);
            // is there a cached layer configured?
            if (tileLayer != null) {
                List<MimeType> mimeTypes = tileLayer.getMimeTypes();
                // is there any vector format, that is, actual "data"?
                return mimeTypes
                        .stream()
                        .filter(m -> m.isVector())
                        .map(
                                m -> {
                                    String href =
                                            ResponseUtils.buildURL(
                                                    APIRequestInfo.get().getBaseURL(),
                                                    "ogc/tiles/collections/"
                                                            + ResponseUtils.urlEncode(
                                                                    layer.prefixedName()
                                                                            + "/tiles"),
                                                    Collections.singletonMap("f", m.getFormat()),
                                                    URLMangler.URLType.SERVICE);
                                    return new Link(
                                            href,
                                            "tiles",
                                            m.getFormat(),
                                            "Tiles as " + m.getFormat());
                                })
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}

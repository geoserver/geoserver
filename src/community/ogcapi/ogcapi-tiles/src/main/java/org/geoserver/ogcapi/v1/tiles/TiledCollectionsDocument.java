/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.gwc.GWC;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.wms.WMS;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.util.logging.Logging;
import org.geowebcache.layer.TileLayer;
import org.springframework.http.HttpStatus;

/**
 * A class representing the tiles server "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class TiledCollectionsDocument extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(TiledCollectionsDocument.class);

    private final GWC gwc;
    private final GeoServer gs;
    private final WMS wms;

    public TiledCollectionsDocument(GeoServer gs, WMS wms, GWC gwc) {
        this.gs = gs;
        this.gwc = gwc;
        this.wms = wms;

        // build the self links
        addSelfLinks("ogc/tiles/v1/collections/");
    }

    @Override
    public List<Link> getLinks() {
        return links;
    }

    public Iterator<TiledCollectionDocument> getCollections() {
        Iterator<TileLayer> tileLayers = gwc.getTileLayers().iterator();
        boolean skipInvalid =
                gs.getGlobal().getResourceErrorHandling()
                        == ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS;
        return new Iterator<TiledCollectionDocument>() {

            TiledCollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                while (tileLayers.hasNext()) {
                    TileLayer tileLayer = tileLayers.next();
                    try {
                        TiledCollectionDocument collection =
                                new TiledCollectionDocument(wms, tileLayer, true);

                        next = collection;
                        return true;
                    } catch (FactoryException | TransformException e) {
                        if (skipInvalid) {
                            LOGGER.log(Level.WARNING, "Skipping tile layer " + tileLayers);
                        } else {
                            throw new APIException(
                                    "InternalError",
                                    "Failed to iterate over the feature types in the catalog",
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    e);
                        }
                    }
                }

                return next != null;
            }

            @Override
            public TiledCollectionDocument next() {
                TiledCollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
    }
}

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;

/** Landing page for the tiles service */
public class TilesLandingPage extends AbstractLandingPageDocument {

    public static final String TILES_SERVICE_BASE = "ogc/tiles/v1";
    public static final String REL_TILING_SCHEMES =
            "http://www.opengis.net/def/rel/ogc/1.0/tiling-schemes";

    public TilesLandingPage(String title, String description) {
        super(title, description, TILES_SERVICE_BASE);

        // collections
        new LinksBuilder(TiledCollectionsDocument.class, TILES_SERVICE_BASE)
                .segment("collections")
                .title("Tiled collections metadata as ")
                .rel(Link.REL_DATA_URI)
                .add(this);

        // tile matrix sets
        new LinksBuilder(TileMatrixSets.class, TILES_SERVICE_BASE)
                .segment("tileMatrixSets")
                .title("Tile matrix set list as ")
                .rel(REL_TILING_SCHEMES)
                .add(this);
    }
}

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import org.geoserver.api.AbstractLandingPageDocument;

/** Landing page for the tiles service */
public class TilesLandingPage extends AbstractLandingPageDocument {

    public static final String TILES_SERVICE_BASE = "ogc/tiles";
    public static final String REL_TILING_SCHEMES = "tiling-schemes";

    public TilesLandingPage(String title, String description) {
        super(title, description, TILES_SERVICE_BASE);

        // collections
        addLinksFor(
                TILES_SERVICE_BASE + "/collections",
                TiledCollectionsDocument.class,
                "Tiled collections metadata as ",
                "collections",
                null,
                "data");

        // tile matrix sets
        addLinksFor(
                TILES_SERVICE_BASE + "/tileMatrixSets",
                TileMatrixSets.class,
                "Tile matrix set list as ",
                "tileMatrixSets",
                null,
                REL_TILING_SCHEMES);
    }
}

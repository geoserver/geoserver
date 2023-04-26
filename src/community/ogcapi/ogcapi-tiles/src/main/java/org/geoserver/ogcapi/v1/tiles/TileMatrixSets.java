/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.gwc.GWC;
import org.geoserver.ogcapi.AbstractDocument;
import org.geowebcache.grid.GridSetBroker;

/** Lists available tilematrix sets (as links */
public class TileMatrixSets extends AbstractDocument {

    List<TileMatrixSetDocument> tileMatrixSets = new ArrayList<>();

    public TileMatrixSets(GWC gwc) {
        // self links
        addSelfLinks("ogc/tiles/v1/tileMatrixSets");

        GridSetBroker gridSets = gwc.getGridSetBroker();
        Set<String> gridSetNames = new TreeSet<>(gridSets.getGridSetNames());
        for (String gridSetName : gridSetNames) {
            tileMatrixSets.add(new TileMatrixSetDocument(gridSets.get(gridSetName), true));
        }
    }

    public List<TileMatrixSetDocument> getTileMatrixSets() {
        return tileMatrixSets;
    }
}

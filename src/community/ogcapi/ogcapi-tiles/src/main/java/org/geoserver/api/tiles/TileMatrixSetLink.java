/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.grid.GridSubset;

public class TileMatrixSetLink {

    String tileMatrixSet;
    String tileMatrixSetURI;
    List<TileMatrixSetLimit> tileMatrixSetLimits = new ArrayList<>();

    public TileMatrixSetLink(
            String tileMatrixSet,
            String tileMatrixSetURI,
            List<TileMatrixSetLimit> tileMatrixSetLimits) {
        this.tileMatrixSet = tileMatrixSet;
        this.tileMatrixSetURI = tileMatrixSetURI;
        this.tileMatrixSetLimits = tileMatrixSetLimits;
    }

    public TileMatrixSetLink(GridSubset gridSubset) {
        this.tileMatrixSet = gridSubset.getGridSet().getName();
        String baseURL = APIRequestInfo.get().getBaseURL();
        this.tileMatrixSetURI =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/tiles/tileMatrixSets/"
                                + ResponseUtils.urlEncode(gridSubset.getGridSet().getName()),
                        null,
                        URLMangler.URLType.SERVICE);
        if (!gridSubset.fullGridSetCoverage()) {
            String[] levelNames = gridSubset.getGridNames();
            long[][] wmtsLimits = gridSubset.getWMTSCoverages();

            for (int i = 0; i < levelNames.length; i++) {
                TileMatrixSetLimit limit =
                        new TileMatrixSetLimit(
                                levelNames[i],
                                wmtsLimits[i][1],
                                wmtsLimits[i][3],
                                wmtsLimits[i][0],
                                wmtsLimits[i][2]);
                tileMatrixSetLimits.add(limit);
            }
        }
    }

    public String getTileMatrixSet() {
        return tileMatrixSet;
    }

    public void setTileMatrixSet(String tileMatrixSet) {
        this.tileMatrixSet = tileMatrixSet;
    }

    public String getTileMatrixSetURI() {
        return tileMatrixSetURI;
    }

    public void setTileMatrixSetURI(String tileMatrixSetURI) {
        this.tileMatrixSetURI = tileMatrixSetURI;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<TileMatrixSetLimit> getTileMatrixSetLimits() {
        return tileMatrixSetLimits;
    }

    public void setTileMatrixSetLimits(List<TileMatrixSetLimit> tileMatrixSetLimits) {
        this.tileMatrixSetLimits = tileMatrixSetLimits;
    }

    @Override
    public String toString() {
        return "TileMatrixSetLink{"
                + "tileMatrixSet='"
                + tileMatrixSet
                + '\''
                + ", tileMatrixSetURI='"
                + tileMatrixSetURI
                + '\''
                + ", tileMatrixSetLimits="
                + tileMatrixSetLimits
                + '}';
    }
}

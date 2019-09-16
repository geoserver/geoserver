/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.geoserver.api.AbstractDocument;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.springframework.http.MediaType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TileMatrixSetDocument extends AbstractDocument {

    private String title;
    private String abstrac;
    private String[] keywords;
    private String identifier;
    private String supportedCRS;
    private String wellKnownScaleSet;
    private final List<TileMatrixEntry> tileMatrix = new ArrayList<>();

    public TileMatrixSetDocument(GridSet gridSet, boolean summary) {
        this.identifier = gridSet.getName();
        this.title = Optional.ofNullable(gridSet.getDescription()).orElse(identifier);
        this.supportedCRS = "http://www.opengis.net/def/crs/EPSG/0/" + gridSet.getSrs().getNumber();
        // TODO: see if the current CRS matches a well konwn scale set or not

        String path = "ogc/tiles/tileMatrixSets/" + ResponseUtils.urlEncode(identifier);
        if (summary) {
            addLinksFor(
                    path,
                    TileMatrixSetDocument.class,
                    id + " definition as ",
                    "tileMatrixSet",
                    null,
                    "tileMatrixSet");
        } else {
            addSelfLinks(path, MediaType.APPLICATION_JSON);
            for (int z = 0; z < gridSet.getNumLevels(); z++) {
                tileMatrix.add(new TileMatrixEntry(gridSet, z));
            }
        }
    }

    public String getTitle() {
        return title;
    }

    public String getAbstract() {
        return abstrac;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSupportedCRS() {
        return supportedCRS;
    }

    public String getWellKnownScaleSet() {
        return wellKnownScaleSet;
    }

    public List<TileMatrixEntry> getTileMatrix() {
        return tileMatrix;
    }

    @Override
    public String getEncodedId() {
        if (identifier == null) {
            return null;
        }
        return ResponseUtils.urlEncode(identifier);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TileMatrixEntry {
        private String identifier;
        private String title;
        private String abstrac;
        private String[] keywords;
        private double scaleDenominator;
        private double[] topLeftCorner;
        private int tileWidth;
        private int tileHeight;
        private long matrixWidth;
        private long matrixHeight;

        public TileMatrixEntry(GridSet gridSet, int z) {
            Grid grid = gridSet.getGrid(z);
            this.identifier = grid.getName();
            this.scaleDenominator = grid.getScaleDenominator();
            this.topLeftCorner = gridSet.getOrderedTopLeftCorner(z);
            this.tileWidth = gridSet.getTileWidth();
            this.tileHeight = gridSet.getTileHeight();
            this.matrixWidth = grid.getNumTilesWide();
            this.matrixHeight = grid.getNumTilesHigh();
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getTitle() {
            return title;
        }

        public String getAbstract() {
            return abstrac;
        }

        public String[] getKeywords() {
            return keywords;
        }

        public double getScaleDenominator() {
            return scaleDenominator;
        }

        public double[] getTopLeftCorner() {
            return topLeftCorner;
        }

        public int getTileWidth() {
            return tileWidth;
        }

        public int getTileHeight() {
            return tileHeight;
        }

        public long getMatrixWidth() {
            return matrixWidth;
        }

        public long getMatrixHeight() {
            return matrixHeight;
        }
    }
}

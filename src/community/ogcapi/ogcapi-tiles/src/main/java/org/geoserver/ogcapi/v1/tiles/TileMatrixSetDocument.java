/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.springframework.http.MediaType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TileMatrixSetDocument extends AbstractDocument {

    private String title;
    private String abstrac;
    private String[] keywords;
    private String supportedCRS;
    private String wellKnownScaleSet;
    private final List<TileMatrixEntry> tileMatrices = new ArrayList<>();

    public TileMatrixSetDocument(GridSet gridSet, boolean summary) {
        this.id = gridSet.getName();
        this.title = Optional.ofNullable(gridSet.getDescription()).orElse(id);
        if (gridSet.getSrs().getNumber() == 4326) {
            this.supportedCRS = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        } else {
            this.supportedCRS =
                    "http://www.opengis.net/def/crs/EPSG/0/" + gridSet.getSrs().getNumber();
        }
        // TODO: see if the current CRS matches a well konwn scale set or not

        String path = "ogc/tiles/v1/tileMatrixSets/" + ResponseUtils.urlEncode(id);
        if (summary) {
            new LinksBuilder(TileMatrixSetDocument.class)
                    .segment(path)
                    .title(id + " definition as ")
                    .rel("tileMatrixSet")
                    .add(this);
        } else {
            addSelfLinks(path, MediaType.APPLICATION_JSON);
            for (int z = 0; z < gridSet.getNumLevels(); z++) {
                tileMatrices.add(new TileMatrixEntry(gridSet, z));
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

    public String getSupportedCRS() {
        return supportedCRS;
    }

    public String getWellKnownScaleSet() {
        return wellKnownScaleSet;
    }

    public List<TileMatrixEntry> getTileMatrices() {
        return tileMatrices;
    }

    @Override
    public String getEncodedId() {
        if (id == null) {
            return null;
        }
        return ResponseUtils.urlEncode(id);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TileMatrixEntry {
        private String id;
        private String title;
        private String abstrac;
        private String[] keywords;
        private double scaleDenominator;
        private String cornerOfOrigin = "topLeft";
        private double[] pointOfOrigin;
        private int tileWidth;
        private int tileHeight;
        private long matrixWidth;
        private long matrixHeight;

        public TileMatrixEntry(GridSet gridSet, int z) {
            Grid grid = gridSet.getGrid(z);
            this.id = grid.getName();
            this.scaleDenominator = grid.getScaleDenominator();
            this.pointOfOrigin = gridSet.getOrderedTopLeftCorner(z);
            this.tileWidth = gridSet.getTileWidth();
            this.tileHeight = gridSet.getTileHeight();
            this.matrixWidth = grid.getNumTilesWide();
            this.matrixHeight = grid.getNumTilesHigh();
        }

        public String getId() {
            return id;
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

        public double[] getPointOfOrigin() {
            return pointOfOrigin;
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

        public String getCornerOfOrigin() {
            return cornerOfOrigin;
        }
    }
}

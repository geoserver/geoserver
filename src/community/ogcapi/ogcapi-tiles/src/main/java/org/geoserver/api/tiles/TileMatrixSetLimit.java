/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

public class TileMatrixSetLimit {

    String tileMatrix;
    long minTileRow;
    long maxTileRow;
    long minTileCol;
    long maxTileCol;

    public TileMatrixSetLimit(
            String tileMatrix, long minTileRow, long maxTileRow, long minTileCol, long maxTileCol) {
        this.tileMatrix = tileMatrix;
        this.minTileRow = minTileRow;
        this.maxTileRow = maxTileRow;
        this.minTileCol = minTileCol;
        this.maxTileCol = maxTileCol;
    }

    public String getTileMatrix() {
        return tileMatrix;
    }

    public void setTileMatrix(String tileMatrix) {
        this.tileMatrix = tileMatrix;
    }

    public long getMinTileRow() {
        return minTileRow;
    }

    public void setMinTileRow(long minTileRow) {
        this.minTileRow = minTileRow;
    }

    public long getMaxTileRow() {
        return maxTileRow;
    }

    public void setMaxTileRow(long maxTileRow) {
        this.maxTileRow = maxTileRow;
    }

    public long getMinTileCol() {
        return minTileCol;
    }

    public void setMinTileCol(long minTileCol) {
        this.minTileCol = minTileCol;
    }

    public long getMaxTileCol() {
        return maxTileCol;
    }

    public void setMaxTileCol(long maxTileCol) {
        this.maxTileCol = maxTileCol;
    }

    @Override
    public String toString() {
        return "TileMatrixSetLimit{"
                + "tileMatrix='"
                + tileMatrix
                + '\''
                + ", minTileRow="
                + minTileRow
                + ", maxTileRow="
                + maxTileRow
                + ", minTileCol="
                + minTileCol
                + ", maxTileCol="
                + maxTileCol
                + '}';
    }
}

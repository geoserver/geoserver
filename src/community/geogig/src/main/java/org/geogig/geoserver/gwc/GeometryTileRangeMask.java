/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.gwc;

import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.storage.TileRangeMask;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

class GeometryTileRangeMask implements TileRangeMask {

    private Geometry geometryMask;

    private GridSubset gridSubset;

    private long[][] byLevelTileCoverage;

    GeometryTileRangeMask(
            Geometry geometryMask, GridSubset gridSubset, long[][] byLevelTileCoverage) {
        this.geometryMask = geometryMask;
        this.gridSubset = gridSubset;
        this.byLevelTileCoverage = byLevelTileCoverage;
    }

    @Override
    public long[][] getGridCoverages() {
        return byLevelTileCoverage.clone();
    }

    @Override
    public boolean lookup(final long tileX, final long tileY, final int level) {

        final long[] levelCoverage = getGridCoverages()[level];
        final long minxTileX = levelCoverage[0];
        final long maxTileX = levelCoverage[2];
        final long minTileY = levelCoverage[1];
        final long maxTileY = levelCoverage[3];
        if (tileX < minxTileX || tileX > maxTileX || tileY < minTileY || tileY > maxTileY) {
            return false;
        }

        long[] tileIndex = new long[] {tileX, tileY, level};
        Envelope tileBounds = toEnvelope(gridSubset.boundsFromIndex(tileIndex));
        /*
         * Instead of "resampling"/buffering the geometry which can be time/heap consuming, increase
         * the size of the tile bounds by the length of a tile on each direction
         */
        tileBounds.expandBy(tileBounds.getWidth(), tileBounds.getHeight());

        Geometry expandedTileBoundsGeom = geometryMask.getFactory().toGeometry(tileBounds);

        boolean intersects = geometryMask.intersects(expandedTileBoundsGeom);

        return intersects;
    }

    public static TileRangeMask build(
            GeoServerTileLayer tileLayer, GridSubset gridSubset, Geometry geomInGridsetCrs) {

        BoundingBox maskBounds = toBoundingBox(geomInGridsetCrs.getEnvelopeInternal());
        long[][] byLevelTileCoverage = gridSubset.getCoverageIntersections(maskBounds);

        return new GeometryTileRangeMask(geomInGridsetCrs, gridSubset, byLevelTileCoverage);
    }

    private static BoundingBox toBoundingBox(Envelope env) {
        return new BoundingBox(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
    }

    private static Envelope toEnvelope(BoundingBox bounds) {
        return new Envelope(bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY());
    }
}

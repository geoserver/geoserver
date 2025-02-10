package org.geoserver.mapml;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.AxisDirection;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wmts.client.WMTSTileIdentifier;
import org.geotools.ows.wmts.client.WMTSTileService;
import org.geotools.ows.wmts.client.WMTSZoomLevel;
import org.geotools.ows.wmts.model.TileMatrix;
import org.geotools.tile.Tile;
import org.geotools.tile.TileFactory;
import org.geotools.tile.TileIdentifier;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.ZoomLevel;

public class MapMLTileFactory extends TileFactory {
    @Override
    public Tile create(TileIdentifier identifier, TileService service) {
        return null;
    }

    @Override
    public Tile findTileAtCoordinate(double lon, double lat, ZoomLevel zoomLevel, TileService service) {
        return null;
    }

    @Override
    public ZoomLevel getZoomLevel(int zoomLevel, TileService service) {
        return null;
    }

    @Override
    public Tile findRightNeighbour(Tile tile, TileService service) {
        return null;
    }

    @Override
    public Tile findLowerNeighbour(Tile tile, TileService service) {
        return null;
    }

    public static ReferencedEnvelope getExtentFromTileName(MapMLTileIdentifier tileIdentifier, TileService service) {
        MapMLTileService mapmlTileService = (MapMLTileService) service;

        CoordinateReferenceSystem crs = mapmlTileService.getProjectedTileCrs();
        CoordinateSystem coordinateSystem = crs.getCoordinateSystem();

        double pixelSpan = mapmlTileService.getPixelSpan(tileIdentifier.getZ());
        double tileSpanY = (mapmlTileService.getTileHeight() * pixelSpan);
        double tileSpanX = (mapmlTileService.getTileWidth() * pixelSpan);

        double tileMatrixMinX;
        double tileMatrixMaxY;
        boolean longFirst = coordinateSystem.getAxis(0).getDirection().equals(AxisDirection.EAST);
        if (longFirst) {
            tileMatrixMinX = mapmlTileService.getOriginX();
            tileMatrixMaxY = mapmlTileService.getOriginY();
        } else {
            tileMatrixMaxY = mapmlTileService.getOriginX();
            tileMatrixMinX = mapmlTileService.getOriginY();
        }
        ReferencedEnvelope ret = new ReferencedEnvelope(crs);
        double minX = tileIdentifier.getX() * tileSpanX + tileMatrixMinX;
        double maxY = tileMatrixMaxY - tileIdentifier.getY() * tileSpanY;
        double maxX = minX + tileSpanX;
        double minY = maxY - tileSpanY;
        if (longFirst) {
            ret.expandToInclude(minX, minY);
            ret.expandToInclude(maxX, maxY);
        } else {
            ret.expandToInclude(minY, minX);
            ret.expandToInclude(maxY, maxX);
        }

        return ret;
    }
}

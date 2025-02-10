package org.geoserver.mapml;

import org.geotools.tile.Tile;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.ZoomLevel;

import java.net.URL;

public class MapMLTile extends Tile {
    private URL url = null;

    public MapMLTile(int x, int y, ZoomLevel zoomLevel, TileService service) {
        this(new MapMLTileIdentifier(x, y, zoomLevel, service.getName()), service);
    }

    public MapMLTile(MapMLTileIdentifier tileIdentifier, TileService service) {
        super(
                tileIdentifier,
                MapMLTileFactory.getExtentFromTileName(tileIdentifier, service),
                ((MapMLTileService) service).getTileWidth(),
                service);
    }

    private MapMLTileService getService() {
        return (MapMLTileService) service;
    }

    @Override
    public URL getUrl() {
        if (url == null) {
            url = getService().createURL(this);
        }
        return url;
    }
}

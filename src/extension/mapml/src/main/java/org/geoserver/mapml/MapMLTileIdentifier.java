package org.geoserver.mapml;

import org.geotools.tile.TileIdentifier;
import org.geotools.tile.impl.ZoomLevel;

public class MapMLTileIdentifier extends TileIdentifier {
    public MapMLTileIdentifier(int x, int y, ZoomLevel zoomLevel, String serviceName) {
        super(x, y, zoomLevel, serviceName);
    }

    @Override
    public String getId() {
        final String separator = "_";
        StringBuilder sb = createGenericCodeBuilder(separator);
        sb.insert(0, separator).insert(0, getServiceName());
        return sb.toString();
    }

    @Override
    public String getCode() {
        final String separator = "/";
        return createGenericCodeBuilder(separator).toString();
    }

    private StringBuilder createGenericCodeBuilder(final String separator) {
        StringBuilder sb = new StringBuilder(50);
        sb.append(getZ()).append(separator).append(getX()).append(separator).append(getY());

        return sb;
    }

    @Override
    public TileIdentifier getRightNeighbour() {
        int newX = getX() + 1;
        if (newX >= getZoomLevel().getMaxTilePerRowNumber()) return null;
        else return new MapMLTileIdentifier(newX, getY(), getZoomLevel(), getServiceName());
    }

    @Override
    public TileIdentifier getLowerNeighbour() {
        int newY = getY() + 1;
        if (newY >= getZoomLevel().getMaxTilePerColNumber()) return null;
        else return new MapMLTileIdentifier(getX(), newY, getZoomLevel(), getServiceName());
    }
}

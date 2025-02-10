package org.geoserver.mapml;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.AxisDirection;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.tile.TileFactory;
import org.geotools.tile.TileIdentifier;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.ZoomLevel;
import org.geowebcache.grid.GridSet;
import si.uom.NonSI;
import si.uom.SI;

import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import java.util.logging.Level;

public class MapMLTileService extends TileService {

    GridSet gridSet;
    double[] scaleList;
    ReferencedEnvelope bounds;
    CoordinateReferenceSystem projectedTileCrs;

    public MapMLTileService(GridSet gridSet) throws FactoryException {
        super("mapml");
        this.gridSet = gridSet;
        scaleList = new double[gridSet.getNumLevels()];
        for (int i = 0; i < gridSet.getNumLevels(); i++) {
            scaleList[i] = gridSet.getGrid(i).getScaleDenominator();
        }
        projectedTileCrs = CRS.decode(gridSet.getSrs().toString());

        bounds = new ReferencedEnvelope(
                gridSet.getBounds().getMinX(),
                gridSet.getBounds().getMaxX(),
                gridSet.getBounds().getMinY(),
                gridSet.getBounds().getMaxY(),
                projectedTileCrs);
    }

    @Override
    public TileIdentifier identifyTileAtCoordinate(double lon, double lat, ZoomLevel zoomLevel) {
        double pixelSpan = getPixelSpan(zoomLevel.getZoomLevel());
        double tileSpanY = (gridSet.getTileHeight() * pixelSpan);
        double tileSpanX = (gridSet.getTileWidth() * pixelSpan);
        double tileMatrixMinX;
        double tileMatrixMaxY;
        double[] origin = gridSet.tileOrigin();
        if (projectedTileCrs.getCoordinateSystem().getAxis(0).getDirection().equals(AxisDirection.EAST)) {
            tileMatrixMinX = origin[0];
            tileMatrixMaxY = origin[1];
        } else {
            tileMatrixMaxY = origin[0];
            tileMatrixMinX = origin[1];
        }
        // to compensate for floating point computation inaccuracies
        double epsilon = 1e-6;
        long xTile = (int) Math.floor((lon - tileMatrixMinX) / tileSpanX + epsilon);
        long yTile = (int) Math.floor((tileMatrixMaxY - lat) / tileSpanY + epsilon);

        // sanitize
        xTile = Math.max(0, xTile);
        yTile = Math.max(0, yTile);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("identifyTile: (lon,lat)=("
                    + lon
                    + ","
                    + lat
                    + ")  (col,row)="
                    + xTile
                    + ", "
                    + yTile
                    + " zoom:"
                    + zoomLevel.getZoomLevel());
        }

        return new MapMLTileIdentifier((int) xTile, (int) yTile, zoomLevel, getName());
    }

    @Override
    public double[] getScaleList() {
        return scaleList;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return bounds;
    }

    @Override
    public CoordinateReferenceSystem getProjectedTileCrs() {
        return projectedTileCrs;
    }

    @Override
    public int getTileWidth() {
        return gridSet.getTileWidth();
    }

    @Override
    public int getTileHeight() {
        return gridSet.getTileHeight();
    }

    @Override
    public TileFactory getTileFactory() {
        return null;
    }

    public double getPixelSpan(int zoomLevel) {
        CoordinateSystem coordinateSystem = projectedTileCrs.getCoordinateSystem();
        @SuppressWarnings("unchecked")
        Unit<Length> unit = (Unit<Length>) coordinateSystem.getAxis(0).getUnit();

        // now divide by meters per unit!
        double pixelSpan = scaleList[zoomLevel] * gridSet.getMetersPerUnit();
        if (unit.equals(NonSI.DEGREE_ANGLE)) {
            /*
             * use the length of a degree at the equator = 60 nautical miles!
             * unit = USCustomary.NAUTICAL_MILE; UnitConverter metersperunit =
             * unit.getConverterTo(SI.METRE); pixelSpan /=
             * metersperunit.convert(60.0);
             */

            // constant value from
            // https://msi.nga.mil/MSISiteContent/StaticFiles/Calculators/degree.html
            // apparently - 60.10764611706782 NaMiles
            pixelSpan /= 111319;
        } else {
            UnitConverter metersperunit = unit.getConverterTo(SI.METRE);
            pixelSpan /= metersperunit.convert(1);
        }
        return pixelSpan;
    }

    public double getOriginY() {
        return gridSet.tileOrigin()[1];
    }

    public double getOriginX() {
        return gridSet.tileOrigin()[0];
    }
}

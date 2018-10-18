/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import java.util.List;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

class GridSetBuilder {

    public static GridSet build(final GridSetInfo info) throws IllegalStateException {

        String name = checkNotNull(info.getName(), "Name is not set");
        CoordinateReferenceSystem crs = checkNotNull(info.getCrs(), "CRS is not set");
        String epsgCode = checkNotNull(CRS.toSRS(crs, false), "EPSG code not found for CRS");
        if (!epsgCode.startsWith("EPSG:")) {
            throw new IllegalStateException(
                    "EPSG code didn't resolve to a EPSG:XXX identifier: " + epsgCode);
        }

        SRS srs;
        try {
            srs = SRS.getSRS(epsgCode);
        } catch (GeoWebCacheException e) {
            throw new IllegalStateException(e.getMessage());
        }

        ReferencedEnvelope bounds = checkNotNull(info.getBounds(), "Bounds not set");
        if (bounds.isNull()) {
            throw new IllegalArgumentException("Bounds can't be null");
        }
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            throw new IllegalArgumentException(
                    "Bounds can't be empty. Witdh: "
                            + bounds.getWidth()
                            + ". Height: "
                            + bounds.getHeight());
        }

        BoundingBox extent =
                new BoundingBox(
                        bounds.getMinimum(0),
                        bounds.getMinimum(1),
                        bounds.getMaximum(0),
                        bounds.getMaximum(1));

        boolean alignTopLeft = info.isAlignTopLeft();

        final List<Grid> levels = checkNotNull(info.getLevels(), "GridSet levels not set");
        double[] resolutions;
        double[] scaleDenoms;
        if (info.isResolutionsPreserved()) {
            resolutions = resolutions(levels);
            scaleDenoms = null;
        } else {
            resolutions = null;
            scaleDenoms = scaleDenominators(levels);
        }
        String[] scaleNames = scaleNames(levels);

        final Double metersPerUnit =
                checkNotNull(info.getMetersPerUnit(), "Meters per unit not set");
        final double pixelSize = GridSetFactory.DEFAULT_PIXEL_SIZE_METER;
        final int tileWidth = info.getTileWidth();
        final int tileHeight = info.getTileHeight();
        // if CRS axis order is NORTH_EAST (y,x) set to true, else false
        boolean yCoordinateFirst = false;
        try {
            CoordinateReferenceSystem crsNoForceOrder = CRS.decode("urn:ogc:def:crs:" + epsgCode);
            yCoordinateFirst = CRS.getAxisOrder(crsNoForceOrder) == CRS.AxisOrder.NORTH_EAST;
        } catch (FactoryException e) {
            throw new IllegalStateException(
                    "EPSG code didn't resolve to a EPSG:XXX identifier: " + epsgCode);
        }
        // create GridSet
        GridSet gridSet =
                GridSetFactory.createGridSet(
                        name,
                        srs,
                        extent,
                        alignTopLeft,
                        resolutions,
                        scaleDenoms,
                        metersPerUnit,
                        pixelSize,
                        scaleNames,
                        tileWidth,
                        tileHeight,
                        yCoordinateFirst);

        gridSet.setDescription(info.getDescription());

        return gridSet;
    }

    private static String[] scaleNames(List<Grid> levels) {
        String[] scaleNames = new String[levels.size()];
        for (int i = 0; i < scaleNames.length; i++) {
            scaleNames[i] = levels.get(i).getName();
        }
        return scaleNames;
    }

    private static double[] resolutions(List<Grid> levels) {
        double[] resolutions = new double[levels.size()];
        for (int i = 0; i < resolutions.length; i++) {
            resolutions[i] = levels.get(i).getResolution();
        }
        return resolutions;
    }

    private static double[] scaleDenominators(List<Grid> levels) {
        double[] scales = new double[levels.size()];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = levels.get(i).getScaleDenominator();
        }
        return scales;
    }

    private static <T extends Object> T checkNotNull(final T val, final String msg)
            throws IllegalStateException {
        if (val == null) {
            throw new IllegalStateException(msg);
        }
        return val;
    }
}

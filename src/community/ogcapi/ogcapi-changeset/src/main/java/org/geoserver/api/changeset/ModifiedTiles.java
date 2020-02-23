/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.api.APIException;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.layer.TileLayer;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;

/**
 * Allows access to lists of modified tiles to a given changeset, for encoding and counting purposes
 */
public class ModifiedTiles {
    private final CoverageInfo coverageInfo;
    private final TileLayer tileLayer;
    private final int zoomStart;
    private final int zoomEnd;
    private final GridSubset gridSubset;
    private List<GridSubset> subsets = new ArrayList<>();

    public ModifiedTiles(
            CoverageInfo coverageInfo,
            TileLayer tileLayer,
            GridSubset gridSet,
            SimpleFeatureCollection changes,
            ReferencedEnvelope[] boundingBoxes,
            NumberRange<Double> scaleDenominatorRange)
            throws FactoryException, IOException {
        this.coverageInfo = coverageInfo;
        this.tileLayer = tileLayer;
        this.gridSubset = gridSet;

        CoordinateReferenceSystem gridsetCrs =
                CRS.decode("EPSG:" + gridSet.getSRS().getNumber(), true);
        List<ReferencedEnvelope> bboxesInGridsetCrs = transformBounds(boundingBoxes, gridsetCrs);

        this.zoomStart =
                scaleDenominatorRange == null
                        ? 0
                        : getMinZoom(gridSet, scaleDenominatorRange.getMaximum());
        this.zoomEnd =
                scaleDenominatorRange == null
                        ? gridSet.getZoomStop()
                        : getMaxZoom(gridSet, scaleDenominatorRange.getMinimum());

        fillGridSubsets(gridSet, changes, gridsetCrs, bboxesInGridsetCrs);
    }

    private void fillGridSubsets(
            GridSubset gridSubset,
            SimpleFeatureCollection changes,
            CoordinateReferenceSystem gridsetCrs,
            List<ReferencedEnvelope> bboxesInGridsetCrs)
            throws FactoryException, IOException {
        CoordinateReferenceSystem geometryCRS = changes.getSchema().getCoordinateReferenceSystem();
        MathTransform changesToGridset = CRS.findMathTransform(geometryCRS, gridsetCrs);
        changes.accepts(
                new FeatureVisitor() {
                    @Override
                    public void visit(Feature feature) {
                        Geometry geometry =
                                (Geometry) ((SimpleFeature) feature).getDefaultGeometry();

                        try {
                            Geometry transformed = JTS.transform(geometry, changesToGridset);
                            if (bboxesInGridsetCrs == null || bboxesInGridsetCrs.isEmpty()) {
                                subsets.add(
                                        toGridSubset(
                                                gridSubset.getGridSet(),
                                                transformed.getEnvelopeInternal(),
                                                zoomStart,
                                                zoomEnd));
                            } else {
                                for (ReferencedEnvelope bbox : bboxesInGridsetCrs) {
                                    ReferencedEnvelope intersection =
                                            bbox.intersection(transformed.getEnvelopeInternal());
                                    if (!intersection.isEmpty()) {
                                        subsets.add(
                                                toGridSubset(
                                                        gridSubset.getGridSet(),
                                                        intersection,
                                                        zoomStart,
                                                        zoomEnd));
                                    }
                                }
                            }
                        } catch (TransformException e) {
                            throw new APIException(
                                    "InternalError",
                                    "Failed to compute modified tiles sets",
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    e);
                        }
                    }
                },
                null);
    }

    private int getMinZoom(GridSubset subset, double scaleRangeMinimum) {
        if (scaleRangeMinimum <= 0) {
            return 0;
        }
        int z = subset.getZoomStop();
        for (; z >= subset.getZoomStart(); z--) {
            Grid grid = subset.getGridSet().getGrid(z);
            if (grid.getScaleDenominator() > scaleRangeMinimum) {
                break;
            }
        }

        return z > 0 ? z + 1 : 0;
    }

    private int getMaxZoom(GridSubset subset, double scaleRangeMaximum) {
        int z = subset.getZoomStart();
        for (; z <= subset.getZoomStop(); z++) {
            Grid grid = subset.getGridSet().getGrid(z);
            if (grid.getScaleDenominator() < scaleRangeMaximum) {
                break;
            }
        }

        return z < subset.getZoomStop() ? z - 1 : subset.getZoomStop();
    }

    private List<ReferencedEnvelope> transformBounds(
            ReferencedEnvelope[] boundingBoxes, CoordinateReferenceSystem crs) {
        if (boundingBoxes == null || boundingBoxes.length == 0) {
            return null;
        }

        return Stream.of(boundingBoxes)
                .map(
                        b -> {
                            try {
                                return b.transform(crs, true);
                            } catch (Exception e) {
                                throw new APIException(
                                        "InternalError",
                                        "Failed to transform requested bbox in native CRS: " + crs,
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        e);
                            }
                        })
                .collect(Collectors.toList());
    }

    private GridSubset toGridSubset(
            GridSet gridSet, Envelope envelope, int zoomStart, int zoomEnd) {
        BoundingBox bbox =
                new BoundingBox(
                        envelope.getMinX(),
                        envelope.getMinY(),
                        envelope.getMaxX(),
                        envelope.getMaxY());
        return GridSubsetFactory.createGridSubSet(gridSet, bbox, zoomStart, zoomEnd);
    }

    /**
     * Iterates over all the tiles, in all zoom levels, affected by the list of changes. The
     * positions are [x, y, z] with coordinates in the GWC internal order
     */
    public Iterator<long[]> getTiles() {
        return new TileIterator(subsets, zoomStart, zoomEnd);
    }

    /** Retruns the number of tiles modified by the list of changes */
    public long getModifiedTiles() {
        // dump implementation, just count while iterating, surely possible
        // to compute it faster by just counting the tiles in each subset, but
        // one would have to account for overlaps. Not trivial, doing the dumb thing now
        Iterator<long[]> tiles = getTiles();
        long count = 0;
        while (tiles.hasNext()) {
            tiles.next();
            count++;
        }

        return count;
    }

    public TileLayer getTileLayer() {
        return tileLayer;
    }

    public GridSubset getGridSubset() {
        return gridSubset;
    }
}

/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import it.geosolutions.jaiext.range.Range;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.wps.WPSException;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.geometry.Position;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.image.ImageWorker;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;

/** A process that returns values from a coverage at a given location */
@DescribeProcess(title = "GetCoveragesValue", description = "Returns values from a coverage at a given location")
public class GetCoveragesValue implements GeoServerProcess {
    private final Catalog catalog;

    public GetCoveragesValue(Catalog catalog) {
        this.catalog = catalog;
    }

    @DescribeResult(name = "result", description = "Output value or values at location", type = ValuesAtPoint.class)
    public ValuesAtPoint execute(
            @DescribeParameter(
                            name = "name",
                            description =
                                    "Comma delimited names of rasters, optionally fully qualified (workspace:name), will check for the first one that intersects the point and returns a value")
                    String name,
            @DescribeParameter(name = "x", description = "x coordinate of the location to sample") double x,
            @DescribeParameter(name = "y", description = "y coordinate of the location to sample") double y,
            @DescribeParameter(
                            name = "crs",
                            description =
                                    "Coordinate Reference System of the x and y coordinates, defaults to first coverage CRS if not set",
                            min = 0,
                            max = 1)
                    String crs)
            throws IOException {
        CoverageInfo ci = null;
        CoordinateReferenceSystem coordinateReferenceSystem = null;
        String[] names = name.split(",");
        if (names.length == 0) {
            throw new WPSException("No coverage names provided");
        }
        ci = catalog.getCoverageByName(names[0]);
        if (ci == null) {
            throw new WPSException("Could not find coverage " + names[0]);
        }
        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        GridCoverage2DReader reader2d = (GridCoverage2DReader) reader;
        GridCoverage2D gridCoverage2D = null;
        boolean nodata = false;
        Number[] evaluationValue = null;
        try {
            coordinateReferenceSystem =
                    crs != null && !crs.isEmpty() ? CRS.decode(crs) : reader2d.getCoordinateReferenceSystem();
        } catch (FactoryException e) {
            throw new WPSException("Could not decode CRS " + crs, e);
        }
        Position position = new Position2D(coordinateReferenceSystem, x, y);
        if (!coordinateReferenceSystem.equals(reader2d.getCoordinateReferenceSystem())) {
            try {
                MathTransform transform =
                        CRS.findMathTransform(coordinateReferenceSystem, reader2d.getCoordinateReferenceSystem(), true);
                transform.transform(position, position);
            } catch (FactoryException | TransformException e) {
                throw new WPSException("Unable to convert select coordinate reference system to grid CRS ", e);
            }
        }
        for (String n : names) {
            ci = catalog.getCoverageByName(n);
            if (ci == null) {
                throw new WPSException("Could not find coverage " + n);
            }
            reader = ci.getGridCoverageReader(null, null);
            reader2d = (GridCoverage2DReader) reader;
            if (reader2d.getOriginalEnvelope().contains(position)) {
                try {
                    gridCoverage2D = getGridCoverage2D(reader2d, position);
                } catch (TransformException e) {
                    throw new WPSException("Could not transform point " + position, e);
                }
                evaluationValue = convertPrimitiveArrayToNumberArray(gridCoverage2D.evaluate(position));
                nodata = checkNoData(gridCoverage2D, evaluationValue);
                if (!nodata) {
                    break;
                }
            }
        }
        if (gridCoverage2D == null || nodata) {
            return new ValuesAtPoint(new Double[] {});
        }

        return new ValuesAtPoint(evaluationValue);
    }

    private static GridCoverage2D getGridCoverage2D(GridCoverage2DReader reader, Position position)
            throws IOException, TransformException {
        GridCoverage2D gridCoverage2D;

        Format format = reader.getFormat();
        final ParameterValueGroup readParams = format.getReadParameters();
        final Map<String, Serializable> parameters = CoverageUtils.getParametersKVP(readParams);

        // Calculate the bbox around the point
        MathTransform worldToGridCorner =
                reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER).inverse();
        Coordinate coordinate = new Coordinate(position.getCoordinate()[0], position.getCoordinate()[1]);
        JTS.transform(coordinate, coordinate, worldToGridCorner);
        GridEnvelope2D testRange =
                new GridEnvelope2D((int) (coordinate.getX() - 1), (int) (coordinate.getY() - 1), 2, 2);

        // Calculate the world bounds of the buffered point
        final MathTransform gridToWorldCorner = reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
        final GeneralBounds worldBounds = CRS.transform(gridToWorldCorner, new GeneralBounds(testRange.getBounds()));
        worldBounds.setCoordinateReferenceSystem(reader.getCoordinateReferenceSystem());

        // make sure mosaics with many superimposed tiles won't blow up with
        // a "too many open files" exception
        String maxAllowedTiles = ImageMosaicFormat.MAX_ALLOWED_TILES.getName().toString();
        if (parameters.keySet().contains(maxAllowedTiles)) {
            parameters.put(maxAllowedTiles, 1);
        }

        // Since the read sample image won't be greater than 5x5 pixels and we are limiting the
        // number of granules to 1, we may do direct read instead of using JAI
        String useJaiImageRead = ImageMosaicFormat.USE_JAI_IMAGEREAD.getName().toString();
        if (parameters.keySet().contains(useJaiImageRead)) {
            parameters.put(useJaiImageRead, false);
        }
        parameters.put(
                AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(),
                new GridGeometry2D(testRange, worldBounds));
        gridCoverage2D = reader.read(CoverageUtils.getParameters(readParams, parameters, true));
        return gridCoverage2D;
    }

    private boolean checkNoData(GridCoverage2D gridCoverage2D, Number[] evaluationValue) {
        RenderedImage ri = gridCoverage2D.getRenderedImage();
        ImageWorker worker = new ImageWorker(ri);
        Range nodata = worker.getNoData();
        int i = 0;
        if (nodata != null) {
            for (Number value : evaluationValue) {
                if (nodata.contains(value.doubleValue())) {
                    i++;
                }
            }
            if (i == evaluationValue.length) {
                return true;
            }
        }

        return false;
    }

    private Number[] convertPrimitiveArrayToNumberArray(Object evaluate) {
        if (evaluate instanceof double[]) {
            double[] doubleArray = (double[]) evaluate;
            Number[] numberArray = new Number[doubleArray.length];
            for (int i = 0; i < doubleArray.length; i++) {
                numberArray[i] = doubleArray[i];
            }
            return numberArray;
        } else if (evaluate instanceof int[]) {
            int[] intArray = (int[]) evaluate;
            Number[] numberArray = new Number[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                numberArray[i] = intArray[i];
            }
            return numberArray;
        } else if (evaluate instanceof float[]) {
            float[] floatArray = (float[]) evaluate;
            Number[] numberArray = new Number[floatArray.length];
            for (int i = 0; i < floatArray.length; i++) {
                numberArray[i] = floatArray[i];
            }
            return numberArray;
        } else if (evaluate instanceof byte[]) {
            byte[] byteArray = (byte[]) evaluate;
            Number[] numberArray = new Number[byteArray.length];
            for (int i = 0; i < byteArray.length; i++) {
                numberArray[i] = byteArray[i];
            }
            return numberArray;
        } else {
            throw new WPSException("Unsupported data type: " + evaluate.getClass());
        }
    }

    public static class ValuesAtPoint {
        private final Number[] values;

        public ValuesAtPoint(Number[] values) {
            this.values = values;
        }

        public Number[] getValues() {
            return values;
        }
    }
}

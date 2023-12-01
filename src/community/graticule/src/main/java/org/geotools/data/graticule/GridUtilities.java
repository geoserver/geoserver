package org.geotools.data.graticule;

import java.io.IOException;
import java.text.Format;
import java.util.Arrays;
import java.util.List;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.api.style.Style;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.data.graticule.gridsupport.NiceScale;
import org.geotools.data.graticule.gridsupport.XWilkinson;
import org.geotools.data.graticule.gridsupport.XWilkinson.Label;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.DefaultGridFeatureBuilder;
import org.geotools.grid.Lines;
import org.geotools.grid.oblong.Oblongs;
import org.geotools.grid.ortholine.LineOrientation;
import org.geotools.grid.ortholine.OrthoLineDef;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.CoordinateFormat;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

public class GridUtilities {

    public static final double DELTA = 0.00001;

    private GridUtilities() {
        // stop people instantiating the class
    }

    /**
     * Utility class to create a grid layer that neatly wraps the features provided.
     *
     * @param style - the style to draw the grid with
     * @param gridBounds - the bounds to include with in the grid
     * @return - a Layer that contains the grid.
     * @throws IOException if something goes wrong
     */
    public static Layer createNiceGridLayer(Style style, ReferencedEnvelope gridBounds)
            throws IOException {
        double height = gridBounds.getHeight();
        double width = gridBounds.getWidth();
        double xMin = gridBounds.getMinimum(0);
        double yMin = gridBounds.getMinimum(1);
        double xMax = gridBounds.getMaximum(0);
        double yMax = gridBounds.getMaximum(1);
        NiceScale xNiceScale = new NiceScale(xMin, xMax);
        NiceScale yNiceScale = new NiceScale(yMin, yMax);
        /*        System.out.println(
                "Long: "
                        + xNiceScale.getNiceMin()
                        + " to "
                        + xNiceScale.getNiceMax()
                        + " with "
                        + xNiceScale.getTickSpacing()
                        + " steps");
        System.out.println(
                "Lat: "
                        + yNiceScale.getNiceMin()
                        + " to "
                        + yNiceScale.getNiceMax()
                        + " with "
                        + yNiceScale.getTickSpacing()
                        + " steps");*/
        double deltaX = (xNiceScale.getNiceMax() - xNiceScale.getNiceMin()) - width;
        double deltaY = (yNiceScale.getNiceMax() - yNiceScale.getNiceMin()) - height;
        gridBounds.expandBy(deltaX / 2.0, deltaY / 2.0);

        double vertexSpacing = gridBounds.getHeight() / xNiceScale.getTickSpacing();
        System.out.println(vertexSpacing);
        SimpleFeatureSource grid =
                Oblongs.createGrid(
                        gridBounds,
                        xNiceScale.getTickSpacing(),
                        xNiceScale.getTickSpacing(),
                        vertexSpacing,
                        new DefaultGridFeatureBuilder(gridBounds.getCoordinateReferenceSystem()));
        Layer gridLayer = new FeatureLayer(grid.getFeatures(), style);
        return gridLayer;
    }

    private static FeatureType buildType(CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("grid");
        tb.add(LineFeatureBuilder.DEFAULT_GEOMETRY_ATTRIBUTE_NAME, LineString.class, crs);
        tb.add(LineFeatureBuilder.ID_ATTRIBUTE_NAME, Integer.class);
        tb.add(LineFeatureBuilder.LEVEL_ATTRIBUTE_NAME, Integer.class);
        tb.add(LineFeatureBuilder.VALUE_LABEL_NAME, String.class);
        tb.add(LineFeatureBuilder.VALUE_ATTRIBUTE_NAME, Double.class);
        return tb.buildFeatureType();
    }

    public static Layer createLabeledGridLayer(
            Style style, ReferencedEnvelope gridBounds, boolean xwilkinson) throws IOException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
        double height = gridBounds.getHeight();
        double width = gridBounds.getWidth();
        double xMin = gridBounds.getMinimum(0);
        double yMin = gridBounds.getMinimum(1);
        double xMax = gridBounds.getMaximum(0);
        double yMax = gridBounds.getMaximum(1);
        FeatureSource grid = null;
        double xStep;
        double yStep;
        CoordinateReferenceSystem coordinateReferenceSystem =
                gridBounds.getCoordinateReferenceSystem();
        CoordinateSystem coordinateSystem = coordinateReferenceSystem.getCoordinateSystem();
        AxisOrder order = CRS.getAxisOrder(coordinateReferenceSystem);
        CoordinateSystemAxis xaxis, yaxis;
        if (order.equals(AxisOrder.EAST_NORTH)) {
            xaxis = coordinateSystem.getAxis(0);
            yaxis = coordinateSystem.getAxis(1);
        } else {
            xaxis = coordinateSystem.getAxis(1);
            yaxis = coordinateSystem.getAxis(0);
        }
        if (xMax > xaxis.getMaximumValue()) {
            xMax = xaxis.getMaximumValue();
        }
        System.out.println(xMin + " ?<" + xaxis.getMinimumValue());
        if (xMin < xaxis.getMinimumValue()) {
            xMin = xaxis.getMinimumValue();
        }
        System.out.println(yMax + " ?>" + yaxis.getMaximumValue());
        if (yMax > yaxis.getMaximumValue()) {
            yMax = yaxis.getMaximumValue();
        }
        System.out.println(yMin + " ?<" + yaxis.getMinimumValue());
        if (yMin < yaxis.getMinimumValue()) {
            yMin = yaxis.getMinimumValue();
        }
        if (!xwilkinson) {
            NiceScale xNiceScale = new NiceScale(xMin, xMax);
            NiceScale yNiceScale = new NiceScale(yMin, yMax);
            xMin = xNiceScale.getNiceMin();
            xMax = xNiceScale.getNiceMax();
            xStep = xNiceScale.getTickSpacing();
            System.out.println("Long: " + xMin + " to " + xMax + " with " + xStep + " steps");
            yMin = yNiceScale.getNiceMin();
            yMax = yNiceScale.getNiceMax();
            yStep = yNiceScale.getTickSpacing();
            System.out.println("Lat: " + yMin + " to " + yMax + " with " + yStep + " steps");
        } else {
            XWilkinson xXW = XWilkinson.of(new double[] {2, 3, 4, 5, 10, 20}, 10);

            XWilkinson yXW = XWilkinson.of(new double[] {2, 3, 4, 5, 10, 20}, 10);

            System.out.println(xMax + " ?>" + xaxis.getMaximumValue());

            Label xLabels = xXW.search(xMin, xMax, 20);
            System.out.println(
                    "Long: "
                            + xLabels.getMin()
                            + " to "
                            + xLabels.getMax()
                            + " with "
                            + xLabels.getStep()
                            + " steps");
            Label yLabels = yXW.search(yMin, yMax, 20);
            System.out.println(
                    "Lat: "
                            + yLabels.getMin()
                            + " to "
                            + yLabels.getMax()
                            + " with "
                            + yLabels.getStep()
                            + " steps");

            xMin = xLabels.getMin();
            xMax = xLabels.getMax();
            xStep = xLabels.getStep();
            /*if(gridBounds.getMinX()<xMin) {
            xMin-=xStep;
               }
               if(gridBounds.getMaxX()>xMax) {
            xMax+=xStep;
               }*/
            yMin = yLabels.getMin();
            yMax = yLabels.getMax();
            yStep = yLabels.getStep();
            if (gridBounds.getMinY() < yMin) {
                yMin -= yStep;
            }
            if (gridBounds.getMaxY() > yMax) {
                yMax += yStep;
            }
        }
        ReferencedEnvelope bounds =
                ReferencedEnvelope.envelope(
                        new Envelope(xMin, xMax, yMin, yMax), coordinateReferenceSystem);

        SimpleFeatureType type =
                (SimpleFeatureType) GridUtilities.buildType(coordinateReferenceSystem);
        List<OrthoLineDef> lineDefs =
                Arrays.asList(
                        // vertical (longitude) lines
                        new OrthoLineDef(LineOrientation.VERTICAL, 2, xStep),
                        // horizontal (latitude) lines
                        new OrthoLineDef(LineOrientation.HORIZONTAL, 2, yStep));

        // Specify vertex spacing to get "densified" polygons
        double vertexSpacing = 0.1;
        grid =
                Lines.createOrthoLines(
                        bounds, lineDefs, vertexSpacing, new LineFeatureBuilder(type));

        Layer gridLayer = new FeatureLayer(grid, style);
        return gridLayer;
    }

    static GeometryFactory geomFac = new GeometryFactory();
    static int id = 0;

    private static SimpleFeature drawLine(
            double xmin, double ymin, double xmax, double ymax, int step, FeatureType type) {
        if (step <= 0) {
            step = 1;
        }
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder((SimpleFeatureType) type);
        String label = "";
        double count = 2;
        boolean horizontal; // do the lines run parallel to equator
        CoordinateFormat formatter = new CoordinateFormat();
        formatter.setNumberPattern("##0.00");
        formatter.setAnglePattern("DD.dd");
        formatter.setCoordinateReferenceSystem(type.getCoordinateReferenceSystem());
        Format xFormat = formatter.getFormat(0);
        Format yFormat = formatter.getFormat(1);
        String unit =
                type.getCoordinateReferenceSystem()
                        .getCoordinateSystem()
                        .getAxis(0)
                        .getUnit()
                        .toString();
        if (Math.abs(xmax - xmin) < DELTA) {
            label = xFormat.format(xmax) + unit; /*
						 * xmax +
						 * type.getCoordinateReferenceSystem
						 * ().getCoordinateSystem()
						 * .getAxis
						 * (0).getUnit().toString();
						 */
            count = ((ymax - ymin) / step);
            // System.out.println("vertical "+xmax+"->"+xmin+", "+ymin+"->"+ymax+" count "+count);
            horizontal = false;
        } else {
            label = yFormat.format(ymax) + unit; /*
						 * ymax +
						 * type.getCoordinateReferenceSystem
						 * ().getCoordinateSystem()
						 * .getAxis
						 * (1).getUnit().toString();
						 */
            count = ((xmax - xmin) / step);
            horizontal = true;
            // System.out.println("horizontal "+xmax+"->"+xmin+", "+ymin+"->"+ymax+" count "+count);
        }

        Coordinate[] coords = new Coordinate[step + 1];
        coords[0] = new Coordinate(xmin, ymin);
        for (int i = 1; i < step; i++) {
            if (horizontal) {
                coords[i] = new Coordinate(xmin + i * count, ymin);
            } else {
                coords[i] = new Coordinate(xmin, ymin + i * count);
            }
        }
        coords[step] = new Coordinate(xmax, ymax);
        LineString line = geomFac.createLineString(coords);
        builder.add(line);
        builder.add(id++);
        builder.add(label);
        return builder.buildFeature(null);
    }

    /**
     * Utility class to create a grid layer that neatly wraps the features provided.
     *
     * @param style - the style to draw the grid with
     * @param gridBounds - the bounds to include with in the grid
     * @return - a Layer that contains the grid.
     * @throws IOException if something goes wrong
     */
    public static Layer createXWilkinsonGridLayer(Style style, ReferencedEnvelope gridBounds)
            throws IOException {
        double height = gridBounds.getHeight();
        double width = gridBounds.getWidth();
        double xMin = gridBounds.getMinimum(0);
        double yMin = gridBounds.getMinimum(1);
        double xMax = gridBounds.getMaximum(0);
        double yMax = gridBounds.getMaximum(1);
        XWilkinson xXW = XWilkinson.of(new double[] {1, 2, 5, 10, 20}, 10);
        XWilkinson yXW = XWilkinson.of(new double[] {1, 2, 5, 10, 20}, 10);

        Label xLabels = xXW.search(xMin, xMax, 20);
        Label yLabels = yXW.search(yMin, yMax, 20);
        /*System.out.println(
                        "Long: "
                                + xLabels.getMin()
                                + " to "
                                + xLabels.getMax()
                                + " with "
                                + xLabels.getStep()
                                + " steps");

                System.out.println(
                        "Lat: "
                                + yLabels.getMin()
                                + " to "
                                + yLabels.getMax()
                                + " with "
                                + yLabels.getStep()
                                + " steps");
        */
        xMin = Math.min(xMin, xLabels.getMin());
        xMax = Math.max(xMax, xLabels.getMax());
        yMin = Math.min(yMin, yLabels.getMin());
        yMax = Math.max(yMax, yLabels.getMax());
        gridBounds =
                ReferencedEnvelope.envelope(
                        new Envelope(xMin, xMax, yMin, yMax),
                        gridBounds.getCoordinateReferenceSystem());

        double vertexSpacing = gridBounds.getHeight() / 100.0;

        SimpleFeatureSource grid =
                Oblongs.createGrid(
                        gridBounds,
                        xLabels.getStep(),
                        yLabels.getStep(),
                        vertexSpacing,
                        new DefaultGridFeatureBuilder(gridBounds.getCoordinateReferenceSystem()));
        Layer gridLayer = new FeatureLayer(grid.getFeatures(), style);
        return gridLayer;
    }
}

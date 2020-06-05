/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical;

import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.Parameter;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.gridshift.GridShiftLocator;
import org.geotools.referencing.operation.MathTransformProvider;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.geotools.util.logging.Logging;
import org.opengis.parameter.*;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.Transformation;

/**
 * Base class for transformations between 2 different height's related verticalCRS. The
 * transformation goes through a Vertical offset file containing the offset to be applied for each
 * specific x,y pair.
 */
public class VerticalGridTransform extends AbstractMathTransform {

    /** Logger */
    protected static final Logger LOGGER = Logging.getLogger(VerticalGridTransform.class);

    /**
     * Constructs a {@code VerticalGridTransform} from the specified grid shift file.
     *
     * <p>This constructor checks for grid shift file availability, but doesn't actually load the
     * full grid into memory to preserve resources.
     *
     * @param file Vertical grid file name
     * @throws NoSuchIdentifierException if the grid is not available.
     */
    public VerticalGridTransform(URI file, int interpolationCRSCode) throws FactoryException {
        if (file == null) {
            throw new NoSuchIdentifierException("No Vertical Grid File specified.", null);
        }

        this.grid = file;

        gridLocation = locateGrid(grid.toString());
        if (gridLocation == null) {
            throw new NoSuchIdentifierException(
                    "Could not locate Vertical Grid File " + file, null);
        }

        verticalGridShift = FACTORY.createVerticalGrid(gridLocation, interpolationCRSCode);
    }

    URL locateGrid(String grid) {
        for (GridShiftLocator locator : ReferencingFactoryFinder.getGridShiftLocators(null)) {
            URL result = locator.locateGrid(grid);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /** Gets the dimension of input points. */
    public final int getSourceDimensions() {
        return 3;
    }

    /** Gets the dimension of output points. */
    public final int getTargetDimensions() {
        return 3;
    }

    /** The original grid name */
    private URI grid = null;

    /** The grid file name as set in the constructor. */
    private URL gridLocation = null;

    /** The grid shift to be used */
    private VerticalGridShift verticalGridShift;

    /** The factory that loads the grid shift files */
    private static VerticalGridShiftFactory FACTORY = new VerticalGridShiftFactory();

    /**
     * Transform the input height value from the source data using the underlying Vertical Grid
     * Shift.
     *
     * @param x The first coordinate of the grid
     * @param y The second coordinate of the grid
     * @param height The height value to be transformed
     * @return true if the height has been successfully transformed.
     * @throws TransformException if the offset can't be computed for the specified coordinates.
     */
    protected boolean transformHeight(double x, double y, double[] height)
            throws TransformException {
        if (!verticalGridShift.isInValidArea(x, y)) {
            throw new TransformException(
                    "Point ("
                            + x
                            + " "
                            + y
                            + ") is outside of (("
                            + verticalGridShift.getValidArea()
                            + "))");
        }
        return verticalGridShift.shift(x, y, height);
    }

    /** Transforms a list of coordinate point ordinal values. */
    @Override
    public void transform(
            final double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException {
        bidirectionalTransform(srcPts, srcOff, dstPts, dstOff, numPts, true);
    }

    /**
     * Performs the actual transformation.
     *
     * @param srcPts the array containing the source point coordinates.
     * @param srcOff the offset to the first point to be transformed in the source array.
     * @param dstPts the array into which the transformed point coordinates are returned. May be the
     *     same than {@code srcPts}.
     * @param dstOff the offset to the location of the first transformed point that is stored in the
     *     destination array.
     * @param numPts the number of point objects to be transformed.
     * @param forward {@code true} for direct transform, {@code false} for inverse transform.
     * @throws TransformException if an IO error occurs reading the grid file.
     */
    private void bidirectionalTransform(
            double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts, boolean forward)
            throws TransformException {

        boolean shifted;

        double[] val = new double[1];
        while (--numPts >= 0) {
            double x = (srcPts[srcOff++]);
            double y = (srcPts[srcOff++]);
            double z = (srcPts[srcOff++]);
            val[0] = z;
            shifted = transformHeight(x, y, val);
            dstPts[dstOff++] = x;
            dstPts[dstOff++] = y;
            if (!shifted) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(
                            Level.FINER,
                            "Point ("
                                    + srcPts[srcOff - 3]
                                    + ", "
                                    + srcPts[srcOff - 2]
                                    + ") is not covered by '"
                                    + this.grid
                                    + "' Vertical Offset grid,"
                                    + " it will not be shifted.");
                }
            }
            dstPts[dstOff++] = val[0];
        }
    }

    /** The {@link VerticalGridTransform} provider. */
    public static class Provider extends MathTransformProvider {

        public static final String INTERPOLATION_CRS_CODE_KEY = "Interpolation CRS code";

        public static final String VERTICAL_OFFSET_FILE_KEY = "Vertical offset file";

        public static final String VERTICAL_OFFSET_BY_GRID_INTERPOLATION_KEY =
                "Vertical Offset by Grid Interpolation";

        private static final long serialVersionUID = 7111781812366039408L;

        /**
         * The operation parameter descriptor for the "Vertical offset file" parameter value. The
         * default value is "".
         */
        public static final DefaultParameterDescriptor<URI> FILE =
                new DefaultParameterDescriptor<URI>(
                        toMap(
                                new NamedIdentifier[] {
                                    new NamedIdentifier(Citations.EPSG, VERTICAL_OFFSET_FILE_KEY),
                                    new NamedIdentifier(Citations.EPSG, "8732")
                                }),
                        URI.class,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true);

        public static final DefaultParameterDescriptor<Integer> INTERPOLATION_CRS_CODE =
                new DefaultParameterDescriptor<Integer>(
                        toMap(
                                new NamedIdentifier[] {
                                    new NamedIdentifier(Citations.EPSG, INTERPOLATION_CRS_CODE_KEY),
                                    new NamedIdentifier(Citations.EPSG, "1048")
                                }),
                        Integer.class,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false);

        /** The parameters group. */
        static final ParameterDescriptorGroup PARAMETERS =
                createDescriptorGroup(
                        new NamedIdentifier[] {
                            new NamedIdentifier(
                                    Citations.EPSG, VERTICAL_OFFSET_BY_GRID_INTERPOLATION_KEY),
                            new NamedIdentifier(Citations.EPSG, "1081")
                        },
                        new ParameterDescriptor[] {FILE, INTERPOLATION_CRS_CODE});

        /** Constructs a provider. */
        public Provider() {
            super(3, 3, PARAMETERS);
        }

        /** Returns the operation type. */
        @Override
        public Class<Transformation> getOperationType() {
            return Transformation.class;
        }

        /**
         * Creates a math transform from the specified group of parameter values.
         *
         * @param values The group of parameter values.
         * @return The created math transform.
         * @throws ParameterNotFoundException if a required parameter was not found.
         * @throws FactoryException if there is a problem creating this math transform.
         */
        protected MathTransform createMathTransform(final ParameterValueGroup values)
                throws ParameterNotFoundException, FactoryException {
            return new VerticalGridTransform(
                    value(FILE, values), value(INTERPOLATION_CRS_CODE, values));
        }
    }

    /**
     * Returns the parameter values for this math transform.
     *
     * @return A copy of the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterValue<URI> file = new Parameter<URI>(Provider.FILE);
        file.setValue(grid);

        final ParameterValue<Integer> interpolationCRSCode =
                new Parameter<Integer>(Provider.INTERPOLATION_CRS_CODE);
        interpolationCRSCode.setValue(verticalGridShift.getCRSCode());

        return new ParameterGroup(
                Provider.PARAMETERS, new ParameterValue[] {file, interpolationCRSCode});
    }

    public VerticalGridShift getVerticalGridShift() {
        return verticalGridShift;
    }

    @Override
    public int hashCode() {
        return this.grid.hashCode();
    }
}

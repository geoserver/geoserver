/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import it.geosolutions.jaiext.utilities.ImageLayout2;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;
import org.geoserver.data.util.CoverageUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Class encapsulating the logic to scale a coverage to a pre-defined target size.
 *
 * @author Stefano Costa, GeoSolutions
 */
class ScaleToTarget {

    /** The overview policy. By default, NEAREST policy is used * */
    private OverviewPolicy overviewPolicy;

    /** The interpolation method. By default, NEAREST interpolation is used * */
    private Interpolation interpolation;

    private GridCoverage2DReader reader;

    private Envelope envelope;

    private Integer adjustedTargetSizeX;

    private Integer adjustedTargetSizeY;

    /**
     * Constructor.
     *
     * @param reader the coverage reader to use for reading metadata
     */
    ScaleToTarget(GridCoverage2DReader reader) {
        this(reader, null);
    }

    /**
     * Two-args constructor.
     *
     * @param reader the coverage reader to use for reading metadata
     * @param envelope the envelope of the ROI we want to scale (if <code>null</code>, the envelope
     *     of the whole coverage is used)
     */
    ScaleToTarget(GridCoverage2DReader reader, Envelope envelope) {
        checkNotNull(reader, "reader");
        this.reader = reader;
        this.envelope = envelope;
        if (this.envelope == null) {
            this.envelope = reader.getOriginalEnvelope();
        }
        this.interpolation =
                (Interpolation) ImageUtilities.NN_INTERPOLATION_HINT.get(JAI.KEY_INTERPOLATION);
        this.overviewPolicy = OverviewPolicy.NEAREST;
    }

    /** @return the interpolation */
    public Interpolation getInterpolation() {
        return interpolation;
    }

    /** @param interpolation the interpolation to set */
    public void setInterpolation(Interpolation interpolation) {
        checkNotNull(interpolation, "interpolation");
        this.interpolation = interpolation;
    }

    /** @return the overviewPolicy */
    public OverviewPolicy getOverviewPolicy() {
        return overviewPolicy;
    }

    /** @param overviewPolicy the overviewPolicy to set */
    public void setOverviewPolicy(OverviewPolicy overviewPolicy) {
        checkNotNull(overviewPolicy, "overviewPolicy");
        this.overviewPolicy = overviewPolicy;
    }

    /** @return the current target size */
    public Integer[] getTargetSize() {
        return new Integer[] {this.adjustedTargetSizeX, this.adjustedTargetSizeY};
    }

    /**
     * Sets the size of the scaled image (target).
     *
     * <p>If one of the two inputs is omitted, the missing value is inferred from the provided one
     * so that the aspect ratio of the specified envelope is preserved.
     *
     * @param targetSizeX the size of the target image along the X axis
     * @param targetSizeY the size of the target image along the Y axis
     */
    public void setTargetSize(Integer targetSizeX, Integer targetSizeY) throws TransformException {
        // validate input
        checkTargetSize(targetSizeX, "X");
        checkTargetSize(targetSizeY, "Y");

        // store input values in internal state
        this.adjustedTargetSizeX = targetSizeX;
        this.adjustedTargetSizeY = targetSizeY;

        if (this.adjustedTargetSizeX == null && this.adjustedTargetSizeY == null) {
            // no scaling should be done, return
            return;
        }

        // adjust target size, if needed
        if ((this.adjustedTargetSizeX == null && this.adjustedTargetSizeY != null)
                || (this.adjustedTargetSizeY == null && this.adjustedTargetSizeX != null)) {
            // target size was specified for a single axis: calculate target size along the other
            // axis preserving original aspect ratio
            MathTransform g2w = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);
            GridGeometry2D gg =
                    new GridGeometry2D(
                            PixelInCell.CELL_CENTER, g2w, envelope, GeoTools.getDefaultHints());
            double width = gg.getGridRange2D().getWidth();
            double height = gg.getGridRange2D().getHeight();
            double whRatio = width / height;
            if (this.adjustedTargetSizeY != null) {
                // calculate X size
                this.adjustedTargetSizeX = (int) Math.round(this.adjustedTargetSizeY * whRatio);
            } else {
                // calculate Y size
                this.adjustedTargetSizeY = (int) Math.round(this.adjustedTargetSizeX / whRatio);
            }
        }
    }

    /** @return the grid geometry at the picked read resolution */
    GridGeometry2D getGridGeometry() throws IOException {
        MathTransform gridToCRS = getGridToCRSTransform();
        GridGeometry2D gridGeometry =
                new GridGeometry2D(
                        PixelInCell.CELL_CENTER, gridToCRS, envelope, GeoTools.getDefaultHints());

        return gridGeometry;
    }

    /**
     * Reads the coverage using the provided reader and read parameters, and then scales it to the
     * set target size.
     *
     * <p>The method properly sets the {@link AbstractGridFormat#READ_GRIDGEOMETRY2D} parameter
     * before reading.
     *
     * <p>If no target size is set, or the requested resolution matches the native resolution of the
     * image, or the resolution of one of its overviews, scaling is not performed.
     *
     * <p>In any case, if the selected interpolation method is not Nearest Neighbor, interpolation
     * is performed.
     *
     * @param readParameters the read parameters to pass to the coverage reader
     * @return the scaled coverage
     */
    public GridCoverage2D scale(GeneralParameterValue[] readParameters) throws IOException {
        if (readParameters == null) {
            readParameters = new GeneralParameterValue[] {};
        }

        // setup reader parameters to have it exploit overviews
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        final List<GeneralParameterDescriptor> parameterDescriptors =
                readParametersDescriptor.getDescriptor().descriptors();
        readParameters =
                CoverageUtils.mergeParameter(
                        parameterDescriptors,
                        readParameters,
                        getGridGeometry(),
                        AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().getCode());

        GridCoverage2D inputGC = reader.read(readParameters);
        return scale(inputGC);
    }

    /**
     * Scale the provided coverage to the set target size.
     *
     * <p>Please note that the method assumes the coverage was read taking overviews into account,
     * i.e. by properly setting the {@link AbstractGridFormat#READ_GRIDGEOMETRY2D} parameter.
     *
     * <p>If no target size was set, or the requested resolution matches the native resolution of
     * the image, or the resolution of one of its overviews, scaling is not performed.
     *
     * <p>In any case, if the selected interpolation method is not Nearest Neighbor, interpolation
     * is performed.
     *
     * @param sourceGC the scaled coverage
     */
    /*
     * Code adapted from org.geoserver.wcs2_0.ScalingPolicy.ScaleToSize
     */
    public GridCoverage2D scale(GridCoverage2D sourceGC) throws IOException {
        checkNotNull(sourceGC, "sourceGC)");

        if (!isTargetSizeSet() && (interpolation instanceof InterpolationNearest)) {
            return sourceGC;
        }

        // scale
        final Hints hints = GeoTools.getDefaultHints();
        final GridEnvelope2D sourceGE = sourceGC.getGridGeometry().getGridRange2D();
        if ((isTargetSizeSet()
                        && this.adjustedTargetSizeX.equals(sourceGE.width)
                        && this.adjustedTargetSizeY == sourceGE.height)
                || (!isTargetSizeSet())) {
            // NO NEED TO SCALE, do we need interpolation?
            if (interpolation instanceof InterpolationNearest) {
                return sourceGC;
            } else {
                // interpolate coverage if requested and not nearest!!!!
                final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(sourceGC);
                parameters
                        .parameter("warp")
                        .setValue(
                                new WarpAffine(AffineTransform.getScaleInstance(1, 1))); // identity
                parameters.parameter("interpolation").setValue(interpolation);
                parameters
                        .parameter("backgroundValues")
                        .setValue(
                                CoverageUtilities.getBackgroundValues(sourceGC)); // TODO check and
                // improve
                return (GridCoverage2D)
                        CoverageProcessor.getInstance().doOperation(parameters, hints);
            }
        }

        // create final warp
        final double scaleX = 1.0 * this.adjustedTargetSizeX / sourceGE.width;
        final double scaleY = 1.0 * this.adjustedTargetSizeY / sourceGE.height;
        final RenderedImage sourceImage = sourceGC.getRenderedImage();
        final int sourceMinX = sourceImage.getMinX();
        final int sourceMinY = sourceImage.getMinY();
        final AffineTransform affineTransform =
                new AffineTransform(
                        scaleX,
                        0,
                        0,
                        scaleY,
                        sourceMinX - scaleX * sourceMinX, // preserve sourceImage.getMinX()
                        sourceMinY
                                - scaleY
                                        * sourceMinY); // preserve sourceImage.getMinY() as per spec
        Warp warp;
        try {
            warp = new WarpAffine(affineTransform.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
        // impose final
        final ImageLayout2 layout =
                new ImageLayout2(
                        sourceMinX, sourceMinY, this.adjustedTargetSizeX, this.adjustedTargetSizeY);
        hints.add(new Hints(JAI.KEY_IMAGE_LAYOUT, layout));
        final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
        final ParameterValueGroup parameters = operation.getParameters();
        parameters.parameter("Source").setValue(sourceGC);
        parameters.parameter("warp").setValue(warp);
        parameters.parameter("interpolation").setValue(interpolation);
        parameters
                .parameter("backgroundValues")
                .setValue(
                        CoverageUtilities.getBackgroundValues(sourceGC)); // TODO check and improve
        GridCoverage2D gc =
                (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters, hints);
        return gc;
    }

    /**
     * Computes the transformation between raster and world coordinates, taking scaling into
     * account.
     *
     * @return the grid-to-CRS transformation
     */
    MathTransform getGridToCRSTransform() throws IOException {
        // scaling transform
        AffineTransform scaleTransform = getScaleTransform();

        // grid-to-world transformation
        AffineTransform g2w =
                (AffineTransform) reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);

        // final transformation: g2w + scaling
        AffineTransform finalTransform = new AffineTransform(g2w);
        finalTransform.concatenate(scaleTransform);

        return ProjectiveTransform.create(finalTransform);
    }

    /**
     * Computes the scaling transformation for the overview which would be picked for the requested
     * resolution.
     *
     * @return the scaling transformation
     */
    private AffineTransform getScaleTransform() throws IOException {
        // getting the native resolution
        final double[] nativeResolution = computeNativeResolution();

        // getting the requested resolution
        final double[] requestedResolution = computeRequestedResolution();

        // getting the read resolution from the reader, based on the current Overview Policy
        final double[] readResolution = computeReadingResolution(requestedResolution);

        // setup a scaling to get the transformation to be used to access the specified overview
        AffineTransform scaleTransform = new AffineTransform();
        double[] scaleFactors =
                new double[] {
                    readResolution[0] / nativeResolution[0], readResolution[1] / nativeResolution[1]
                };
        scaleTransform.scale(scaleFactors[0], scaleFactors[1]);

        return scaleTransform;
    }

    /** @return the native resolution */
    double[] computeNativeResolution() {
        double[] nativeResolution = new double[2];

        MathTransform transform = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);
        AffineTransform af = (AffineTransform) transform;
        // getting the native resolution
        nativeResolution[0] = XAffineTransform.getScaleX0(af);
        nativeResolution[1] = XAffineTransform.getScaleY0(af);

        return nativeResolution;
    }

    /** @return a resolution satisfying the scaling */
    double[] computeRequestedResolution() {
        if (!isTargetSizeSet()) {
            return computeNativeResolution();
        }

        double[] requestedResolution = new double[2];

        // Getting the requested resolution (using envelope and requested scaleSize)
        final GridToEnvelopeMapper mapper =
                new GridToEnvelopeMapper(
                        new GridEnvelope2D(
                                0, 0, this.adjustedTargetSizeX, this.adjustedTargetSizeY),
                        this.envelope);
        AffineTransform scalingTransform = mapper.createAffineTransform();
        requestedResolution[0] = XAffineTransform.getScaleX0(scalingTransform);
        requestedResolution[1] = XAffineTransform.getScaleY0(scalingTransform);

        return requestedResolution;
    }

    /**
     * @param requestedResolution the requested resolution
     * @return the resolution of the overview which would be picked out for the provided requested
     *     resolution using the current OverviewPolicy
     */
    double[] computeReadingResolution(double[] requestedResolution) throws IOException {
        return reader.getReadingResolutions(overviewPolicy, requestedResolution);
    }

    private boolean isTargetSizeSet() {
        return this.adjustedTargetSizeX != null || this.adjustedTargetSizeY != null;
    }

    private void checkNotNull(Object param, String paramName) {
        if (param == null) {
            throw new IllegalArgumentException(paramName + " parameter must be specified");
        }
    }

    private void checkTargetSize(Integer value, String dim) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(dim + " target size must be > 0");
        }
    }
}

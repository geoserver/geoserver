/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import it.geosolutions.jaiext.utilities.ImageLayout2;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;
import net.opengis.wcs20.ScaleAxisByFactorType;
import net.opengis.wcs20.ScaleAxisType;
import net.opengis.wcs20.ScaleByFactorType;
import net.opengis.wcs20.ScaleToExtentType;
import net.opengis.wcs20.ScaleToSizeType;
import net.opengis.wcs20.ScalingType;
import net.opengis.wcs20.TargetAxisExtentType;
import net.opengis.wcs20.TargetAxisSizeType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Scale;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.util.Utilities;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;
import org.vfny.geoserver.util.WCSUtils;

/**
 * {@link Enum} for implementing the management of the various scaling options available for the
 * scaling extension.
 *
 * <p>This enum works as a factory to separate the code that handles the scaling operations.
 *
 * @author Simone Giannecchini, GeoSolutions
 */
enum ScalingPolicy {
    DoNothing {

        @Override
        public GridCoverage2D scale(
                GridCoverage2D sourceGC,
                ScalingType scaling,
                Interpolation interpolation,
                Hints hints,
                WCSInfo wcsinfo) {
            Utilities.ensureNonNull("sourceGC", sourceGC);
            Utilities.ensureNonNull("ScalingType", scaling);
            Utilities.ensureNonNull("Interpolation", interpolation);
            return sourceGC;
        }
    },
    /**
     * In this case we scale each axis by the same factor.
     *
     * <p>We do rely on the {@link Scale} operation.
     */
    ScaleByFactor {

        @Override
        public GridCoverage2D scale(
                GridCoverage2D sourceGC,
                ScalingType scaling,
                Interpolation interpolation,
                Hints hints,
                WCSInfo wcsinfo) {
            Utilities.ensureNonNull("sourceGC", sourceGC);
            Utilities.ensureNonNull("ScalingType", scaling);

            // get scale factor
            double[] scaleFactors = getScaleFactors(scaling);
            // reading the data can cause the coverage to have asymmetric pre-applied scale factors
            // due to small numerical differences in their values, so keep both
            scaleFactors =
                    arrangeScaleFactors(hints, new double[] {scaleFactors[0], scaleFactors[0]});

            // checks
            if (scaleFactors[0] <= 0) {
                throw new WCS20Exception(
                        "Invalid scale factor",
                        WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor,
                        String.valueOf(scaleFactors[0]));
            }
            if (scaleFactors[1] <= 0) {
                throw new WCS20Exception(
                        "Invalid scale factor",
                        WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor,
                        String.valueOf(scaleFactors[0]));
            }

            // return coverage unchanged if we don't scale
            if (scaleFactors[0] == 1 && scaleFactors[1] == 1) {
                // NO SCALING do we need interpolation?
                if (interpolation instanceof InterpolationNearest) {
                    return sourceGC;
                } else {
                    // interpolate coverage if requested and not nearest!!!!
                    final Operation operation =
                            CoverageProcessor.getInstance().getOperation("Warp");
                    final ParameterValueGroup parameters = operation.getParameters();
                    parameters.parameter("Source").setValue(sourceGC);
                    parameters
                            .parameter("warp")
                            .setValue(
                                    new WarpAffine(
                                            AffineTransform.getScaleInstance(1, 1))); // identity
                    parameters.parameter("interpolation").setValue(interpolation);
                    parameters
                            .parameter("backgroundValues")
                            .setValue(
                                    CoverageUtilities.getBackgroundValues(
                                            sourceGC)); // TODO check and
                    // improve
                    return (GridCoverage2D)
                            CoverageProcessor.getInstance(hints).doOperation(parameters, hints);
                }
            }

            // ==== check limits
            final GridGeometry2D gridGeometry = sourceGC.getGridGeometry();
            final GridEnvelope gridRange = gridGeometry.getGridRange();
            WCSUtils.checkOutputLimits(
                    wcsinfo,
                    new GridEnvelope2D(
                            0,
                            0,
                            (int)
                                    (gridRange.getSpan(gridGeometry.gridDimensionX)
                                            * scaleFactors[0]),
                            (int)
                                    (gridRange.getSpan(gridGeometry.gridDimensionY)
                                            * scaleFactors[1])),
                    sourceGC.getRenderedImage().getSampleModel());

            // === scale
            final Operation operation = CoverageProcessor.getInstance().getOperation("Scale");
            final ParameterValueGroup parameters = operation.getParameters();
            parameters.parameter("Source").setValue(sourceGC);
            parameters
                    .parameter("interpolation")
                    .setValue(
                            interpolation != null
                                    ? interpolation
                                    : InterpolationPolicy.getDefaultPolicy().getInterpolation());
            parameters.parameter("xScale").setValue(scaleFactors[0]);
            parameters.parameter("yScale").setValue(scaleFactors[1]);
            parameters.parameter("xTrans").setValue(0.0);
            parameters.parameter("yTrans").setValue(0.0);
            return (GridCoverage2D)
                    CoverageProcessor.getInstance(hints).doOperation(parameters, hints);
        }
    },

    /**
     * In this case we scale each axis bto a predefined size.
     *
     * <p>We do rely on the {@link org.geotools.coverage.processing.operation.Warp} operation as the
     * final size must be respected on each axis.
     */
    ScaleToSize {

        /**
         * In this case we must retain the lower bounds by scale the size, hence {@link
         * ScaleDescriptor} JAI operation cannot be used. Same goes for {@link AffineDescriptor},
         * the only real option is {@link WarpDescriptor}.
         */
        @Override
        public GridCoverage2D scale(
                GridCoverage2D sourceGC,
                ScalingType scaling,
                Interpolation interpolation,
                Hints hints,
                WCSInfo wcsinfo) {

            // get scale size
            final int[] targetSize = getTargetSize(scaling);
            final int sizeX = targetSize[0];
            final int sizeY = targetSize[1];

            // scale
            final GridEnvelope2D sourceGE = sourceGC.getGridGeometry().getGridRange2D();
            if (sizeY == sourceGE.width && sizeX == sourceGE.height) {
                // NO SCALING do we need interpolation?
                if (interpolation instanceof InterpolationNearest) {
                    return sourceGC;
                } else {
                    // interpolate coverage if requested and not nearest!!!!
                    final Operation operation =
                            CoverageProcessor.getInstance().getOperation("Warp");
                    final ParameterValueGroup parameters = operation.getParameters();
                    parameters.parameter("Source").setValue(sourceGC);
                    parameters
                            .parameter("warp")
                            .setValue(
                                    new WarpAffine(
                                            AffineTransform.getScaleInstance(1, 1))); // identity
                    parameters.parameter("interpolation").setValue(interpolation);
                    parameters
                            .parameter("backgroundValues")
                            .setValue(
                                    CoverageUtilities.getBackgroundValues(
                                            sourceGC)); // TODO check and
                    // improve
                    return (GridCoverage2D)
                            CoverageProcessor.getInstance().doOperation(parameters, hints);
                }
            }

            // === enforce output limits
            WCSUtils.checkOutputLimits(
                    wcsinfo,
                    new GridEnvelope2D(0, 0, sizeX, sizeY),
                    sourceGC.getRenderedImage().getSampleModel());

            // create final warp
            final double scaleX = 1.0 * sizeX / sourceGE.width;
            final double scaleY = 1.0 * sizeY / sourceGE.height;
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
                                    - scaleY * sourceMinY); // preserve sourceImage.getMinY() as per
            // spec
            Warp warp;
            try {
                warp = new WarpAffine(affineTransform.createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new RuntimeException(e);
            }
            // impose final
            final ImageLayout2 layout = new ImageLayout2(sourceMinX, sourceMinY, sizeX, sizeY);
            hints.add(new Hints(JAI.KEY_IMAGE_LAYOUT, layout));
            final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
            final ParameterValueGroup parameters = operation.getParameters();
            parameters.parameter("Source").setValue(sourceGC);
            parameters.parameter("warp").setValue(warp);
            parameters
                    .parameter("interpolation")
                    .setValue(
                            interpolation != null
                                    ? interpolation
                                    : InterpolationPolicy.getDefaultPolicy().getInterpolation());
            parameters
                    .parameter("backgroundValues")
                    .setValue(
                            CoverageUtilities.getBackgroundValues(
                                    sourceGC)); // TODO check and improve
            GridCoverage2D gc =
                    (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters, hints);
            return gc;
        }
    },
    /**
     * In this case we scale each axis to a predefined extent.
     *
     * <p>We do rely on the {@link org.geotools.coverage.processing.operation.Warp} operation as the
     * final extent must be respected on each axis.
     */
    ScaleToExtent {

        @Override
        public GridCoverage2D scale(
                GridCoverage2D sourceGC,
                ScalingType scaling,
                Interpolation interpolation,
                Hints hints,
                WCSInfo wcsinfo) {
            Utilities.ensureNonNull("sourceGC", sourceGC);
            Utilities.ensureNonNull("ScalingType", scaling);

            // parse area
            final ScaleToExtentType scaleType = scaling.getScaleToExtent();
            final EList<TargetAxisExtentType> targetAxisExtentElements =
                    scaleType.getTargetAxisExtent();

            TargetAxisExtentType xExtent = null, yExtent = null;
            for (TargetAxisExtentType axisExtentType : targetAxisExtentElements) {
                final String axisName = axisExtentType.getAxis();
                if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/i")
                        || axisName.equals("i")) {
                    xExtent = axisExtentType;
                } else if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/j")
                        || axisName.equals("j")) {
                    yExtent = axisExtentType;
                } else {
                    // TODO remove when supporting TIME and ELEVATION
                    throw new WCS20Exception(
                            "Scale Axis Undefined",
                            WCS20Exception.WCS20ExceptionCode.ScaleAxisUndefined,
                            axisName);
                }
            }
            if (xExtent == null) {
                throw new WCS20Exception(
                        "Missing extent along i",
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        "Null");
            }
            if (yExtent == null) {
                throw new WCS20Exception(
                        "Missing extent along j",
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        "Null");
            }

            final int minx = (int) targetAxisExtentElements.get(0).getLow(); // TODO should this be
            // int?
            final int maxx = (int) targetAxisExtentElements.get(0).getHigh();
            final int miny = (int) targetAxisExtentElements.get(1).getLow();
            final int maxy = (int) targetAxisExtentElements.get(1).getHigh();

            // check on source geometry
            final GridEnvelope2D sourceGE = sourceGC.getGridGeometry().getGridRange2D();

            if (minx >= maxx) {
                throw new WCS20Exception(
                        "Invalid Extent for dimension:" + targetAxisExtentElements.get(0).getAxis(),
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        String.valueOf(maxx));
            }
            if (miny >= maxy) {
                throw new WCS20Exception(
                        "Invalid Extent for dimension:" + targetAxisExtentElements.get(1).getAxis(),
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        String.valueOf(maxy));
            }
            final Rectangle destinationRectangle =
                    new Rectangle(minx, miny, maxx - minx + 1, maxy - miny + 1);
            // UNSCALE
            if (destinationRectangle.equals(sourceGE)) {
                // NO SCALING do we need interpolation?
                if (interpolation instanceof InterpolationNearest) {
                    return sourceGC;
                } else {
                    // interpolate coverage if requested and not nearest!!!!
                    final Operation operation =
                            CoverageProcessor.getInstance().getOperation("Warp");
                    final ParameterValueGroup parameters = operation.getParameters();
                    parameters.parameter("Source").setValue(sourceGC);
                    parameters
                            .parameter("warp")
                            .setValue(
                                    new WarpAffine(
                                            AffineTransform.getScaleInstance(1, 1))); // identity
                    parameters.parameter("interpolation").setValue(interpolation);
                    parameters
                            .parameter("backgroundValues")
                            .setValue(
                                    CoverageUtilities.getBackgroundValues(
                                            sourceGC)); // TODO check and
                    // improve
                    return (GridCoverage2D)
                            CoverageProcessor.getInstance(hints).doOperation(parameters, hints);
                }
            }

            // === enforce output limits
            WCSUtils.checkOutputLimits(
                    wcsinfo,
                    new GridEnvelope2D(destinationRectangle),
                    sourceGC.getRenderedImage().getSampleModel());

            // create final warp
            final double scaleX = 1.0 * destinationRectangle.width / sourceGE.width;
            final double scaleY = 1.0 * destinationRectangle.height / sourceGE.height;
            final RenderedImage sourceImage = sourceGC.getRenderedImage();
            final int sourceMinX = sourceImage.getMinX();
            final int sourceMinY = sourceImage.getMinY();
            final AffineTransform affineTransform =
                    new AffineTransform(
                            scaleX,
                            0,
                            0,
                            scaleY,
                            destinationRectangle.x
                                    - scaleX * sourceMinX, // preserve sourceImage.getMinX()
                            destinationRectangle.y
                                    - scaleY * sourceMinY); // preserve sourceImage.getMinY()
            // as per spec
            Warp warp;
            try {
                warp = new WarpAffine(affineTransform.createInverse());
            } catch (NoninvertibleTransformException e) {
                throw new RuntimeException(e);
            }

            // impose size
            final ImageLayout2 layout =
                    new ImageLayout2(
                            destinationRectangle.x,
                            destinationRectangle.y,
                            destinationRectangle.width,
                            destinationRectangle.height);
            hints.add(new Hints(JAI.KEY_IMAGE_LAYOUT, layout));

            final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
            final ParameterValueGroup parameters = operation.getParameters();
            parameters.parameter("Source").setValue(sourceGC);
            parameters.parameter("warp").setValue(warp);
            parameters
                    .parameter("interpolation")
                    .setValue(
                            interpolation != null
                                    ? interpolation
                                    : InterpolationPolicy.getDefaultPolicy().getInterpolation());
            parameters
                    .parameter("backgroundValues")
                    .setValue(
                            CoverageUtilities.getBackgroundValues(
                                    sourceGC)); // TODO check and improve
            GridCoverage2D gc =
                    (GridCoverage2D) CoverageProcessor.getInstance().doOperation(parameters, hints);
            // RenderedImageBrowser.showChain(gc.getRenderedImage(),false);
            return gc;
        }
    },
    /**
     * In this case we scale each axis by the a provided factor.
     *
     * <p>We do rely on the {@link Scale} operation.
     */
    ScaleAxesByFactor {

        @Override
        public GridCoverage2D scale(
                GridCoverage2D sourceGC,
                ScalingType scaling,
                Interpolation interpolation,
                Hints hints,
                WCSInfo wcsinfo) {
            Utilities.ensureNonNull("sourceGC", sourceGC);
            Utilities.ensureNonNull("ScalingType", scaling);

            // TODO dimension management

            // get scale factor
            double scaleFactors[] = getScaleFactors(scaling);
            scaleFactors = arrangeScaleFactors(hints, scaleFactors);
            double scaleFactorX = scaleFactors[0];
            double scaleFactorY = scaleFactors[1];

            // unscale
            if (scaleFactorX == 1.0 && scaleFactorY == 1.0) {
                // NO SCALING do we need interpolation?
                if (interpolation instanceof InterpolationNearest) {
                    return sourceGC;
                } else {
                    // interpolate coverage if requested and not nearest!!!!
                    final Operation operation =
                            CoverageProcessor.getInstance().getOperation("Warp");
                    final ParameterValueGroup parameters = operation.getParameters();
                    parameters.parameter("Source").setValue(sourceGC);
                    parameters
                            .parameter("warp")
                            .setValue(
                                    new WarpAffine(
                                            AffineTransform.getScaleInstance(1, 1))); // identity
                    parameters.parameter("interpolation").setValue(interpolation);
                    parameters
                            .parameter("backgroundValues")
                            .setValue(
                                    CoverageUtilities.getBackgroundValues(
                                            sourceGC)); // TODO check and
                    // improve
                    return (GridCoverage2D)
                            CoverageProcessor.getInstance(hints).doOperation(parameters, hints);
                }
            }

            // ==== check limits
            final GridGeometry2D gridGeometry = sourceGC.getGridGeometry();
            final GridEnvelope gridRange = gridGeometry.getGridRange();
            WCSUtils.checkOutputLimits(
                    wcsinfo,
                    new GridEnvelope2D(
                            0,
                            0,
                            (int) (gridRange.getSpan(gridGeometry.gridDimensionX) * scaleFactorX),
                            (int) (gridRange.getSpan(gridGeometry.gridDimensionY) * scaleFactorY)),
                    sourceGC.getRenderedImage().getSampleModel());

            // scale
            final Operation operation = CoverageProcessor.getInstance().getOperation("Scale");
            final ParameterValueGroup parameters = operation.getParameters();
            parameters.parameter("Source").setValue(sourceGC);
            parameters
                    .parameter("interpolation")
                    .setValue(
                            interpolation != null
                                    ? interpolation
                                    : InterpolationPolicy.getDefaultPolicy().getInterpolation());
            parameters.parameter("xScale").setValue(scaleFactors[0]);
            parameters.parameter("yScale").setValue(scaleFactors[1]);
            parameters.parameter("xTrans").setValue(0.0);
            parameters.parameter("yTrans").setValue(0.0);
            return (GridCoverage2D)
                    CoverageProcessor.getInstance(hints).doOperation(parameters, hints);
        }
    };
    /**
     * Scale the provided {@link GridCoverage2D} according to the provided {@link ScalingType} and
     * the provided {@link Interpolation} and {@link Hints}.
     *
     * @param sourceGC the {@link GridCoverage2D} to scale.
     * @param scaling the instance of {@link ScalingType} that contains he type of scaling to
     *     perform.
     * @param interpolation the {@link Interpolation} to use. In case it is <code>null</code> we
     *     will use the {@link InterpolationPolicy} default value.
     * @param hints {@link Hints} to use during this operation.
     * @param wcsinfo the current instance of {@link WCSInfo} that contains wcs config for GeoServer
     * @return a scaled version of the input {@link GridCoverage2D}. It cam be subsampled or
     *     oversampled, it depends on the {@link ScalingType} content.
     */
    public abstract GridCoverage2D scale(
            GridCoverage2D sourceGC,
            ScalingType scaling,
            Interpolation interpolation,
            Hints hints,
            WCSInfo wcsinfo);

    /** Retrieve the {@link ScalingPolicy} from the provided {@link ScalingType} */
    public static ScalingPolicy getPolicy(ScalingType scaling) {
        if (scaling != null) {
            if (scaling.getScaleAxesByFactor() != null) {
                return ScaleAxesByFactor;
            }
            if (scaling.getScaleByFactor() != null) {
                return ScaleByFactor;
            }
            if (scaling.getScaleToExtent() != null) {
                return ScaleToExtent;
            }
            if (scaling.getScaleToSize() != null) {
                return ScaleToSize;
            }
        }
        return DoNothing;
    }

    /**
     * Extract the requested targetSize from this scaling extension in case the provided scaling is
     * a ScaleToSizeType type.
     *
     * <p>Throw an {@link IllegalArgumentException} in case the scaling type is not a supported one.
     */
    public static int[] getTargetSize(ScalingType scaling) {
        if (scaling.getScaleToSize() != null) {
            final ScaleToSizeType scaleType = scaling.getScaleToSize();
            final EList<TargetAxisSizeType> targetAxisSizeElements = scaleType.getTargetAxisSize();

            TargetAxisSizeType xSize = null, ySize = null;
            for (TargetAxisSizeType axisSizeType : targetAxisSizeElements) {
                final String axisName = axisSizeType.getAxis();
                if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/i")
                        || axisName.equals("i")) {
                    xSize = axisSizeType;
                } else if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/j")
                        || axisName.equals("j")) {
                    ySize = axisSizeType;
                } else {
                    // TODO remove when supporting TIME and ELEVATION
                    throw new WCS20Exception(
                            "Scale Axis Undefined",
                            WCS20Exception.WCS20ExceptionCode.ScaleAxisUndefined,
                            axisName);
                }
            }
            final int sizeX = (int) xSize.getTargetSize(); // TODO should this be int?
            if (sizeX <= 0) {
                throw new WCS20Exception(
                        "Invalid target size",
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        Integer.toString(sizeX));
            }
            final int sizeY = (int) ySize.getTargetSize(); // TODO should this be int?
            if (sizeY <= 0) {
                throw new WCS20Exception(
                        "Invalid target size",
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        Integer.toString(sizeY));
            }
            return new int[] {sizeX, sizeY};
        } else if (scaling.getScaleToExtent() != null) {
            ScaleToExtentType ste = scaling.getScaleToExtent();
            TargetAxisExtentType xSize = null, ySize = null;
            for (TargetAxisExtentType axisSizeType : ste.getTargetAxisExtent()) {
                final String axisName = axisSizeType.getAxis();
                if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/i")
                        || axisName.equals("i")) {
                    xSize = axisSizeType;
                } else if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/j")
                        || axisName.equals("j")) {
                    ySize = axisSizeType;
                } else {
                    // TODO remove when supporting TIME and ELEVATION
                    throw new WCS20Exception(
                            "Scale Axis Undefined",
                            WCS20Exception.WCS20ExceptionCode.ScaleAxisUndefined,
                            axisName);
                }
            }
            final int sizeX = (int) (xSize.getHigh() - xSize.getLow()); // TODO should this be int?
            if (sizeX <= 0) {
                throw new WCS20Exception(
                        "Invalid target extent, high is greater than low",
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        Integer.toString((int) xSize.getHigh()));
            }
            final int sizeY = (int) (ySize.getHigh() - ySize.getLow());
            if (sizeY <= 0) {
                throw new WCS20Exception(
                        "Invalid target extent, high is greater than low",
                        WCS20Exception.WCS20ExceptionCode.InvalidExtent,
                        Integer.toString((int) ySize.getHigh()));
            }
            return new int[] {sizeX, sizeY};

        } else {
            throw new IllegalArgumentException(
                    "targe size can not be computed from this type of scaling: "
                            + getPolicy(scaling));
        }
    }

    /**
     * Extract the requested scaleFactors from this scaling extension in case the provided scaling
     * is a ScaleXXXFactor type.
     *
     * <p>Throw an {@link IllegalArgumentException} in case the scaling type is not a supported one.
     */
    public static double[] getScaleFactors(ScalingType scaling) {
        ScalingPolicy policy = getPolicy(scaling);
        switch (policy) {
            case ScaleByFactor:
                final ScaleByFactorType scaleByFactorType = scaling.getScaleByFactor();
                double scaleFactor = scaleByFactorType.getScaleFactor();
                if (scaleFactor <= 0) {
                    throw new WCS20Exception(
                            "Invalid scale factor, needs to be a positive number",
                            WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor,
                            Double.toString(scaleFactor));
                }
                return new double[] {scaleFactor, scaleFactor};
            case ScaleAxesByFactor:
                final ScaleAxisByFactorType scaleType = scaling.getScaleAxesByFactor();
                final EList<ScaleAxisType> targetAxisScaleElements = scaleType.getScaleAxis();

                ScaleAxisType xScale = null, yScale = null;
                for (ScaleAxisType scaleAxisType : targetAxisScaleElements) {
                    final String axisName = scaleAxisType.getAxis();
                    if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/i")
                            || axisName.equals("i")) {
                        xScale = scaleAxisType;
                    } else if (axisName.equals("http://www.opengis.net/def/axis/OGC/1/j")
                            || axisName.equals("j")) {
                        yScale = scaleAxisType;
                    } else {
                        // TODO remove when supporting TIME and ELEVATION
                        throw new WCS20Exception(
                                "Scale Axis Undefined",
                                WCS20Exception.WCS20ExceptionCode.ScaleAxisUndefined,
                                axisName);
                    }
                }
                if (xScale == null) {
                    throw new WCS20Exception(
                            "Missing scale factor along i",
                            WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor,
                            "Null");
                }
                if (yScale == null) {
                    throw new WCS20Exception(
                            "Missing scale factor along j",
                            WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor,
                            "Null");
                }

                final double scaleFactorX = xScale.getScaleFactor();
                if (scaleFactorX <= 0) {
                    throw new WCS20Exception(
                            "Invalid scale factor",
                            WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor,
                            Double.toString(scaleFactorX));
                }
                final double scaleFactorY = yScale.getScaleFactor();
                if (scaleFactorY <= 0) {
                    throw new WCS20Exception(
                            "Invalid scale factor",
                            WCS20Exception.WCS20ExceptionCode.InvalidScaleFactor,
                            Double.toString(scaleFactorY));
                }
                return new double[] {scaleFactorX, scaleFactorY};
            default:
                throw new IllegalArgumentException(
                        "scale factors can not be computed from this type of scaling: " + policy);
        }
    }

    /**
     * In case some scaling factor have been pre-applied, make sure to arrange the requested target
     * scaleFactors by taking into account the previous ones.
     *
     * <p>This is usually required when using overviews. Suppose you want to get a target
     * scaleFactor of 0.00001 and the worst overview provide you a scale factor of 0.0001, then the
     * current scaleFactor need to be adjusted by a remaining 0.1 factor.
     *
     * @return the arranged scaleFactor
     */
    private static double[] arrangeScaleFactors(Hints hints, final double[] scaleFactors) {
        if (hints != null && hints.containsKey(GetCoverage.PRE_APPLIED_SCALE)) {
            Double[] preAppliedScale = (Double[]) hints.get(GetCoverage.PRE_APPLIED_SCALE);
            if (preAppliedScale != null) {
                scaleFactors[0] = scaleFactors[0] * preAppliedScale[0];
                scaleFactors[1] = scaleFactors[1] * preAppliedScale[1];
            }
        }
        return scaleFactors;
    }
}

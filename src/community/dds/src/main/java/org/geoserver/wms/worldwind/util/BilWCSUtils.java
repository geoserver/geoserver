/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind.util;

import java.util.HashMap;
import javax.media.jai.Interpolation;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.coverage.processing.operation.Resample;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.util.WCSUtils;
import org.vfny.geoserver.wcs.WcsException;

/**
 * This class adds the missing data mangling with geotools functions from 2.0.x WCSUtils. They were
 * removed for deprecation reasons, this is a placeholder till more permanent solutions are found
 *
 * @author Tishampati Dhar
 * @since 2.1.x
 */
public class BilWCSUtils extends WCSUtils {

    public static final Hints LENIENT_HINT = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

    // ///////////////////////////////////////////////////////////////////
    //
    // Static Processors
    //
    // ///////////////////////////////////////////////////////////////////
    private static final CoverageProcessor processor = CoverageProcessor.getInstance();

    private static final Hints hints = new Hints(new HashMap(5));

    static {
        hints.add(LENIENT_HINT);
    }

    /**
     * <strong>Reprojecting</strong><br>
     * The new grid geometry can have a different coordinate reference system than the underlying
     * grid geometry. For example, a grid coverage can be reprojected from a geodetic coordinate
     * reference system to Universal Transverse Mercator CRS.
     *
     * @param coverage GridCoverage2D
     * @param sourceCRS CoordinateReferenceSystem
     * @param targetCRS CoordinateReferenceSystem
     * @return GridCoverage2D
     */
    public static GridCoverage2D reproject(
            GridCoverage2D coverage,
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final Interpolation interpolation)
            throws WcsException {
        // ///////////////////////////////////////////////////////////////////
        //
        // REPROJECT
        //
        //
        // ///////////////////////////////////////////////////////////////////
        if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            /*
             * Operations.DEFAULT.resample( coverage, targetCRS, null,
             * Interpolation.getInstance(Interpolation.INTERP_NEAREST))
             */
            final ParameterValueGroup param =
                    (ParameterValueGroup) processor.getOperation("Resample").getParameters();
            param.parameter("Source").setValue(coverage);
            param.parameter("CoordinateReferenceSystem").setValue(targetCRS);
            param.parameter("GridGeometry").setValue(null);
            param.parameter("InterpolationType").setValue(interpolation);

            coverage =
                    (GridCoverage2D)
                            ((Resample) processor.getOperation("Resample"))
                                    .doOperation(param, hints);
        }

        return coverage;
    }

    /**
     * <strong>Scaling</strong><br>
     * Let user to scale down to the EXACT needed resolution. This step does not prevent from having
     * loaded an overview of the original image based on the requested scale.
     *
     * @param coverage GridCoverage2D
     * @param newGridRange GridRange
     * @param sourceCoverage GridCoverage
     * @param sourceCRS CoordinateReferenceSystem
     * @return GridCoverage2D
     */
    public static GridCoverage2D scale(
            final GridCoverage2D coverage,
            final GridEnvelope newGridRange,
            final GridCoverage sourceCoverage,
            final CoordinateReferenceSystem sourceCRS,
            final GeneralEnvelope destinationEnvelopeInSourceCRS) {
        // ///////////////////////////////////////////////////////////////////
        //
        // SCALE to the needed resolution
        // Let me now scale down to the EXACT needed resolution. This step does
        // not prevent from having loaded an overview of the original image
        // based on the requested scale.
        //
        // ///////////////////////////////////////////////////////////////////
        GridGeometry2D scaledGridGeometry =
                new GridGeometry2D(
                        newGridRange,
                        (destinationEnvelopeInSourceCRS != null)
                                ? destinationEnvelopeInSourceCRS
                                : sourceCoverage.getEnvelope());

        /*
         * Operations.DEFAULT.resample( coverage, sourceCRS, scaledGridGeometry,
         * Interpolation.getInstance(Interpolation.INTERP_NEAREST));
         */
        final ParameterValueGroup param =
                (ParameterValueGroup) processor.getOperation("Resample").getParameters();
        param.parameter("Source").setValue(coverage);
        param.parameter("CoordinateReferenceSystem").setValue(sourceCRS);
        param.parameter("GridGeometry").setValue(scaledGridGeometry);
        param.parameter("InterpolationType")
                .setValue(Interpolation.getInstance(Interpolation.INTERP_NEAREST));

        final GridCoverage2D scaledGridCoverage =
                (GridCoverage2D)
                        ((Resample) processor.getOperation("Resample")).doOperation(param, hints);

        return scaledGridCoverage;
    }

    /**
     * <strong>Cropping</strong><br>
     * The crop operation is responsible for selecting geographic subareas of the source coverage.
     *
     * @param coverage Coverage
     * @param sourceEnvelope GeneralEnvelope
     * @param sourceCRS CoordinateReferenceSystem
     * @param destinationEnvelopeInSourceCRS GeneralEnvelope
     * @return GridCoverage2D
     */
    public static GridCoverage2D crop(
            final Coverage coverage,
            final GeneralEnvelope sourceEnvelope,
            final CoordinateReferenceSystem sourceCRS,
            final GeneralEnvelope destinationEnvelopeInSourceCRS,
            final Boolean conserveEnvelope)
            throws WcsException {
        // ///////////////////////////////////////////////////////////////////
        //
        // CROP
        //
        //
        // ///////////////////////////////////////////////////////////////////
        final GridCoverage2D croppedGridCoverage;

        // intersect the envelopes
        final GeneralEnvelope intersectionEnvelope =
                new GeneralEnvelope(destinationEnvelopeInSourceCRS);
        intersectionEnvelope.setCoordinateReferenceSystem(sourceCRS);
        intersectionEnvelope.intersect((GeneralEnvelope) sourceEnvelope);

        // dow we have something to show?
        if (intersectionEnvelope.isEmpty()) {
            throw new WcsException("The Intersection is null. Check the requested BBOX!");
        }

        if (!intersectionEnvelope.equals((GeneralEnvelope) sourceEnvelope)) {
            // get the cropped grid geometry
            // final GridGeometry2D cropGridGeometry = getCroppedGridGeometry(
            // intersectionEnvelope, gridCoverage);

            /* Operations.DEFAULT.crop(coverage, intersectionEnvelope) */
            final ParameterValueGroup param =
                    (ParameterValueGroup) processor.getOperation("CoverageCrop").getParameters();
            param.parameter("Source").setValue(coverage);
            param.parameter("Envelope").setValue(intersectionEnvelope);
            // param.parameter("ConserveEnvelope").setValue(conserveEnvelope);

            croppedGridCoverage =
                    (GridCoverage2D)
                            ((Crop) processor.getOperation("CoverageCrop"))
                                    .doOperation(param, hints);
        } else {
            croppedGridCoverage = (GridCoverage2D) coverage;
        }

        // prefetch to be faster afterwards.
        // This step is important since at this stage we might be loading tiles
        // from disk
        croppedGridCoverage.prefetch(intersectionEnvelope.toRectangle2D());

        return croppedGridCoverage;
    }
}

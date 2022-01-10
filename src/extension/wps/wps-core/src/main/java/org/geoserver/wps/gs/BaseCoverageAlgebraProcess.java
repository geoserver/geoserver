/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.renderer.i18n.ErrorKeys;
import org.geotools.renderer.i18n.Errors;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/** @author Daniele Romagnoli, GeoSolutions */
public class BaseCoverageAlgebraProcess {

    static final String MISMATCHING_ENVELOPE_MESSAGE =
            "coverageA and coverageB should share the same Envelope";

    static final String MISMATCHING_GRID_MESSAGE =
            "coverageA and coverageB should have the same gridRange";

    static final String MISMATCHING_CRS_MESSAGE =
            "coverageA and coverageB should share the same CoordinateReferenceSystem";

    private BaseCoverageAlgebraProcess() {}

    public static void checkCompatibleCoverages(GridCoverage2D coverageA, GridCoverage2D coverageB)
            throws ProcessException {
        if (coverageA == null || coverageB == null) {
            String coveragesNull =
                    coverageA == null
                            ? (coverageB == null ? "coverageA and coverageB" : "coverageA")
                            : "coverageB";
            throw new ProcessException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1, coveragesNull));
        }

        //
        // checking same CRS
        //
        CoordinateReferenceSystem crsA = coverageA.getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsB = coverageB.getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(crsA, crsB)) {
            MathTransform mathTransform = null;
            try {
                mathTransform = CRS.findMathTransform(crsA, crsB);
            } catch (FactoryException e) {
                throw new ProcessException(
                        "Exceptions occurred while looking for a mathTransform between the 2 coverage's CRSs",
                        e);
            }
            if (mathTransform != null && !mathTransform.isIdentity()) {
                throw new ProcessException(MISMATCHING_CRS_MESSAGE);
            }
        }

        //
        // checking same Envelope and grid range
        //
        Envelope envA = coverageA.getEnvelope();
        Envelope envB = coverageB.getEnvelope();
        if (!envA.equals(envB)) {
            throw new ProcessException(MISMATCHING_ENVELOPE_MESSAGE);
        }

        GridEnvelope gridRangeA = coverageA.getGridGeometry().getGridRange();
        GridEnvelope gridRangeB = coverageA.getGridGeometry().getGridRange();
        if (gridRangeA.getSpan(0) != gridRangeB.getSpan(0)
                || gridRangeA.getSpan(1) != gridRangeB.getSpan(1)) {
            throw new ProcessException(MISMATCHING_GRID_MESSAGE);
        }
    }
}

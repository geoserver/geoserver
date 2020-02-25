/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.media.jai.ImageLayout;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.EnvelopeCompositionType;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.CoverageView.SelectedResolution;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.DataSourceException;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;

/**
 * Class delegate to parse coverageView information and computing consistency checks, resolution and
 * envelope computations
 */
class CoverageViewHandler {

    /**
     * Visit the a coverage and decide if its resolution is better than the ones previously visited
     */
    abstract static class CoverageResolutionChooser {

        double[] resolution;

        boolean visit(GridCoverage2D coverage) throws IOException {
            MathTransform2D mt = coverage.getGridGeometry().getGridToCRS2D();
            // cannot do a comparison
            if (!(mt instanceof AffineTransform2D)) {
                return false;
            }
            AffineTransform2D at = (AffineTransform2D) mt;
            double scaleX = Math.abs(at.getScaleX());
            double scaleY = Math.abs(at.getScaleY());
            if (resolution == null) {
                this.resolution = new double[2];
                this.resolution[0] = scaleX;
                this.resolution[1] = scaleY;
                return true;
            }
            if (compare(scaleX, scaleY, resolution)) {
                this.resolution[0] = scaleX;
                this.resolution[1] = scaleY;
                return true;
            } else {
                return false;
            }
        }

        protected abstract boolean compare(double scaleX, double scaleY, double[] resolution);
    }

    /** Visit the reader for a coverage composing the view and compute the resolution levels */
    interface ReaderResolutionComposer {
        void visit(GridCoverage2DReader reader) throws IOException;

        double[][] getResolutionLevels();

        String getReferenceName();

        CoverageResolutionChooser getCoverageResolutionChooser();
    }

    abstract class AbstractReaderResolutionComposer implements ReaderResolutionComposer {
        double[][] resolution;

        String referenceName = null;

        @Override
        public double[][] getResolutionLevels() {
            return resolution;
        }

        @Override
        public String getReferenceName() {
            return referenceName;
        }
    }

    /** Implementation returning the Best resolution of the visited elements */
    class BestReaderResolutionComposer extends AbstractReaderResolutionComposer {

        @Override
        public void visit(GridCoverage2DReader reader) throws IOException {
            if (resolution == null) {
                resolution = reader.getResolutionLevels();
                referenceName = reader.getGridCoverageNames()[0];
            } else {
                double[][] tempRes = reader.getResolutionLevels();
                if (tempRes[0][0] < resolution[0][0] && tempRes[0][1] < resolution[0][1]) {
                    resolution = tempRes;
                    referenceName = reader.getGridCoverageNames()[0];
                }
            }
        }

        @Override
        public CoverageResolutionChooser getCoverageResolutionChooser() {
            return new CoverageResolutionChooser() {
                @Override
                protected boolean compare(double scaleX, double scaleY, double[] resolution) {
                    return scaleX < resolution[0] && scaleY < resolution[1];
                }
            };
        }
    }

    /** Implementation returning the Worst resolution of the visited elements */
    class WorstReaderResolutionComposer extends AbstractReaderResolutionComposer {

        @Override
        public void visit(GridCoverage2DReader reader) throws IOException {
            if (resolution == null) {
                resolution = reader.getResolutionLevels();
                referenceName = reader.getGridCoverageNames()[0];
            } else {
                double[][] tempRes = reader.getResolutionLevels();
                if (tempRes[0][0] > resolution[0][0] && tempRes[0][1] > resolution[0][1]) {
                    resolution = tempRes;
                    referenceName = reader.getGridCoverageNames()[0];
                }
            }
        }

        @Override
        public CoverageResolutionChooser getCoverageResolutionChooser() {
            return new CoverageResolutionChooser() {
                @Override
                protected boolean compare(double scaleX, double scaleY, double[] resolution) {
                    return scaleX > resolution[0] && scaleY > resolution[1];
                }
            };
        }
    }

    /** Visit the reader for a coverage composing the view and compute the envelope. */
    interface EnvelopeComposer {
        void visit(GridCoverage2DReader reader);

        GeneralEnvelope getOriginalEnvelope();
    };

    abstract class AbstractEnvelopeComposer implements EnvelopeComposer {
        GeneralEnvelope env = null;

        @Override
        public GeneralEnvelope getOriginalEnvelope() {
            return env;
        }
    }

    /** Implementation returning the union envelope of the visited elements */
    class UnionEnvelopeComposer extends AbstractEnvelopeComposer {
        @Override
        public void visit(GridCoverage2DReader reader) {
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            if (env == null) {
                env = envelope;
            } else {
                env.add(envelope);
            }
        }
    }

    /** Implementation returning the intersection envelope of the visited elements */
    class IntersectionEnvelopeComposer extends AbstractEnvelopeComposer {
        @Override
        public void visit(GridCoverage2DReader reader) {
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            if (env == null) {
                env = envelope;
            } else {
                env.intersect(envelope);
            }
        }
    }

    /**
     * A CoveragesConsistencyChecker checks if the composing coverages respect the constraints which
     * currently are:
     *
     * <UL>
     *   <LI>same CRS
     *   <LI>same resolution
     *   <LI>same bbox
     *   <LI>same data type
     *   <LI>same dimensions (same number of dimension, same type, and same name)
     * </UL>
     *
     * When JAI-EXT BandMerge is available, constraints on resolution and bbox can be excluded.
     */
    static class CoveragesConsistencyChecker {

        private static double DELTA = 1E-10;

        private Set<ParameterDescriptor<List>> dynamicParameters;

        private String[] metadataNames;

        private GridEnvelope gridRange;

        private GeneralEnvelope envelope;

        private CoordinateReferenceSystem crs;

        private ImageLayout layout;

        private boolean canSupportHeterogeneousCoverages = false;

        public CoveragesConsistencyChecker(
                GridCoverage2DReader reader, boolean canSupportHeterogeneousCoverages)
                throws IOException {
            envelope = reader.getOriginalEnvelope();
            gridRange = reader.getOriginalGridRange();
            crs = reader.getCoordinateReferenceSystem();
            metadataNames = reader.getMetadataNames();
            dynamicParameters = reader.getDynamicParameters();
            layout = reader.getImageLayout();
            this.canSupportHeterogeneousCoverages = canSupportHeterogeneousCoverages;
        }

        /**
         * Check whether the coverages associated to the provided reader is consistent with the
         * reference coverage.
         */
        public boolean checkConsistency(GridCoverage2DReader reader) throws IOException {
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            GridEnvelope gridRange = reader.getOriginalGridRange();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
            String[] metadataNames = reader.getMetadataNames();

            // Checking envelope equality
            if (!envelope.equals(this.envelope, DELTA, true)) {

                // Throw an exception in case we are not supporting heterogeneous coverages
                if (!canSupportHeterogeneousCoverages) {
                    throw new IllegalArgumentException(
                            "The coverage envelope must be the same for all coverages");
                }

                // We won't support coverage views made of coverages having empty intersection
                if (!envelope.intersects(this.envelope, true)) {
                    throw new IllegalArgumentException(
                            "The coverage envelopes need to intersect each other");
                }
                return false;
            }

            // Checking gridRange equality
            final Rectangle thisRectangle =
                    new Rectangle(
                            this.gridRange.getLow(0),
                            this.gridRange.getLow(1),
                            this.gridRange.getSpan(0),
                            this.gridRange.getSpan(1));
            final Rectangle thatRectangle =
                    new Rectangle(
                            gridRange.getLow(0),
                            gridRange.getLow(1),
                            gridRange.getSpan(0),
                            gridRange.getSpan(1));
            if (!thisRectangle.equals(thatRectangle)) {
                if (!canSupportHeterogeneousCoverages) {
                    throw new IllegalArgumentException(
                            "The coverage gridRange should be the same for all coverages");
                }
                return false;
            }

            // Checking dimensions
            if (metadataNames == null) {
                if (this.metadataNames != null && this.metadataNames.length > 0) {
                    throw new IllegalArgumentException(
                            "The coverage metadataNames should have the same size");
                }
            } else if (this.metadataNames == null) {
                if (metadataNames != null && metadataNames.length > 0) {
                    throw new IllegalArgumentException(
                            "The coverage metadataNames should have the same size");
                }
            } else if (metadataNames.length != this.metadataNames.length) {
                throw new IllegalArgumentException(
                        "The coverage metadataNames should have the same size");
            } else {
                final Set<String> metadataSet = new HashSet<String>(Arrays.asList(metadataNames));
                for (String metadataName : this.metadataNames) {
                    if (!metadataSet.contains(metadataName)) {
                        throw new IllegalArgumentException("The coverage metadata are different");
                    }
                }
            }

            // TODO: Add check for dynamic parameters

            // Checking CRS
            MathTransform destinationToSourceTransform = null;
            if (!CRS.equalsIgnoreMetadata(crs, this.crs)) {
                try {
                    destinationToSourceTransform = CRS.findMathTransform(crs, this.crs, true);
                } catch (FactoryException e) {
                    throw new DataSourceException("Unable to inspect request CRS", e);
                }
            }

            // now transform the requested envelope to source crs
            if (destinationToSourceTransform != null
                    && !destinationToSourceTransform.isIdentity()) {
                throw new IllegalArgumentException(
                        "The coverage coordinateReferenceSystem should be the same for all coverages");
            }

            // Checking data type
            if (layout.getSampleModel(null).getDataType()
                    != this.layout.getSampleModel(null).getDataType()) {
                throw new IllegalArgumentException(
                        "The coverage dataType should be the same for all coverages");
            }
            return true;
        }
    }

    private GridCoverage2DReader delegate;

    /**
     * The coverageName to be used as reference. It is used for homogeneous coverages case, to
     * extract shared properties. It is used for heterogeneous coverages case, to access a specific
     * coverage in order to get the resolutions
     */
    private String referenceName;

    /** specifying whether this view is made of homogeneous coverages or not */
    private boolean homogeneousCoverages = true;

    /**
     * specifying whether we can support heterogeneous coverages (JAI-EXT's BandMerge is required to
     * support heterogeneous composition)
     */
    boolean supportHeterogeneousCoverages;

    /** Checker used to verify constraints are respected */
    private CoveragesConsistencyChecker checker;

    /** The coverageView definition */
    private CoverageView coverageView;

    private ReaderResolutionComposer resolutionComposer;

    private EnvelopeComposer envelopeComposer;

    public CoverageViewHandler(
            boolean supportHeterogeneousCoverages,
            GridCoverage2DReader delegate,
            String referenceName,
            CoverageView coverageView) {
        this.supportHeterogeneousCoverages = supportHeterogeneousCoverages;
        this.delegate = delegate;
        this.referenceName = referenceName;
        this.coverageView = coverageView;
        this.resolutionComposer = initResolutionComposer();
        this.envelopeComposer = initEnvelopeComposer();

        List<CoverageBand> bands = coverageView.getCoverageBands();
        int coverageBandsSize = bands.size();

        for (int bIdx = 0; bIdx < coverageBandsSize; bIdx++) {
            CoverageBand band = bands.get(bIdx);
            List<InputCoverageBand> selectedBands = band.getInputCoverageBands();

            // Peek for coverage name
            String coverageName = selectedBands.get(0).getCoverageName();
            GridCoverage2DReader reader = SingleGridCoverage2DReader.wrap(delegate, coverageName);

            try {
                if (checker == null) {
                    checker =
                            new CoveragesConsistencyChecker(reader, supportHeterogeneousCoverages);
                } else {
                    homogeneousCoverages &= checker.checkConsistency(reader);
                }
                envelopeComposer.visit(reader);
                resolutionComposer.visit(reader);
            } catch (IOException ioe) {
                // the next read operation will report the issue
            }
        }
        this.referenceName = resolutionComposer.getReferenceName();
    }

    public boolean isHomogeneousCoverages() {
        return homogeneousCoverages;
    }

    public GeneralEnvelope getOriginalEnvelope() {
        if (homogeneousCoverages) {
            return delegate.getOriginalEnvelope(referenceName);
        }
        return envelopeComposer.getOriginalEnvelope();
    }

    public double[][] getResolutionLevels() throws IOException {
        if (homogeneousCoverages) {
            return delegate.getResolutionLevels(referenceName);
        }
        return resolutionComposer.getResolutionLevels();
    }

    public GridEnvelope getOriginalGridRange() {
        if (homogeneousCoverages) {
            return delegate.getOriginalGridRange(referenceName);
        }
        // Due to mixed combinations, let's take the envelope and divide the span
        // by the resolution
        GeneralEnvelope envelope = getOriginalEnvelope();
        double[] res;
        try {
            res = getResolutionLevels()[0];
            return new GridEnvelope2D(
                    new Rectangle(
                            (int) (envelope.getSpan(0) / res[0]),
                            (int) (envelope.getSpan(1) / res[1])));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        if (homogeneousCoverages) {
            return delegate.getOriginalGridToWorld(referenceName, pixInCell);
        }
        final GridToEnvelopeMapper geMapper =
                new GridToEnvelopeMapper(getOriginalGridRange(), getOriginalEnvelope());
        geMapper.setPixelAnchor(PixelInCell.CELL_CENTER);
        MathTransform2D coverageGridToWorld2D = (MathTransform2D) geMapper.createTransform();

        // we do not have to change the pixel datum
        if (pixInCell == PixelInCell.CELL_CENTER) return coverageGridToWorld2D;

        // we do have to change the pixel datum
        if (coverageGridToWorld2D instanceof AffineTransform) {
            final AffineTransform tr = new AffineTransform((AffineTransform) coverageGridToWorld2D);
            tr.concatenate(AffineTransform.getTranslateInstance(-0.5, -0.5));
            return ProjectiveTransform.create(tr);
        }
        throw new IllegalStateException("This reader's grid to world transform is invalid!");
    }

    private ReaderResolutionComposer initResolutionComposer() {
        SelectedResolution selectedResolution = coverageView.getSelectedResolution();
        switch (selectedResolution) {
            case WORST:
                return new WorstReaderResolutionComposer();
            case BEST:
                return new BestReaderResolutionComposer();
            default:
                return new BestReaderResolutionComposer();
        }
    }

    private EnvelopeComposer initEnvelopeComposer() {
        EnvelopeCompositionType envelopeCompositionType = coverageView.getEnvelopeCompositionType();
        switch (envelopeCompositionType) {
            case INTERSECTION:
                return new IntersectionEnvelopeComposer();
            case UNION:
                return new UnionEnvelopeComposer();
            default:
                return new UnionEnvelopeComposer();
        }
    }

    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(referenceName, policy, requestedResolution);
    }

    EnvelopeCompositionType getEnvelopeCompositionType() {
        return coverageView.getEnvelopeCompositionType();
    }

    CoverageResolutionChooser getCoverageResolutionChooser() {
        return resolutionComposer.getCoverageResolutionChooser();
    }
}

/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.v1.coverages.cis.Axis;
import org.geoserver.ogcapi.v1.coverages.cis.DomainSet;
import org.geoserver.ogcapi.v1.coverages.cis.GeneralGrid;
import org.geoserver.ogcapi.v1.coverages.cis.GridLimits;
import org.geoserver.ogcapi.v1.coverages.cis.IndexAxis;
import org.geoserver.ogcapi.v1.coverages.cis.IrregularAxis;
import org.geoserver.ogcapi.v1.coverages.cis.RegularAxis;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.springframework.http.HttpStatus;
import tech.units.indriya.format.SimpleUnitFormat;

class DomainSetBuilder {

    private static final String TIME_AXIS = "Time";
    private final CoverageInfo coverage;

    public DomainSetBuilder(CoverageInfo coverage) {
        this.coverage = coverage;
    }

    public DomainSet build() throws IOException, FactoryException {
        EnvelopeAxesLabelsMapper mapper = new EnvelopeAxesLabelsMapper();
        CoordinateReferenceSystem crs = coverage.getCRS();
        String srsName = CoveragesService.getCRSURI(crs);

        // check coordinate system is supported
        CoordinateSystem cs = crs.getCoordinateSystem();
        if (cs.getDimension() > 2)
            throw new APIException(
                    APIException.NO_APPLICABLE_CODE,
                    "Too many dimensions, cannot describe domain",
                    HttpStatus.INTERNAL_SERVER_ERROR);

        // map CRS to domain axis
        List<Axis> domainAxes = new ArrayList<>();
        for (int i = 0; i < cs.getDimension(); i++) {
            domainAxes.add(toRegularAxis(cs.getAxis(i), mapper, coverage, i));
        }

        // map CRS to index axis
        List<IndexAxis> indexAxes = new ArrayList<>();
        for (int i = 0; i < cs.getDimension(); i++) {
            indexAxes.add(toIndexAxis(i, coverage));
        }

        // handle time as well
        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time != null) {
            GridCoverage2DReader reader =
                    (GridCoverage2DReader) coverage.getGridCoverageReader(null, null);
            TimeDimensionHelper helper = new TimeDimensionHelper(time, reader);
            switch (time.getPresentation()) {
                case CONTINUOUS_INTERVAL:
                case DISCRETE_INTERVAL:
                    RegularAxis regular = toRegularTimeAxis(helper);
                    domainAxes.add(regular);
                    indexAxes.add(toIndexAxis(indexAxes.size(), helper));
                    break;
                default:
                    IrregularAxis irregular = toIrregularTimeAxis(helper);
                    domainAxes.add(irregular);
                    indexAxes.add(toIndexAxis(indexAxes.size(), irregular));
            }
        }

        List<String> domainAxisLabels =
                domainAxes.stream().map(a -> a.getAxisLabel()).collect(Collectors.toList());
        List<String> indexAxisLabels =
                indexAxes.stream().map(a -> a.getAxisLabel()).collect(Collectors.toList());
        GridLimits limits = new GridLimits(indexAxisLabels, indexAxes);
        GeneralGrid gg = new GeneralGrid(srsName, domainAxisLabels, domainAxes, limits);
        return new DomainSet(gg);
    }

    private IrregularAxis toIrregularTimeAxis(TimeDimensionHelper helper) throws IOException {
        return new IrregularAxis(TIME_AXIS, helper.getFormattedDomain(), "s");
    }

    private RegularAxis toRegularTimeAxis(TimeDimensionHelper helper) throws IOException {
        return new RegularAxis(
                TIME_AXIS,
                helper.getFormattedBegin(),
                helper.getFormattedEnd(),
                helper.getResolutionValue(),
                helper.getResolutionUnit());
    }

    private RegularAxis toRegularAxis(
            CoordinateSystemAxis axis,
            EnvelopeAxesLabelsMapper mapper,
            CoverageInfo coverage,
            int axisIndex) {
        double lowerBound, upperBound, resolution;
        ReferencedEnvelope envelope = coverage.getNativeBoundingBox();
        GridGeometry grid = coverage.getGrid();
        if (axisIndex == 0 || axisIndex == 1) {
            lowerBound = envelope.getMinimum(axisIndex);
            upperBound = envelope.getMaximum(axisIndex);
            resolution = (upperBound - lowerBound) / grid.getGridRange().getSpan(axisIndex);
        } else {
            throw new UnsupportedOperationException(
                    "Cannot describe a coverage with a CRS having "
                            + (axisIndex + 1)
                            + " dimensions");
        }

        return new RegularAxis(
                mapper.getAxisLabel(axis),
                lowerBound,
                upperBound,
                resolution,
                SimpleUnitFormat.getInstance().format(axis.getUnit()));
    }

    private IndexAxis toIndexAxis(int axisIndex, CoverageInfo coverage) {
        String name = indexAxisName(axisIndex);
        GridEnvelope range = coverage.getGrid().getGridRange();
        return new IndexAxis(name, range.getLow(axisIndex), range.getHigh(axisIndex));
    }

    private String indexAxisName(int axisIndex) {
        return new String(new char[] {(char) ('i' + axisIndex)});
    }

    private IndexAxis toIndexAxis(int axisIndex, IrregularAxis axis) {
        String name = indexAxisName(axisIndex);
        return new IndexAxis(name, 0, axis.getCoordinate().size());
    }

    private IndexAxis toIndexAxis(int axisIndex, TimeDimensionHelper helper) throws IOException {
        String name = indexAxisName(axisIndex);
        long range = helper.getEnd().getTime() - helper.getBegin().getTime();
        BigDecimal rm = helper.getResolutionMillis();
        if (rm == null) {
            // assume second resolution
            rm = BigDecimal.valueOf(1000);
        }
        return new IndexAxis(name, 0, (int) (range / rm.longValue()));
    }
}

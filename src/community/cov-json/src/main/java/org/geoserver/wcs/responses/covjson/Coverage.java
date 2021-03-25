/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;

@JsonPropertyOrder({"type", "domain", "referencing", "parameters", "ranges"})
public class Coverage extends CoverageJson {

    private static final String TYPE = "Coverage";

    @JsonProperty(required = true)
    private Domain domain;

    @JsonProperty private Map<String, Parameter> parameters;

    private GridCoverage2D coverage;

    @JsonProperty(required = true)
    private Map<String, NdArray> ranges;

    private GridCoverage2D referenceCoverage;

    public Coverage(GridCoverage2D coverage) {
        super(TYPE);
        this.coverage = coverage;
        List<GridCoverage2D> coverages;
        if (coverage instanceof GranuleStack) {
            GranuleStack granuleStack = (GranuleStack) coverage;
            coverages = granuleStack.getGranules();
            referenceCoverage = coverages.get(0);
        } else {
            coverages = Collections.singletonList(coverage);
            referenceCoverage = coverage;
        }
        domain = buildDomain(coverage);
        parameters = buildParameters(coverage);
        ranges = buildRanges(coverages, domain, parameters);
    }

    private Map<String, NdArray> buildRanges(
            List<GridCoverage2D> coverage, Domain domain, Map<String, Parameter> parameters) {
        Map<String, NdArray> ranges = new HashMap<>();
        for (String param : parameters.keySet()) {
            ranges.put(param, buildRange(parameters.get(param), domain, coverage));
        }

        return ranges;
    }

    private NdArray buildRange(Parameter parameter, Domain domain, List<GridCoverage2D> coverages) {
        return new NdArray(
                referenceCoverage.getRenderedImage().getSampleModel().getDataType(),
                domain,
                coverages);
    }

    private Map<String, Parameter> buildParameters(GridCoverage2D coverage) {
        Map<String, Parameter> parameters = new HashMap<>();
        GridSampleDimension[] dimensions = coverage.getSampleDimensions();
        for (GridSampleDimension dim : dimensions) {
            String desc = dim.getDescription().toString();
            String name = desc.replaceAll(" ", "").toUpperCase();
            parameters.put(name, buildParameter(dim));
        }
        return parameters;
    }

    private Parameter buildParameter(GridSampleDimension dim) {
        return new Parameter(dim);
    }

    private Domain buildDomain(GridCoverage2D coverage) {
        List<DimensionBean> dimensions = null;
        GranuleStack granuleStack = null;

        if (coverage instanceof GranuleStack) {
            granuleStack = (GranuleStack) coverage;
            dimensions = granuleStack.getDimensions();
        } else {
            dimensions = Collections.emptyList();
        }

        Domain.DomainBuilder builder = new Domain.DomainBuilder();
        builder.setDimensions(dimensions);
        builder.setCrs(referenceCoverage.getCoordinateReferenceSystem());
        builder.setGridGeometry(referenceCoverage.getGridGeometry());
        if (!dimensions.isEmpty()) {
            builder.setGranuleStack(granuleStack);
        }
        return builder.build();
    }

    public Domain getDomain() {
        return domain;
    }
}

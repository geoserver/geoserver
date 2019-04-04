/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.Predicates;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.coverage.grid.Format;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Applies access limits policies around the wrapped reader
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredGridCoverage2DReader extends DecoratingGridCoverage2DReader {

    /** Parameters used to control the {@link Crop} operation. */
    private static final ParameterValueGroup cropParams;

    /** Cached crop factory */
    private static final Crop coverageCropFactory = new Crop();

    static {
        final CoverageProcessor processor =
                new CoverageProcessor(new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
        cropParams = processor.getOperation("CoverageCrop").getParameters();
    }

    WrapperPolicy policy;

    public SecuredGridCoverage2DReader(GridCoverage2DReader delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    public Format getFormat() {
        Format format = delegate.getFormat();
        if (format == null) {
            return null;
        } else {
            return (Format) SecuredObjects.secure(format, policy);
        }
    }

    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return SecuredGridCoverage2DReader.read(delegate, policy, parameters);
    }

    static GridCoverage2D read(
            GridCoverage2DReader delegate, WrapperPolicy policy, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        // Package private static method to share reading code with Structured reader
        MultiPolygon rasterFilter = null;
        if (policy.getLimits() instanceof CoverageAccessLimits) {
            CoverageAccessLimits limits = (CoverageAccessLimits) policy.getLimits();

            // get the crop filter
            rasterFilter = limits.getRasterFilter();
            Filter readFilter = limits.getReadFilter();

            // update the read params
            final GeneralParameterValue[] limitParams = limits.getParams();
            if (parameters == null) {
                parameters = limitParams;
            } else if (limitParams != null) {
                // scan the input params, add and overwrite with the limits params as needed
                List<GeneralParameterValue> params =
                        new ArrayList<GeneralParameterValue>(Arrays.asList(parameters));
                for (GeneralParameterValue lparam : limitParams) {
                    // remove the overwritten param, if any
                    for (Iterator it = params.iterator(); it.hasNext(); ) {
                        GeneralParameterValue param = (GeneralParameterValue) it.next();
                        if (param.getDescriptor().equals(lparam.getDescriptor())) {
                            it.remove();
                            break;
                        }
                    }
                    // add the overwrite param (will be an overwrite if it was already there, an
                    // addition otherwise)
                    params.add(lparam);
                }

                parameters = params.toArray(new GeneralParameterValue[params.size()]);
            }

            if (readFilter != null && !Filter.INCLUDE.equals(readFilter)) {
                Format format = delegate.getFormat();
                ParameterValueGroup readParameters = format.getReadParameters();
                List<GeneralParameterDescriptor> descriptors =
                        readParameters.getDescriptor().descriptors();

                // scan all the params looking for the one we want to add
                boolean replacedOriginalFilter = false;
                for (GeneralParameterValue pv : parameters) {
                    String pdCode = pv.getDescriptor().getName().getCode();
                    if ("FILTER".equals(pdCode) || "Filter".equals(pdCode)) {
                        replacedOriginalFilter = true;
                        ParameterValue pvalue = (ParameterValue) pv;
                        Filter originalFilter = (Filter) pvalue.getValue();
                        if (originalFilter == null || Filter.INCLUDE.equals(originalFilter)) {
                            pvalue.setValue(readFilter);
                        } else {
                            Filter combined = Predicates.and(originalFilter, readFilter);
                            pvalue.setValue(combined);
                        }
                    }
                }
                if (!replacedOriginalFilter) {
                    parameters =
                            CoverageUtils.mergeParameter(
                                    descriptors, parameters, readFilter, "FILTER", "Filter");
                }
            }
        }

        GridCoverage2D grid = delegate.read(parameters);

        // crop if necessary
        if (rasterFilter != null && grid != null) {
            Geometry coverageBounds =
                    JTS.toGeometry((Envelope) new ReferencedEnvelope(grid.getEnvelope2D()));
            if (coverageBounds.intersects(rasterFilter)) {
                final ParameterValueGroup param = cropParams.clone();
                param.parameter("source").setValue(grid);
                param.parameter("ROI").setValue(rasterFilter);
                grid = (GridCoverage2D) coverageCropFactory.doOperation(param, null);
            } else {
                return null;
            }
        }
        return grid;
    }

    @Override
    public ServiceInfo getInfo() {
        ServiceInfo info = delegate.getInfo();
        if (info == null) {
            return null;
        } else {
            return (ServiceInfo) SecuredObjects.secure(info, policy);
        }
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        ResourceInfo info = delegate.getInfo(coverageName);
        if (info == null) {
            return null;
        } else {
            return (ResourceInfo) SecuredObjects.secure(info, policy);
        }
    }
}

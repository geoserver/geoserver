/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import net.opengis.wcs10.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Response object for the store=true path, that is, one that stores the coverage on disk and
 * returns its path thru the Coverages document
 *
 * @author Andrea Aime - TOPP
 */
public class Wcs10GetCoverageResponse extends Response {
    private static final Hints LENIENT_HINT = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

    // private final static Hints IGNORE_OVERVIEWS = new Hints(
    // Hints.IGNORE_COVERAGE_OVERVIEW, Boolean.TRUE);

    private static final Hints hints = new Hints(new HashMap(5));

    static {
        // ///////////////////////////////////////////////////////////////////
        //
        // HINTS
        //
        // ///////////////////////////////////////////////////////////////////
        hints.add(LENIENT_HINT);
        // hints.add(IGNORE_OVERVIEWS);
    }

    Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;

    public Wcs10GetCoverageResponse(
            Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        super(GridCoverage[].class);
        this.catalog = catalog;
        this.responseFactory = responseFactory;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        if (!(operation.getParameters()[0] instanceof GetCoverageType))
            throw new WcsException(
                    "Cannot handle object of type: " + operation.getParameters()[0].getClass());

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = getCoverage.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);
        return getCoverage.getSourceCoverage() + "." + delegate.getFileExtension(outputFormat);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (!(operation.getParameters()[0] instanceof GetCoverageType))
            throw new WcsException(
                    "Cannot handle object of type: " + operation.getParameters()[0].getClass());

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = getCoverage.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);

        return delegate.getMimeType(outputFormat);
    }

    private CoverageResponseDelegate getResponseDelegate(String outputFormat) {
        CoverageResponseDelegate delegate = responseFactory.encoderFor(outputFormat);
        if (delegate == null) {
            throw new WcsException("Could not find encoder for output format " + outputFormat);
        }
        return delegate;
    }

    @Override
    public boolean canHandle(Operation operation) {
        if (!(operation.getParameters()[0] instanceof GetCoverageType)) return false;

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = getCoverage.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);

        return delegate.canProduce(outputFormat);
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GridCoverage[] coverages = (GridCoverage[]) value;

        // grab the delegate for coverage encoding
        GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = request.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);

        // grab the coverage info for Coverages document encoding
        final GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        // ImageIOUtilities.visualize(coverage.getRenderedImage());

        // write the coverage
        delegate.encode(coverage, outputFormat, Collections.EMPTY_MAP, output);
        output.flush();
    }
}

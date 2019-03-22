/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.geoserver.ows.util.ResponseUtils.appendPath;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import net.opengis.wcs11.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Response object for the store=true path, that is, one that stores the coverage on disk and
 * returns its path thru the Coverages document
 *
 * @author Andrea Aime - TOPP
 */
public class WCSGetCoverageStoreResponse extends Response {

    static final Logger LOGGER = Logging.getLogger(WCSGetCoverageStoreResponse.class);

    GeoServer geoServer;
    Catalog catalog;
    CoverageResponseDelegateFinder responseFactory;

    public WCSGetCoverageStoreResponse(
            GeoServer gs, CoverageResponseDelegateFinder responseFactory) {
        super(GridCoverage[].class);
        this.geoServer = gs;
        this.catalog = gs.getCatalog();
        this.responseFactory = responseFactory;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }

    @Override
    public boolean canHandle(Operation operation) {
        // this one can handle GetCoverage responses where store = false
        if (!(operation.getParameters()[0] instanceof GetCoverageType)) return false;

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        return getCoverage.getOutput().isStore();
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GridCoverage[] coverages = (GridCoverage[]) value;

        // grab the delegate for coverage encoding
        GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = request.getOutput().getFormat();
        CoverageResponseDelegate delegate = responseFactory.encoderFor(outputFormat);
        if (delegate == null)
            throw new WcsException("Could not find encoder for output format " + outputFormat);

        // grab the coverage info for Coverages document encoding
        final GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        CoverageInfo coverageInfo = catalog.getCoverageByName(request.getIdentifier().getValue());

        // write the coverage to temporary storage in the data dir
        Resource wcsStore = null;
        try {
            GeoServerResourceLoader loader = geoServer.getCatalog().getResourceLoader();
            wcsStore = loader.get("temp/wcs");
        } catch (Exception e) {
            throw new WcsException("Could not create the temporary storage directory for WCS");
        }

        // Make sure we create a file name that's not already there (even if splitting the same
        // nanosecond
        // with two requests should not ever happen...)
        Resource coverageFile = null;
        while (true) {
            // TODO: find a way to get good extensions
            coverageFile =
                    wcsStore.get(
                            coverageInfo.getName().replace(':', '_')
                                    + "_"
                                    + System.nanoTime()
                                    + "."
                                    + delegate.getFileExtension(outputFormat));
            if (!Resources.exists(coverageFile)) break;
        }

        // store the coverage
        try (OutputStream os = new BufferedOutputStream(coverageFile.out())) {
            delegate.encode(coverage, outputFormat, Collections.EMPTY_MAP, os);
            os.flush();
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Saving coverage to temp file: " + coverageFile);
        }

        // build the path where the clients will be able to retrieve the coverage files
        final String coverageLocation =
                buildURL(
                        request.getBaseUrl(),
                        appendPath("temp/wcs", coverageFile.name()),
                        null,
                        URLType.RESOURCE);

        // build the response
        CoveragesTransformer tx = new CoveragesTransformer(request, coverageLocation);
        try {
            tx.transform(coverageInfo, output);
        } catch (TransformerException e) {
            throw new WcsException("Failure trying to encode Coverages response", e);
        }
    }
}

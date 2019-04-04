/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.ExtensionType;
import net.opengis.wcs20.GetCoverageType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.Response;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geoserver.platform.Operation;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Returns a single coverage encoded in the specified output format (eventually the native one)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20GetCoverageResponse extends Response {

    public static final String COVERAGE_ID_PARAM = "coverageId";

    CoverageResponseDelegateFinder responseFactory;

    public WCS20GetCoverageResponse(CoverageResponseDelegateFinder responseFactory) {
        super(GridCoverage.class);
        this.responseFactory = responseFactory;
    }

    public String getMimeType(Object value, Operation operation) {
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String format = getCoverage.getFormat();
        if (format == null) {
            return "image/tiff";
        } else {
            CoverageResponseDelegate delegate = responseFactory.encoderFor(format);
            if (delegate == null) {
                throw new WCS20Exception(
                        "Unsupported format " + format,
                        OWSExceptionCode.InvalidParameterValue,
                        "format");
            } else {
                return format;
            }
        }
    }

    @Override
    public boolean canHandle(Operation operation) {
        Object firstParam = operation.getParameters()[0];
        if (!(firstParam instanceof GetCoverageType)) {
            // we only handle WCS 2.0 requests
            return false;
        }

        GetCoverageType getCoverage = (GetCoverageType) firstParam;
        // this class only handles encoding the coverage in its output format
        return getCoverage.getMediaType() == null;
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // grab the coverage
        GridCoverage2D coverage = (GridCoverage2D) value;

        // grab the format
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String format = getCoverage.getFormat();
        if (format == null) {
            format = "image/tiff";
        }

        // extract additional extensions
        final Map<String, String> encodingParameters = new HashMap<String, String>();
        final ExtensionType extension = getCoverage.getExtension();
        if (extension != null) {
            final EList<ExtensionItemType> extensions = extension.getContents();
            for (ExtensionItemType ext : extensions) {
                encodingParameters.put(ext.getName(), ext.getSimpleContent());
            }
        }

        String coverageId = getCoverage.getCoverageId();
        if (coverageId != null) {
            encodingParameters.put(COVERAGE_ID_PARAM, coverageId);
        }

        // grab the delegate
        CoverageResponseDelegate delegate = responseFactory.encoderFor(format);
        delegate.encode(coverage, format, encodingParameters, output);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        // grab the format
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String format = getCoverage.getFormat();
        if (format == null) {
            format = "image/tiff";
        }

        // grab the delegate and thus the extension
        CoverageResponseDelegate delegate = responseFactory.encoderFor(format);

        // collect the name of the coverages that have been requested
        return delegate.getFileName((GridCoverage2D) value, getCoverage.getCoverageId(), format);
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import java.io.IOException;
import java.util.function.Predicate;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.MessageConverterResponseAdapter;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.response.WCS20GetCoverageResponse;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Adapts all output formats able to encode a WCS {@link org.opengis.coverage.grid.GridCoverage} to
 * a {@link org.springframework.http.converter.HttpMessageConverter} encoding a {@link
 * CoveragesResponse}. Allows to reuse all existing WCS output formats in the OGC Coverages API
 * implementation.
 */
@Component
public class CoverageResponseMessageConverter
        extends MessageConverterResponseAdapter<CoveragesResponse> {

    public CoverageResponseMessageConverter() {
        super(CoveragesResponse.class, GridCoverage.class);
    }

    @Override
    protected void writeResponse(
            CoveragesResponse value,
            HttpOutputMessage httpOutputMessage,
            Operation operation,
            Response response)
            throws IOException {
        try {
            setHeaders(value.getResponse(), operation, response, httpOutputMessage);
            response.write(value.getResponse(), httpOutputMessage.getBody(), operation);
        } catch (RuntimeException e) {
            // workaround, we really need to find out if the output format can handle the
            // coverage based on reader metadata instead
            if (e.getMessage().contains("Unable to render RenderedOp for this operation")) {
                throw new APIException(
                        ServiceException.NO_APPLICABLE_CODE,
                        "Cannot encode this coverage in the requested format",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        e);
            }
        }
    }

    @Override
    protected void setHeaders(
            Object result, Operation operation, Response response, HttpOutputMessage message) {
        // nothing to do so far
    }

    @Override
    protected Operation getOperation(CoveragesResponse result, Request dr, MediaType mediaType) {
        Operation op = dr.getOperation();
        // need to update the request with the requested media type
        GetCoverageType request = (GetCoverageType) result.getRequest();
        request.setFormat(mediaType.toString());
        return new Operation(
                "GetCoverage", op.getService(), op.getMethod(), new Object[] {request});
    }

    @Override
    protected Predicate<Response> getResponseFilterPredicate() {
        return r -> r instanceof WCS20GetCoverageResponse;
    }
}

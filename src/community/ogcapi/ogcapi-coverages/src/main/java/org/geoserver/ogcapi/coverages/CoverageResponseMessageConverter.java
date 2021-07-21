/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.coverages;

import java.io.IOException;
import java.util.function.Predicate;
import org.geoserver.ogcapi.MessageConverterResponseAdapter;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wcs2_0.response.WCS20GetCoverageResponse;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.http.HttpOutputMessage;
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
        setHeaders(value.getResponse(), operation, response, httpOutputMessage);
        response.write(value.getResponse(), httpOutputMessage.getBody(), operation);
    }

    @Override
    protected void setHeaders(
            Object result, Operation operation, Response response, HttpOutputMessage message) {
        // nothing to do so far
    }

    @Override
    protected Operation getOperation(CoveragesResponse result, Request dr) {
        Operation op = dr.getOperation();
        return new Operation(
                "GetCoverage", op.getService(), op.getMethod(), new Object[] {result.getRequest()});
    }

    @Override
    protected Predicate<Response> getResponseFilterPredicate() {
        return r -> r instanceof WCS20GetCoverageResponse;
    }
}

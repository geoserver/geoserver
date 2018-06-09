/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.ServiceException;

/**
 * Wrapping service exception handler that wraps content from a delegate handler in a soap Fault
 * wrapper.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SOAPServiceExceptionHandler extends ServiceExceptionHandler {

    ServiceExceptionHandler delegate;

    public SOAPServiceExceptionHandler(ServiceExceptionHandler delegate) {
        super(delegate.getServices());
        this.delegate = delegate;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {

        HttpServletResponse response = request.getHttpResponse();
        response.setContentType(Dispatcher.SOAP_MIME);

        try {
            // write out the Fault header
            StringBuilder sb =
                    new StringBuilder("<soap:Fault xmlns:soap='")
                            .append(request.getSOAPNamespace())
                            .append("'>");
            if (exception.getCode() != null) {
                sb.append("<soap:faultcode>")
                        .append(exception.getCode())
                        .append("</soap:faultcode>");
            }
            sb.append("<soap:faultstring>")
                    .append(exception.getLocalizedMessage())
                    .append("</soap:faultstring>");
            sb.append("<soap:detail>");
            response.getOutputStream().write(sb.toString().getBytes());

            // delegate
            delegate.handleServiceException(exception, request);

            // write out the Fault footer
            response.getOutputStream().write("</soap:detail></soap:Fault>".getBytes());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error encoding SOAP fault", e);
        }
    }
}

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.core.exception.ServiceError;
import com.boundlessgeo.gsr.core.exception.ServiceException;
import com.boundlessgeo.gsr.core.map.AbstractLayerOrTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves unhandled exceptions by converting them to {@link ServiceException}, then encoding that to json via
 * {@link GeoServicesJacksonJsonConverter}
 *
 * TODO: If this ever supports more than f=json, look up the right converter programmatically.
 */
@Component
public class GeoServicesExceptionResolver extends AbstractHandlerExceptionResolver {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(GeoServicesExceptionResolver.class);

    @Autowired
    GeoServicesJacksonJsonConverter converter;

    public GeoServicesExceptionResolver() {
        setOrder(0);
    }

    /**
     * Checks if the ServletPath of the {@link HttpServletRequest} is "/gsr", in which case it applies this
     * exception handler
     */
    @Override
    protected boolean shouldApplyTo(HttpServletRequest request, Object handler) {
        return "/gsr".equals(request.getServletPath());
    }

    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ServiceException exception = new ServiceException(new ServiceError(
                500, "Internal Server Error", Collections.singletonList(ex.getMessage())));

        //Log the full stack trace, since the response just has the error message.
        LOGGER.log(Level.INFO, "Error handling request", ex);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        try {
            converter.writeToOutputStream(response.getOutputStream(), exception);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing exception response", e);
        }
        return new ModelAndView();
    }
}

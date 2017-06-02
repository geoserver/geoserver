package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.core.exception.ServiceError;
import com.boundlessgeo.gsr.core.exception.ServiceException;
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

/**
 * Resolves unhandled exceptions by converting them to {@link ServiceException}, then encoding that to json via
 * {@link GeoServicesJSONConverter}
 *
 * TODO: If this ever supports more than f=json, look up the right converter programmatically.
 */
@Component
public class GeoServicesExceptionResolver extends AbstractHandlerExceptionResolver {

    @Autowired
    GeoServicesJSONConverter converter;

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

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        try {
            converter.getXStream().toXML(exception, response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView();
    }
}

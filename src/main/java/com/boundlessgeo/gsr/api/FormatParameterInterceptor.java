package com.boundlessgeo.gsr.api;

import org.geoserver.rest.RestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the "f" parameter in a way that does not interfere with the regular geoserver api
 */
public class FormatParameterInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("gsr".equals(request.getContextPath())) {
            String format = request.getParameter("f");
            if (null == format) {
                //TODO: What do we do if no format is set?
            } else if ("json".equals(format)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                return true;
            }
            throw new RestException("Format " + format + " is not supported", HttpStatus.BAD_REQUEST);
        }
        return true;
    }
}


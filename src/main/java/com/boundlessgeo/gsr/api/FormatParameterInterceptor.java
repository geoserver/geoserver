/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.core.exception.ServiceError;
import com.boundlessgeo.gsr.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * Handles the "f" parameter for all gsr api requests
 *
 * TODO: If this ever supports more than f=json, look up the right converter programmatically.
 */
public class FormatParameterInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    GeoServicesJacksonJsonConverter converter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("/gsr".equals(request.getServletPath())) {
            String format = request.getParameter("f");
            if (null == format) {
                //TODO: What do we do if no format is set? Choose a default?
            } else if ("json".equals(format)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                return true;
            }
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            converter.getMapper().writeValue(response.getOutputStream(), new ServiceException(new ServiceError(
                    HttpStatus.BAD_REQUEST.value(), "Output format not supported", Collections.singletonList("Format " + format + " is not supported")
            )));
            return false;
        }
        return true;
    }
}


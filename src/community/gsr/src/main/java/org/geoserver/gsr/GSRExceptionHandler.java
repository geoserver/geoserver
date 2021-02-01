/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.gsr.api.ServiceException;
import org.geoserver.gsr.model.exception.ServiceError;
import org.geoserver.ogcapi.APIExceptionHandler;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

@Component
public class GSRExceptionHandler implements APIExceptionHandler {
    static final Logger LOGGER = Logging.getLogger(GSRExceptionHandler.class);

    @Override
    public boolean canHandle(Throwable throwable, APIRequestInfo apiRequestInfo) {
        return apiRequestInfo.getRequest().getServletPath().equals("/gsr");
    }

    @Override
    public void handle(Throwable throwable, HttpServletResponse httpServletResponse) {
        Map<String, Object> error = new LinkedHashMap<>();
        if (throwable instanceof ServiceException) {
            ServiceException exception = (ServiceException) throwable;
            error.put("error", exception.getError());
        } else {
            error.put(
                    "error",
                    new ServiceError(
                            500,
                            throwable.getMessage(),
                            Collections.singletonList(throwable.getMessage())));
        }
        try (ServletOutputStream os = httpServletResponse.getOutputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(os, error);
            os.flush();
        } catch (Exception ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        }
    }
}

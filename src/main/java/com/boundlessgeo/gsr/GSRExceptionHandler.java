package com.boundlessgeo.gsr;

import com.boundlessgeo.gsr.api.ServiceException;
import com.boundlessgeo.gsr.model.exception.ServiceError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geoserver.api.APIExceptionHandler;
import org.geoserver.api.APIRequestInfo;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            error.put("error", new ServiceError(500, throwable.getMessage(), Collections.singletonList(throwable.getMessage())));
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            ServletOutputStream os = httpServletResponse.getOutputStream();
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

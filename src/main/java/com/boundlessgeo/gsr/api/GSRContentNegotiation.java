package com.boundlessgeo.gsr.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * This exists to work around the fact that older versions of the ESRI send along an image format parameter
 * even when they request JSON
 */
@Component public class GSRContentNegotiation implements ContentNegotiationStrategy {
    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        if (request.getServletPath().startsWith("/gsr")) {
            List<MediaType> acceptedMediaTypes = new ArrayList<>();
            String contentType = webRequest.getHeader("Content-Type");
            if (contentType != null && !contentType.equals("*/*")) {
                acceptedMediaTypes.add(MediaType.valueOf(contentType));
            }

            if (webRequest.getParameter("f") != null && "json".equals(webRequest.getParameter("f"))) {
                acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
            }

            return acceptedMediaTypes;
        } else {
            return Collections.emptyList();
        }
    }
}

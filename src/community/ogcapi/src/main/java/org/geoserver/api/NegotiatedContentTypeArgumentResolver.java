/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */
package org.geoserver.api;

import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class NegotiatedContentTypeArgumentResolver implements HandlerMethodArgumentResolver {

    APIContentNegotiationManager contenTypeNegotiator = new APIContentNegotiationManager();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(NegotiatedContentType.class) != null
                && MediaType.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory)
            throws Exception {
        List<MediaType> mediaTypes = contenTypeNegotiator.resolveMediaTypes(webRequest);
        MediaType response = mediaTypes.get(0);
        return response == MediaType.ALL ? null : response;
    }
}

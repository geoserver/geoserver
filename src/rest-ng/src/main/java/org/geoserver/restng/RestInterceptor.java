package org.geoserver.restng;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor for all rest-ng requests
 *
 * Adds a {@link RequestInfo} to the request attributes
 */
public class RestInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        RequestContextHolder.getRequestAttributes().setAttribute( RequestInfo.KEY, new RequestInfo(request), RequestAttributes.SCOPE_REQUEST );

        return true;
    }
}

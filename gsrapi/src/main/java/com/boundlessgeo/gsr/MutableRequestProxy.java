package com.boundlessgeo.gsr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Just a simple servlet request decorator that allows for changing request parameters. Used for manually invoking the
 * dispatcher in order to forward requests.
 */
public class MutableRequestProxy extends HttpServletRequestWrapper {

    private Map<String, String[]> mutableParams = new HashMap<>();

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request The wrapped request
     * @throws IllegalArgumentException if the request is null
     */
    public MutableRequestProxy(HttpServletRequest request) {
        super(request);
        mutableParams.putAll(request.getParameterMap());
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(mutableParams);
    }

    public Map<String, String[]> getMutableParams() {
        return mutableParams;
    }

    @Override
    public String getParameter(String name) {
        String param = super.getParameter(name);
        if (param == null) {
            param = super.getParameter(name.toUpperCase());
        }
        return param;
    }
}

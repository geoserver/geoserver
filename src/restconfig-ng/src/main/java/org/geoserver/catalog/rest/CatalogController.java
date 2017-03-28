/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.rest.RestBaseController;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

/**
 * Base controller for catalog info requests
 */
public class CatalogController extends RestBaseController implements RequestBodyAdvice {
    
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String TEXT_JSON = "text/json";
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String APPLICATION_ZIP = "application/zip";

    protected final Catalog catalog;
    protected final GeoServerDataDirectory dataDir;

    protected final List<String> validImageFileExtensions;

    public CatalogController(Catalog catalog) {
        super();
        this.catalog = catalog;
        this.dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        this.validImageFileExtensions = Arrays.asList("svg", "png", "jpg");
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return false;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }
}

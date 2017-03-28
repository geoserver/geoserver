package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@ControllerAdvice
public class CoverageStoreControllerAdvice extends CatalogControllerAdvice {
    @Autowired
    public CoverageStoreControllerAdvice(Catalog catalog) {
        super(catalog);
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return CoverageStoreInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return new RestHttpInputWrapper(inputMessage) {
            @Override
            public void configurePersister(XStreamPersister persister, XStreamMessageConverter xStreamMessageConverter) {
                persister.setCallback(new XStreamPersister.Callback() {
                    @Override
                    protected Class<CoverageStoreInfo> getObjectClass() {
                        return CoverageStoreInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder.getRequestAttributes().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                        String workspace = uriTemplateVars.get("workspace");
                        String coveragestore = uriTemplateVars.get("store");

                        if (workspace == null || coveragestore == null) {
                            return null;
                        }
                        return catalog.getCoverageStoreByName(workspace, coveragestore);
                    }
                });
            }
        };
    }
}

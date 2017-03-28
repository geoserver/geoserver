package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Catalog-aware extension of {@link RequestBodyAdviceAdapter}
 */
public abstract class CatalogControllerAdvice extends RequestBodyAdviceAdapter {
    Catalog catalog;

    public CatalogControllerAdvice(Catalog catalog) {
        this.catalog = catalog;
    }
}

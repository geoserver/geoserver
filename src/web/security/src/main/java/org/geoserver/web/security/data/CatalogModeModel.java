package org.geoserver.web.security.data;

import java.io.Serializable;

import org.geoserver.security.CatalogMode;

public class CatalogModeModel implements Serializable {

    CatalogMode catalogMode;

    public CatalogModeModel(CatalogMode catalogMode) {
        setCatalogMode(catalogMode);
    }

    public CatalogMode getCatalogMode() {
        return catalogMode;
    }

    public void setCatalogMode(CatalogMode catalogMode) {
        this.catalogMode = catalogMode;
    }

}

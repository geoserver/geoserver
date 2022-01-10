/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.io.Serializable;
import org.geoserver.security.CatalogMode;

public class CatalogModeModel implements Serializable {

    private static final long serialVersionUID = 1L;

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

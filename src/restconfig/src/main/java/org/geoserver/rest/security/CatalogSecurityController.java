/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** CatalogSecurityController provides REST endpoints to Reload GeoServer's SecurityManager */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/acl/catalog")
public class CatalogSecurityController {

    GeoServerSecurityManager getSecurityManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    protected void checkUserIsAdmin() {
        if (!getSecurityManager().checkAuthenticationForAdminRole()) {
            throw new RestException("Administrative privileges required", HttpStatus.FORBIDDEN);
        }
    }

    // Existing endpoints for GET/PUT on catalog mode...
    // -----------------------------------------------

    /** Reload endpoint for /acl/catalog POST or PUT /geoserver/rest/security/acl/catalog/reload */
    @RequestMapping(
            value = "/reload",
            method = {RequestMethod.POST, RequestMethod.PUT})
    public void reloadCatalogSecurity() throws Exception {
        checkUserIsAdmin();
        getSecurityManager().reload();
    }
}

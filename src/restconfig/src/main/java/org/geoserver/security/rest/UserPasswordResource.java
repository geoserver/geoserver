/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.RestletException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;

import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;
import org.restlet.data.Status;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * REST Resource for changing a user's password
 *
 * @author Emanuele Tajariol <etj at geo-solutions.it>
 */
public class UserPasswordResource extends MapResource {

    static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.rest");

    static final String UP_NEW_PW = "newPassword";
    static final String XML_ROOT_ELEM = "userPassword";

    Map putMap;

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    /**
     * PUT is allowed if the user is authenticated, AND
     * the provider which authenticated the user is not readonly.
     */
    @Override
    public boolean allowPut() {
        if ( ! getManager().checkAuthenticationForRole(
                SecurityContextHolder.getContext().getAuthentication(),
                GeoServerRole.AUTHENTICATED_ROLE))
            return false;

        try {
            // Look for the service that handles the current user
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();

            GeoServerUserGroupService ugService = null;

            for (GeoServerUserGroupService service : getManager().loadUserGroupServices()) {
                if (service.getUserByUsername(userName) != null) {
                    ugService = service;
                    break;
                }
            }

            if (ugService == null) {
                throw new RestletException("Cannot calculate if PUT is allowed (service not found)",
                        Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
            }

            // Check if the provider allows updates
            return ugService.canCreateStore();

        } catch (IOException e) {
            throw new RestletException("Cannot calculate if PUT is allowed ("+e.getMessage()+")",
                    Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e);
        }
    }

    @Override
    public Map getMap() throws Exception {
        return Collections.EMPTY_MAP;
    }

    @Override
    protected void putMap(Map map) throws Exception {
        putMap = map;
    }

    /**
     * Trigger a user password change
     */
    @Override
    public void handlePut() {
        super.handlePut();

        String newpass = (String) putMap.get(UP_NEW_PW);

        if (StringUtils.isBlank(newpass))
            throw new RestletException("Missing '" + UP_NEW_PW + "'",
                    Status.CLIENT_ERROR_BAD_REQUEST);


        GeoServerUser user = null;
        GeoServerUserGroupService ugService = null;

        try {
            // Look for the authentication service
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();

            for (GeoServerUserGroupService service : getManager().loadUserGroupServices()) {
                user = service.getUserByUsername(userName);
                if (user != null) {
                    ugService = service;
                    break;
                }
            }
        } catch(IOException e) {
            throw new RestletException("Cannot retrieve user service",
                    Status.CLIENT_ERROR_FAILED_DEPENDENCY, e);
        }

        if (ugService == null) {
            throw new RestletException("User service not found",
                    Status.CLIENT_ERROR_FAILED_DEPENDENCY);
        }

        // Check again if the provider allows updates
        if ( ! ugService.canCreateStore()) {
            throw new RestletException("User service does not support changing pw",
                    Status.CLIENT_ERROR_FAILED_DEPENDENCY);
        }

        try {
            UserGroupStoreValidationWrapper ugStore = new UserGroupStoreValidationWrapper(ugService.createStore());

            user.setPassword(newpass);
            ugStore.updateUser(user);

            ugStore.store();
            ugService.load();

            LOGGER.log(Level.INFO, "Changed password for user {0}", user.getUsername());

        } catch (IOException e) {
            throw new RestletException("Internal IO error",
                    Status.SERVER_ERROR_INTERNAL, e);
        } catch (PasswordPolicyException e) {
            throw new RestletException("Bad password",
                    Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e);
        }
    }

}

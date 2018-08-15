/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/self/password")
public class UserPasswordController extends RestBaseController {
    static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.rest");

    static final String UP_NEW_PW = "newPassword";

    static final String XML_ROOT_ELEM = "userPassword";

    @GetMapping()
    public void passwordGet() {
        throw new RestException("You can not request the password!", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @PutMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        }
    )
    public void passwordPut(@RequestBody Map<String, String> putMap) {
        if (!getManager()
                .checkAuthenticationForRole(
                        SecurityContextHolder.getContext().getAuthentication(),
                        GeoServerRole.AUTHENTICATED_ROLE))
            // yes, for backwards compat, it's really METHOD_NOT_ALLOWED
            throw new RestException(
                    "Amdinistrative privelges required", HttpStatus.METHOD_NOT_ALLOWED);

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
                throw new RestException(
                        "Cannot calculate if PUT is allowed (service not found)",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

        } catch (IOException e) {
            throw new RestException(
                    "Cannot calculate if PUT is allowed (" + e.getMessage() + ")",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    e);
        }
        String newpass = putMap.get(UP_NEW_PW);

        if (StringUtils.isBlank(newpass))
            throw new RestException("Missing '" + UP_NEW_PW + "'", HttpStatus.BAD_REQUEST);

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
        } catch (IOException e) {
            throw new RestException(
                    "Cannot retrieve user service", HttpStatus.FAILED_DEPENDENCY, e);
        }

        if (ugService == null) {
            throw new RestException("User service not found", HttpStatus.FAILED_DEPENDENCY);
        }

        // Check again if the provider allows updates
        if (!ugService.canCreateStore()) {
            throw new RestException(
                    "User service does not support changing pw", HttpStatus.FAILED_DEPENDENCY);
        }

        try {
            UserGroupStoreValidationWrapper ugStore =
                    new UserGroupStoreValidationWrapper(ugService.createStore());

            user.setPassword(newpass);
            ugStore.updateUser(user);

            ugStore.store();
            ugService.load();

            LOGGER.log(Level.INFO, "Changed password for user {0}", user.getUsername());

        } catch (IOException e) {
            throw new RestException("Internal IO error", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } catch (PasswordPolicyException e) {
            throw new RestException("Bad password", HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }
}

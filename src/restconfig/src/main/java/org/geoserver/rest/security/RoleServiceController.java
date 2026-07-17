/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import com.google.common.base.Strings;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.RoleServiceSummary;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST controller to manage Role Services (GeoServerRoleService) configurations. */
@RestController
@RequestMapping(
        path = RestBaseController.ROOT_PATH + "/security/roleservices",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class RoleServiceController extends RestBaseController {

    private static final Logger LOGGER = Logger.getLogger(RoleServiceController.class.getName());

    private final GeoServerSecurityManager securityManager;

    // Extra safety net for the bootstrap "default" service, matching UserGroupServiceController's identical
    // convention. This does NOT track which role service is actually active: the real, general protection against
    // deleting the in-use role service (whatever its name) lives in
    // SecurityConfigValidator.validateRemoveRoleService, which removeRoleService() below already goes through and
    // surfaces as a SecurityConfigException -> 400.
    private static final Set<String> DELETE_BLACK_LIST = Set.of("default");

    public RoleServiceController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    // ---------------------------------------------------------------------
    // REST API
    // ---------------------------------------------------------------------

    // 200, 403
    @GetMapping
    public RestWrapper<RoleServiceSummary> list() {
        checkAuthorisation();
        List<RoleServiceSummary> result = loadRoleServices();
        return wrapList(result, RoleServiceSummary.class);
    }

    // 200, 403, 404
    @GetMapping(value = "/{serviceName}")
    public RestWrapper<SecurityRoleServiceConfig> view(@PathVariable("serviceName") String serviceName) {
        checkAuthorisation();
        try {
            SecurityRoleServiceConfig cfg = securityManager.loadRoleServiceConfig(serviceName);
            if (cfg == null) {
                throw new ResourceNotFoundException("Cannot find role service %s".formatted(serviceName));
            }
            return wrapObject(cfg, SecurityRoleServiceConfig.class);
        } catch (IOException e) {
            throw new RestException("Cannot load role service config", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    // 201, 400, 403
    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public @ResponseStatus(code = HttpStatus.CREATED) RestWrapper<SecurityRoleServiceConfig> post(
            @RequestBody SecurityRoleServiceConfig request) {
        checkAuthorisation();
        SecurityRoleServiceConfig saved = saveRoleService(request);
        return wrapObject(saved, SecurityRoleServiceConfig.class);
    }

    // 200, 400, 404, 403
    @PutMapping(
            value = "/{serviceName}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public @ResponseStatus(code = HttpStatus.OK) void put(
            @PathVariable("serviceName") String serviceName, @RequestBody SecurityRoleServiceConfig request) {
        checkAuthorisation();
        updateRoleService(serviceName, request);
    }

    // 200, 404, 403
    @DeleteMapping(value = "/{serviceName}")
    public @ResponseStatus(code = HttpStatus.OK) void delete(@PathVariable("serviceName") String serviceName) {
        checkAuthorisation();
        removeRoleService(serviceName);
    }

    // ---------------------------------------------------------------------
    // Controller Advice
    // ---------------------------------------------------------------------

    private void checkAuthorisation() {
        if (!securityManager.checkAuthenticationForAdminRole()) {
            throw new RestException("Admin role required to access this resource", HttpStatus.FORBIDDEN);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.getXStream().allowTypesByWildcard(new String[] {
            "org.geoserver.security.**",
            "org.geoserver.security.config.**",
            "org.geoserver.rest.security.xml.**",
            getClass().getPackage().getName() + ".**"
        });

        persister.getXStream().processAnnotations(new Class[] {RoleServiceSummary.class});

        super.configurePersister(persister, converter);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return RoleServiceSummary.class.isAssignableFrom(methodParameter.getParameterType())
                || SecurityRoleServiceConfig.class.isAssignableFrom(methodParameter.getParameterType());
    }

    // ---------------------------------------------------------------------
    // Internal logic
    // ---------------------------------------------------------------------

    protected List<RoleServiceSummary> loadRoleServices() {
        try {
            Set<String> names = securityManager.listRoleServices();
            List<RoleServiceSummary> out = new ArrayList<>();
            for (String name : names) {
                SecurityRoleServiceConfig cfg = securityManager.loadRoleServiceConfig(name);
                if (cfg != null) {
                    out.add(RoleServiceSummary.from(cfg));
                }
            }
            return out;
        } catch (IOException ex) {
            throw new RestException("Cannot list role services", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }

    protected SecurityRoleServiceConfig saveRoleService(SecurityRoleServiceConfig newCfg) {
        if (newCfg == null) {
            throw new RestException("Request body is empty", HttpStatus.BAD_REQUEST);
        }
        if (Strings.isNullOrEmpty(newCfg.getName())) {
            LOGGER.warning("Cannot create role service: missing name");
            throw new RestException("Cannot create the config: no name parameter provided", HttpStatus.BAD_REQUEST);
        }

        try {
            if (securityManager.loadRoleServiceConfig(newCfg.getName()) != null) {
                LOGGER.warning("Cannot create role service %s: name already exists".formatted(newCfg.getName()));
                throw new RestException(
                        "Cannot create the config %s because the name is already in use".formatted(newCfg.getName()),
                        HttpStatus.BAD_REQUEST);
            }
        } catch (IOException ex) {
            throw new RestException("Cannot access role service configs", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }

        try {
            // Validation happens inside saveRoleService; throws SecurityConfigException if invalid.
            // No reload() here: saveRoleService() already evicts the stale cache entry and, if this is the
            // active role service, refreshes it in place - a full reload() would re-run security migration
            // checks and reinitialize every unrelated security provider for no benefit.
            securityManager.saveRoleService(newCfg);
        } catch (SecurityConfigException sce) {
            // configuration validation problem -> client error
            throw new RestException(sce.getMessage(), HttpStatus.BAD_REQUEST, sce);
        } catch (IOException ioe) {
            // persistence problem -> server error
            throw new RestException(
                    "Cannot save role service " + newCfg.getName(), HttpStatus.INTERNAL_SERVER_ERROR, ioe);
        }

        try {
            return securityManager.loadRoleServiceConfig(newCfg.getName());
        } catch (IOException ex) {
            throw new RestException(
                    "Cannot reload role service " + newCfg.getName() + " after save",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex);
        }
    }

    protected void updateRoleService(String serviceName, SecurityRoleServiceConfig request) {
        if (request == null) {
            throw new RestException("Request body is empty", HttpStatus.BAD_REQUEST);
        }
        if (!serviceName.equals(request.getName())) {
            LOGGER.warning("Cannot modify service %s because the name %s in the body does not match"
                    .formatted(serviceName, request.getName()));
            throw new RestException(
                    "Cannot modify the config %s because the name %s in the body does not match"
                            .formatted(serviceName, request.getName()),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            SecurityRoleServiceConfig existing = securityManager.loadRoleServiceConfig(serviceName);
            if (existing == null) {
                LOGGER.warning("Cannot update %s because it does not exist".formatted(serviceName));
                // for this API we use 400 (matches existing tests/behavior)
                throw new RestException(
                        "Cannot update %s because it does not exist".formatted(serviceName), HttpStatus.BAD_REQUEST);
            }

            // keep id stable
            request.setId(existing.getId());

            // Validation happens inside saveRoleService; see the comment in saveRoleService() above for why no
            // reload() is needed here.
            securityManager.saveRoleService(request);
        } catch (SecurityConfigException sce) {
            throw new RestException(sce.getMessage(), HttpStatus.BAD_REQUEST, sce);
        } catch (IOException ioe) {
            throw new RestException("Cannot update role service " + serviceName, HttpStatus.INTERNAL_SERVER_ERROR, ioe);
        }
    }

    protected void removeRoleService(String serviceName) {
        if (DELETE_BLACK_LIST.contains(serviceName)) {
            LOGGER.warning("Cannot delete %s because it is a required role service".formatted(serviceName));
            throw new RestException(
                    "Cannot delete %s because it is a required role service".formatted(serviceName),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            SecurityRoleServiceConfig cfg = securityManager.loadRoleServiceConfig(serviceName);
            if (cfg == null) {
                LOGGER.warning("Cannot delete %s because it does not exist".formatted(serviceName));
                throw new ResourceNotFoundException("Cannot find role service " + serviceName);
            }
            // No reload() here: removeRoleService() already evicts the cache entry and deletes the config
            // file; validateRemoveRoleService() (called inside it) already refuses to remove the active
            // role service, so there's no stale in-memory reference left to clean up.
            securityManager.removeRoleService(cfg);
        } catch (SecurityConfigException sce) {
            throw new RestException(sce.getMessage(), HttpStatus.BAD_REQUEST, sce);
        } catch (IOException ioe) {
            throw new RestException("Cannot remove role service " + serviceName, HttpStatus.INTERNAL_SERVER_ERROR, ioe);
        }
    }
}

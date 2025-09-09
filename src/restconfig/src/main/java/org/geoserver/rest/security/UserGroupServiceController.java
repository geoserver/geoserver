/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static java.lang.String.format;

import com.google.common.base.Strings;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.UserGroupServiceSummary;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** REST controller to manage User/Group Services (UserGroupServer) configurations. */
@RestController
@RequestMapping(
        path = RestBaseController.ROOT_PATH + "/security/usergroupservices",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class UserGroupServiceController extends RestBaseController {

    private static final Logger LOGGER = Logger.getLogger(UserGroupServiceController.class.getName());

    private final GeoServerSecurityManager securityManager;

    // prevent accidental removal of core services
    private static final Set<String> DELETE_BLACK_LIST = Set.of("default");

    public UserGroupServiceController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    // ---------------------------------------------------------------------
    // REST API
    // ---------------------------------------------------------------------

    // 200, 403
    @GetMapping
    public RestWrapper<UserGroupServiceSummary> list() {
        checkAuthorisation();
        List<UserGroupServiceSummary> result = loadUserGroupServices();
        return wrapList(result, UserGroupServiceSummary.class);
    }

    // 200, 403, 404
    @GetMapping(value = "/{serviceName}")
    public RestWrapper<SecurityUserGroupServiceConfig> view(@PathVariable("serviceName") String serviceName) {
        checkAuthorisation();
        try {
            SecurityUserGroupServiceConfig cfg = securityManager.loadUserGroupServiceConfig(serviceName);
            if (cfg == null) {
                throw new IllegalArgumentException(format("Cannot find user/group service %s", serviceName));
            }
            return wrapObject(cfg, SecurityUserGroupServiceConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load user/group service config", e);
        }
    }

    // 201, 400, 403
    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<SecurityUserGroupServiceConfig> post(
            @RequestBody SecurityUserGroupServiceConfig request, UriComponentsBuilder uriComponentsBuilder) {
        checkAuthorisation();
        SecurityUserGroupServiceConfig saved = saveUserGroupService(request);
        return wrapObject(saved, SecurityUserGroupServiceConfig.class);
    }

    // 200, 400, 404, 403
    @PutMapping(
            value = "/{serviceName}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public @ResponseStatus(code = HttpStatus.OK) void put(
            @PathVariable("serviceName") String serviceName, @RequestBody SecurityUserGroupServiceConfig request) {
        checkAuthorisation();
        updateUserGroupService(serviceName, request);
    }

    // 200, 404, 403
    @DeleteMapping(value = "/{serviceName}")
    public @ResponseStatus(code = HttpStatus.OK) void delete(@PathVariable("serviceName") String serviceName) {
        checkAuthorisation();
        removeUserGroupService(serviceName);
    }

    // ---------------------------------------------------------------------
    // Controller Advice
    // ---------------------------------------------------------------------

    private void checkAuthorisation() {
        if (!securityManager.checkAuthenticationForAdminRole()) {
            throw new NotAuthorised();
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

        persister.getXStream().processAnnotations(new Class[] {UserGroupServiceSummary.class});

        super.configurePersister(persister, converter);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return UserGroupServiceSummary.class.isAssignableFrom(methodParameter.getParameterType())
                || SecurityUserGroupServiceConfig.class.isAssignableFrom(methodParameter.getParameterType());
    }

    // ---------------------------------------------------------------------
    // Internal logic
    // ---------------------------------------------------------------------

    protected List<UserGroupServiceSummary> loadUserGroupServices() {
        try {
            Set<String> names = securityManager.listUserGroupServices();
            List<UserGroupServiceSummary> out = new ArrayList<>();
            for (String name : names) {
                SecurityUserGroupServiceConfig cfg = securityManager.loadUserGroupServiceConfig(name);
                if (cfg != null) {
                    out.add(UserGroupServiceSummary.from(cfg));
                }
            }
            return out;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot list user/group services", ex);
        }
    }

    protected SecurityUserGroupServiceConfig saveUserGroupService(SecurityUserGroupServiceConfig newCfg) {
        if (newCfg == null) throw new BadRequest("Request body is empty");
        if (Strings.isNullOrEmpty(newCfg.getName())) {
            LOGGER.warning("Cannot create user/group service: missing name");
            throw new MissingNameException();
        }

        try {
            if (securityManager.loadUserGroupServiceConfig(newCfg.getName()) != null) {
                LOGGER.warning(format("Cannot create user/group service %s: name already exists", newCfg.getName()));
                throw new DuplicateNameException(newCfg.getName());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot access user/group service configs", ex);
        }

        try {
            // Validation happens inside saveUserGroupService; throws SecurityConfigException if invalid
            securityManager.saveUserGroupService(newCfg);
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot save user/group service " + newCfg.getName(), ex);
        }

        try {
            return securityManager.loadUserGroupServiceConfig(newCfg.getName());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot reload user/group service " + newCfg.getName() + " after save", ex);
        }
    }

    protected void updateUserGroupService(String serviceName, SecurityUserGroupServiceConfig request) {
        if (request == null) throw new BadRequest("Request body is empty");
        if (!serviceName.equals(request.getName())) {
            LOGGER.warning(format(
                    "Cannot modify service %s because the name %s in the body does not match",
                    serviceName, request.getName()));
            throw new NameMismatchException(serviceName, request.getName());
        }

        try {
            SecurityUserGroupServiceConfig existing = securityManager.loadUserGroupServiceConfig(serviceName);
            if (existing == null) {
                LOGGER.warning(format("Cannot update %s because it does not exist", serviceName));
                throw new IllegalArgumentException(format("Cannot update %s because it does not exist", serviceName));
            }

            // keep id stable
            request.setId(existing.getId());

            // Validation happens inside saveUserGroupService
            securityManager.saveUserGroupService(request);
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot update user/group service " + serviceName, ex);
        }
    }

    protected void removeUserGroupService(String serviceName) {
        if (DELETE_BLACK_LIST.contains(serviceName)) {
            LOGGER.warning(format("Cannot delete %s because it is a required user/group service", serviceName));
            throw new DeleteBlackListException(serviceName);
        }

        try {
            SecurityUserGroupServiceConfig cfg = securityManager.loadUserGroupServiceConfig(serviceName);
            if (cfg == null) {
                LOGGER.warning(format("Cannot delete %s because it does not exist", serviceName));
                throw new IllegalArgumentException("Cannot find user/group service " + serviceName);
            }
            securityManager.removeUserGroupService(cfg);
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot remove user/group service " + serviceName, ex);
        }
    }

    // ---------------------------------------------------------------------
    // Exception handlers (mirroring your example)
    // ---------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> somethingNotFound(IllegalArgumentException exception) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DeleteBlackListException.class)
    public ResponseEntity<ErrorResponse> blackListed(DeleteBlackListException exception) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NameMismatchException.class)
    public ResponseEntity<ErrorResponse> nameMismatched(NameMismatchException exception) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingNameException.class)
    public ResponseEntity<ErrorResponse> missingName(MissingNameException exception) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequest exception) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityConfigException.class)
    public ResponseEntity<ErrorResponse> securityIssue(SecurityConfigException exception) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> ioIssue(IOException exception) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------------
    // Exceptions
    // ---------------------------------------------------------------------
    public static class ErrorResponse {
        private int status;
        private String message;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class DeleteBlackListException extends RuntimeException {
        public DeleteBlackListException(String name) {
            super(format("Cannot delete %s because it is a required user/group service", name));
        }
    }

    public static class NameMismatchException extends RuntimeException {
        public NameMismatchException(String pathName, String configName) {
            super(format(
                    "Cannot modify the config %s because the name %s in the body does not match",
                    pathName, configName));
        }
    }

    public static class DuplicateNameException extends RuntimeException {
        public DuplicateNameException(String configName) {
            super(format("Cannot create the config %s because the name is already in use", configName));
        }
    }

    public static class MissingNameException extends RuntimeException {
        public MissingNameException() {
            super("Cannot create the config: no name parameter provided");
        }
    }

    public static class BadRequest extends RuntimeException {
        public BadRequest(String message) {
            super(message);
        }
    }

    public static class NotAuthorised extends RuntimeException {
        public NotAuthorised() {
            super("Admin role required to access this resource");
        }
    }
}

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
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.AuthFilter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
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

@RestController
@RequestMapping(
        path = RestBaseController.ROOT_PATH + "/security/authfilters",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class AuthenticationFilterController extends RestBaseController {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationFilterController.class.getName());

    private final GeoServerSecurityManager securityManager;
    private final FilterConfigValidator filterConfigValidator;

    private static final Set<String> DELETE_BLACK_LIST = Set.of("anonymous", "basic", "form", "rememberme");

    public AuthenticationFilterController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
        this.filterConfigValidator = new FilterConfigValidator(securityManager);
    }

    /// ///////////////////////////////////////////////////////////////////////
    /// REST API methods

    // 200
    // 403
    @GetMapping
    public RestWrapper<AuthFilter> list() {
        checkAuthorisation();
        List<AuthFilter> result = loadAuthFilters();
        return wrapList(result, AuthFilter.class);
    }

    // 200
    // 403
    // 404
    @GetMapping(value = "/{filterName}")
    public RestWrapper<SecurityFilterConfig> view(@PathVariable("filterName") String filterName) {
        checkAuthorisation();
        SecurityFilterConfig authfilter = null;
        try {
            authfilter = securityManager.loadFilterConfig(filterName, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return wrapObject(authfilter, SecurityFilterConfig.class);
    }

    // FilterConfigValidator to check if filter is valid
    // 201
    // 400
    // 403
    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<SecurityFilterConfig> post(
            @RequestBody SecurityFilterConfig authfilterRequest, UriComponentsBuilder uriComponentsBuilder) {
        checkAuthorisation();
        SecurityFilterConfig authfilterResponse = saveAuthFilter(authfilterRequest);
        return wrapObject(authfilterResponse, SecurityFilterConfig.class);
    }

    // 200
    // 400
    // 404
    @PutMapping(
            value = "/{filterName}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public @ResponseStatus(code = HttpStatus.OK) void put(
            @PathVariable("filterName") String filterName, @RequestBody SecurityFilterConfig authfilterRequest) {
        checkAuthorisation();
        updateAuthFilter(filterName, authfilterRequest);
    }

    // 200 when deleted
    // 404 if already deleted
    @DeleteMapping(value = "/{filterName}")
    public @ResponseStatus(code = HttpStatus.OK) void delete(@PathVariable("filterName") String filterName) {
        checkAuthorisation();
        removeAuthFilter(filterName);
    }
    /// ///////////////////////////////////////////////////////////////////////
    /// Controller Advice

    private void checkAuthorisation() {
        if (!securityManager.checkAuthenticationForAdminRole()) {
            throw new NotAuthorised();
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.getXStream().allowTypesByWildcard(new String[] {
            "org.geoserver.security.**", "org.geoserver.security.config.**", "org.geoserver.rest.security.xml.**"
        });

        persister.getXStream().processAnnotations(new Class[] {AuthFilter.class});

        super.configurePersister(persister, converter);
    }

    @Override
    /*
     * Any subclass that implements {@link #configurePersister(XStreamPersister, XStreamMessageConverter)} and require
     * this configuration for reading objects from incoming requests should override this method to return true when
     * called from the appropriate controller, and should also be annotated with
     * {@link org.springframework.web.bind.annotation.ControllerAdvice}
     */
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AuthFilter.class.isAssignableFrom(methodParameter.getParameterType());
    }

    /// ///////////////////////////////////////////////////////////////////////
    /// Internal logic

    protected List<AuthFilter> loadAuthFilters() {
        try {
            Set<String> authfilterNames = securityManager.listFilters(GeoServerAuthenticationFilter.class);
            List<AuthFilter> authfilters = new ArrayList<>();
            for (String filterName : authfilterNames) {
                SecurityFilterConfig securityFilterConfig = securityManager.loadFilterConfig(filterName, false);
                if (securityFilterConfig != null) {
                    AuthFilter filter = new AuthFilter(securityFilterConfig);
                    authfilters.add(filter);
                }
            }
            return authfilters;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load provider filters", ex);
        }
    }

    protected SecurityFilterConfig saveAuthFilter(SecurityFilterConfig newFilter) {
        if (Strings.isNullOrEmpty(newFilter.getName())) {
            LOGGER.warning("Cannot create the filter %s because client has not provided a name"
                    .formatted(newFilter.getName()));
            throw new MissingNameException();
        }

        try {
            if (securityManager.loadFilterConfig(newFilter.getName(), false) != null) {
                LOGGER.warning("Cannot create the filter %s because a filter with the name already exists"
                        .formatted(newFilter.getName()));
                throw new DuplicateNameException(newFilter.getName());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot access filter provider config", ex);
        }

        try {
            filterConfigValidator.validateFilterConfig(newFilter);
            newFilter.setName(newFilter.getName());
            securityManager.saveFilter(newFilter);
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot save filter provider config" + newFilter.getName(), ex);
        }

        try {
            return securityManager.loadFilterConfig(newFilter.getName(), false);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Cannot access filter provider  " + newFilter.getName() + " it has been saved", ex);
        }
    }

    protected void updateAuthFilter(String filterName, SecurityFilterConfig authfilterRequest) {
        if (!filterName.equals(authfilterRequest.getName())) {
            LOGGER.warning("Cannot modify the config %s because the name %s in the body does not match"
                    .formatted(filterName, authfilterRequest.getName()));
            throw new NameMismatchException(filterName, authfilterRequest.getName());
        }

        try {
            SecurityFilterConfig filter = securityManager.loadFilterConfig(filterName, false);
            if (filter == null) {
                LOGGER.warning("Cannot update %s because it does not exist".formatted(filterName));
                throw new IllegalArgumentException("Cannot update %s because it does not exist".formatted(filterName));
            }

            filterConfigValidator.validateFilterConfig(authfilterRequest);
            authfilterRequest.setId(filter.getId());
            authfilterRequest.setName(authfilterRequest.getName());
            securityManager.saveFilter(authfilterRequest);
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot access filter provider configs", ex);
        }
    }

    protected void removeAuthFilter(String filterName) {
        if (DELETE_BLACK_LIST.contains(filterName)) {
            LOGGER.warning("Cannot delete %s because it is a required authentication filter".formatted(filterName));
            throw new DeleteBlackListException(filterName);
        }

        try {
            SecurityFilterConfig filter = securityManager.loadFilterConfig(filterName, false);
            if (filter == null) {
                LOGGER.warning("Cannot delete %s because it does not exist".formatted(filterName));
                throw new IllegalArgumentException("Cannot find filter " + filterName);
            }
            securityManager.removeFilter(filter);
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot access filter provider config " + filterName, ex);
        }
    }

    /// ///////////////////////////////////////////////////////////////////////
    /// Exception handlers

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> somethingNotFound(IllegalArgumentException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DeleteBlackListException.class)
    public ResponseEntity<ErrorResponse> blackListed(DeleteBlackListException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NameMismatchException.class)
    public ResponseEntity<ErrorResponse> nameMismatched(NameMismatchException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingNameException.class)
    public ResponseEntity<ErrorResponse> missingName(MissingNameException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IdNotSet.class)
    public ResponseEntity<ErrorResponse> idNotSet(IdNotSet exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityConfigException.class)
    public ResponseEntity<ErrorResponse> securityIssue(SecurityConfigException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> ioIssue(IOException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FilterConfigException.class)
    public ResponseEntity<ErrorResponse> validationError(FilterConfigException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ErrorResponse> duplicatedName(DuplicateNameException exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotAuthorised.class)
    public ResponseEntity<ErrorResponse> notAuthorised(NotAuthorised exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Inner class to model the error response
    public static class ErrorResponse {
        private int status;
        private String message;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }

        // Getters and setters for JSON serialization
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

    /// ///////////////////////////////////////////////////////////////////////
    /// Bespoke Exceptions

    public static class DeleteBlackListException extends RuntimeException {
        public DeleteBlackListException(String name) {
            super("Cannot delete %s because it is a required authentication filter".formatted(name));
        }
    }

    public static class NameMismatchException extends RuntimeException {
        public NameMismatchException(String pathName, String configName) {
            super("Cannot modify the config %s because the name %s in the body does not match"
                    .formatted(pathName, configName));
        }
    }

    public static class DuplicateNameException extends RuntimeException {
        public DuplicateNameException(String configName) {
            super("Cannot create the config %s because the name is already in use".formatted(configName));
        }
    }

    public static class MissingNameException extends RuntimeException {
        public MissingNameException() {
            super("Cannot create the config has no name parameter");
        }
    }

    public static class IdNotSet extends RuntimeException {
        public IdNotSet(String requestBodyId, String id) {
            super("Cannot modify the config with %s because the id %s in the body does not match"
                    .formatted(requestBodyId, id));
        }
    }

    public static class NotAuthorised extends RuntimeException {
        public NotAuthorised() {
            super("Admin role required to access this resource");
        }
    }
}

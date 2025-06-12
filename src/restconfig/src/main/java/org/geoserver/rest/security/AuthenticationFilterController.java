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
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.AuthFilter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.geoserver.security.validation.SecurityConfigException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController("authenticationFilterChainController")
@ControllerAdvice(assignableTypes = {AuthenticationFilterController.class})
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/authFilters")
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

    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthFilter> list() {
        checkAuthorisation();
        List<AuthFilter> result = loadAuthFilters();
        return wrapList(result, AuthFilter.class);
    }

    // 200
    // 404
    @GetMapping(
            value = "/{filterName}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthFilter> view(@PathVariable("filterName") String filterName) {
        checkAuthorisation();
        AuthFilter authFilter = loadAuthFilter(filterName);
        return wrapObject(authFilter, AuthFilter.class);
    }

    // FilterConfigValidator to check if filter is valid
    // 201
    // 400
    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> post(
            @RequestBody AuthFilter authFilterRequest, UriComponentsBuilder uriComponentsBuilder) {
        checkAuthorisation();
        AuthFilter authFilterResponse = saveAuthFilter(authFilterRequest);

        UriComponents uriComponents = getUriComponents(authFilterResponse.getName(), uriComponentsBuilder);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(authFilterResponse.getName(), headers, HttpStatus.CREATED);
    }

    // 200
    // 400
    // 404
    @PutMapping(
            value = "/{filterName}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public @ResponseStatus(code = HttpStatus.OK) void put(
            @PathVariable("filterName") String filterName, @RequestBody AuthFilter authFilterRequest) {
        checkAuthorisation();
        updateAuthFilter(filterName, authFilterRequest);
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

    private UriComponents getUriComponents(String name, UriComponentsBuilder builder) {
        return builder.path(RestBaseController.ROOT_PATH + "/security/authFilters/{authFilter}")
                .buildAndExpand(name);
    }

    protected List<AuthFilter> loadAuthFilters() {
        try {
            Set<String> authFilterNames = securityManager.listFilters();
            List<AuthFilter> authFilters = new ArrayList<>();
            for (String filterName : authFilterNames) {
                SecurityFilterConfig securityFilterConfig = securityManager.loadFilterConfig(filterName, true);
                if (securityFilterConfig != null) {
                    authFilters.add(new AuthFilter(securityFilterConfig));
                }
            }
            return authFilters;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load provider filters", ex);
        }
    }

    protected AuthFilter loadAuthFilter(String filterName) {
        try {
            SecurityFilterConfig authFilter = securityManager.loadFilterConfig(filterName, true);
            if (authFilter == null) {
                LOGGER.warning(format("Cannot find %s because it does not exist", filterName));
                throw new IllegalArgumentException(format("Cannot find %s because it does not exist", filterName));
            }
            return new AuthFilter(authFilter);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load provider filter", ex);
        }
    }

    protected AuthFilter saveAuthFilter(AuthFilter newFilter) {
        if (newFilter.getId() != null) {
            LOGGER.warning(format(
                    "Cannot create the filter %s because client has provided an id %s",
                    newFilter.getName(), newFilter.getId()));
            throw new IdSetByServerException(newFilter);
        }

        if (Strings.isNullOrEmpty(newFilter.getName())) {
            LOGGER.warning(
                    format("Cannot create the filter %s because client has not provided a name", newFilter.getName()));
            throw new MissingNameException();
        }

        try {
            if (securityManager.loadFilterConfig(newFilter.getName(), true) != null) {
                LOGGER.warning(format(
                        "Cannot create the filter %s because a filter with the name already exists",
                        newFilter.getName()));
                throw new DuplicateNameException(newFilter.getName());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot access filter provider config", ex);
        }

        try {
            filterConfigValidator.validateFilterConfig(newFilter.getConfig());
            newFilter.getConfig().setName(newFilter.getName());

            securityManager.saveFilter(newFilter.getConfig());
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot save filter provider config" + newFilter.getName(), ex);
        }

        try {
            SecurityFilterConfig authFilter = securityManager.loadFilterConfig(newFilter.getName(), true);
            return new AuthFilter(authFilter);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Cannot access filter provider  " + newFilter.getName() + " it has been saved", ex);
        }
    }

    protected void updateAuthFilter(String filterName, AuthFilter authFilterRequest) {
        if (!filterName.equals(authFilterRequest.getName())) {
            LOGGER.warning(format(
                    "Cannot modify the config %s because the name %s in the body does not match",
                    filterName, authFilterRequest.getName()));
            throw new NameMismatchException(filterName, authFilterRequest.getName());
        }

        try {
            SecurityFilterConfig filter = securityManager.loadFilterConfig(filterName, true);
            if (filter == null) {
                LOGGER.warning(format("Cannot update %s because it does not exist", filterName));
                throw new IllegalArgumentException(format("Cannot update %s because it does not exist", filterName));
            }
            if (authFilterRequest.getId() == null) {
                LOGGER.warning(format(
                        "Cannot modify the config with %s because the id %s is not set",
                        authFilterRequest.getId(), filter.getId()));
                throw new IdNotSet(authFilterRequest.getId(), filter.getId());
            }

            filterConfigValidator.validateFilterConfig(authFilterRequest.getConfig());
            authFilterRequest.getConfig().setName(authFilterRequest.getName());
            authFilterRequest.getConfig().setId(authFilterRequest.getId());

            securityManager.saveFilter(authFilterRequest.getConfig());
            securityManager.reload();
        } catch (IOException | SecurityConfigException ex) {
            throw new IllegalStateException("Cannot access filter provider configs", ex);
        }
    }

    protected void removeAuthFilter(String filterName) {
        if (DELETE_BLACK_LIST.contains(filterName)) {
            LOGGER.warning(format("Cannot delete %s because it is a required authentication filter", filterName));
            throw new DeleteBlackListException(filterName);
        }

        try {
            SecurityFilterConfig filter = securityManager.loadFilterConfig(filterName, true);
            if (filter == null) {
                LOGGER.warning(format("Cannot delete %s because it does not exist", filterName));
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
    public void somethingNotFound(IllegalArgumentException exception, HttpServletResponse response) throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @ExceptionHandler(DeleteBlackListException.class)
    public void cannotDelete(DeleteBlackListException exception, HttpServletResponse response) throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @ExceptionHandler(IdSetByServerException.class)
    public void idSetByServerException(IdSetByServerException exception, HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(NameMismatchException.class)
    public void nameMismatch(NameMismatchException exception, HttpServletResponse response) throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(MissingNameException.class)
    public void missingName(MissingNameException exception, HttpServletResponse response) throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(IdNotSet.class)
    public void idMismatch(IdNotSet exception, HttpServletResponse response) throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(SecurityConfigException.class)
    public void securityConfigException(SecurityConfigException exception, HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public void readWriteFailure(IOException exception, HttpServletResponse response) throws IOException {
        response.sendError(500, exception.getMessage());
    }

    @ExceptionHandler(FilterConfigException.class)
    public void validationError(FilterConfigException exception, HttpServletResponse response) throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(DuplicateNameException.class)
    public void duplicateNameException(FilterConfigException exception, HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(NotAuthorised.class)
    public void duplicateNameException(NotAuthorised exception, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.FORBIDDEN.value(), exception.getMessage());
    }

    /// ///////////////////////////////////////////////////////////////////////
    /// Bespoke Exceptions

    public static class DeleteBlackListException extends RuntimeException {
        public DeleteBlackListException(String name) {
            super(format("Cannot delete %s because it is a required authentication filter", name));
        }
    }

    public static class IdSetByServerException extends RuntimeException {
        public IdSetByServerException(AuthFilter newConfig) {
            super(format(
                    "Cannot create the filter %s because client has provided an id %s",
                    newConfig.getName(), newConfig.getId()));
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
            super("Cannot create the config has no name parameter");
        }
    }

    public static class IdNotSet extends RuntimeException {
        public IdNotSet(String requestBodyId, String id) {
            super(format(
                    "Cannot modify the config with %s because the id %s in the body does not match",
                    requestBodyId, id));
        }
    }

    public static class NotAuthorised extends RuntimeException {
        public NotAuthorised() {
            super("Admin role required to access this resource");
        }
    }
}

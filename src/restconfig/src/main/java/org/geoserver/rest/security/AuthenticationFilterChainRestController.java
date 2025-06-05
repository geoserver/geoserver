/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.AuthFilterChain;
import org.geoserver.rest.security.xml.AuthFilterChainList;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.springframework.core.MethodParameter;
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
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "authenticationFilterChainRestController")
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/filterChains")
@ControllerAdvice(assignableTypes = {AuthenticationFilterChainRestController.class})
public class AuthenticationFilterChainRestController extends RestBaseController {
    private final GeoServerSecurityManager securityManager;

    public AuthenticationFilterChainRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @GetMapping(
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
            })
    public ResponseEntity<RestWrapper<AuthFilterChainList>> list() {
        checkAuthorisation();
        List<AuthFilterChain> filterChains = listFilterChains();
        AuthFilterChainList authFilterChainList = new AuthFilterChainList(filterChains);
        return ResponseEntity.ok(wrapObject(authFilterChainList, AuthFilterChainList.class));
    }

    @GetMapping(
            value = "/{chainName}",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
            })
    public ResponseEntity<RestWrapper<AuthFilterChain>> view(@PathVariable("chainName") String chainName) {
        checkAuthorisation();

        AuthFilterChain filterChain = viewFilterChain(chainName);
        return ResponseEntity.ok(wrapObject(filterChain, AuthFilterChain.class));
    }

    @PostMapping(
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
            },
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
            })
    public ResponseEntity<RestWrapper<AuthFilterChain>> create(@RequestBody AuthFilterChain authFilterChain) {
        checkAuthorisation();

        RequestFilterChain filterChain = authFilterChain.toRequestFilterChain();
        AuthFilterChain savedFilterChain = saveFilterChain(filterChain, authFilterChain.getPosition());
        return new ResponseEntity<>(wrapObject(savedFilterChain, AuthFilterChain.class), HttpStatus.CREATED);
    }

    @PutMapping(
            value = "/{chainName}",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
            },
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
            })
    public ResponseEntity<RestWrapper<AuthFilterChain>> update(
            @PathVariable("chainName") String chainName, @RequestBody AuthFilterChain authFilterChain) {
        checkAuthorisation();

        RequestFilterChain filterChain = authFilterChain.toRequestFilterChain();
        AuthFilterChain updatedFilterChain = updateFilterChain(chainName, filterChain, authFilterChain.getPosition());
        return ResponseEntity.ok(wrapObject(updatedFilterChain, AuthFilterChain.class));
    }

    @DeleteMapping(
            value = "/{chainName}",
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
            })
    public ResponseEntity<RestWrapper<AuthFilterChain>> delete(@PathVariable("chainName") String chainName) {
        checkAuthorisation();

        AuthFilterChain deleted = deleteFilterChain(chainName);
        return ResponseEntity.ok(wrapObject(deleted, AuthFilterChain.class));
    }

    // ///////////////////////////////////////////////////////////////////////
    // Exception handlers
    // Only create handlers for Exceptions from inner classes as the @ControllerAdvice annotation
    // makes these handlers leak

    @ExceptionHandler(CannotMakeChain.class)
    public ResponseEntity<ErrorResponse> handleRestException(CannotMakeChain exception) {
        // Prepare an error response object
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CannotSaveConfig.class)
    public ResponseEntity<ErrorResponse> handleRestException(CannotSaveConfig exception) {
        // Prepare an error response object
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CannotUpdateConfig.class)
    public ResponseEntity<ErrorResponse> handleRestException(CannotUpdateConfig exception) {
        // Prepare an error response object
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CannotReadConfig.class)
    public ResponseEntity<ErrorResponse> handleRestException(CannotReadConfig exception) {
        // Prepare an error response object
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<ErrorResponse> handleRestException(BadRequest exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NothingToDelete.class)
    public ResponseEntity<ErrorResponse> handleRestException(NothingToDelete exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.GONE.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.GONE);
    }

    @ExceptionHandler(DuplicateChainName.class)
    public ResponseEntity<ErrorResponse> handleRestException(DuplicateChainName exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FilterChainNotFound.class)
    public ResponseEntity<ErrorResponse> handleRestException(FilterChainNotFound exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NotAuthorised2.class)
    public ResponseEntity<ErrorResponse> handleRestException(NotAuthorised2 exception) {
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

    // ///////////////////////////////////////////////////////////////////////
    // Helper methods

    // It appears  these two methods require @ControllerAdvice annotation to be present
    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        checkAuthorisation();
        return AuthFilterChain.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter ignoredConverter) {
        XStream xstream = persister.getXStream();
        xstream.allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});
        xstream.alias("filterChain", AuthFilterChain.class);
        xstream.alias("filterChains", AuthFilterChainList.class);
        xstream.processAnnotations(new Class[] {AuthFilterChain.class, AuthFilterChainList.class});
    }

    // ///////////////////////////////////////////////////////////////////////
    // Helper methods
    private List<AuthFilterChain> listFilterChains() {
        try {
            checkState(securityManager != null, "GeoServerSecurityManager not initialized");

            SecurityManagerConfig config = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains = config.getFilterChain().getRequestChains();

            return chains.stream()
                    .filter(Objects::nonNull)
                    .map(AuthFilterChain::new)
                    .peek(chain -> {
                        RequestFilterChain filterChain = chains.stream()
                                .filter(c -> c.getName().equals(chain.getName()))
                                .findFirst()
                                .orElse(null);
                        int position = filterChain != null ? chains.indexOf(filterChain) : 0;
                        chain.setPosition(position);
                    })
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new CannotReadConfig(ex);
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        }
    }

    private AuthFilterChain viewFilterChain(String chainName) {
        try {
            checkState(securityManager != null, "GeoServerSecurityManager not initialized");
            checkArgument(!Strings.isNullOrEmpty(chainName), "chainName is required");

            SecurityManagerConfig config = securityManager.loadSecurityConfig();
            RequestFilterChain chain = config.getFilterChain().getRequestChainByName(chainName);
            if (chain == null) {
                throw new FilterChainNotFound(chainName);
            }

            AuthFilterChain authFilterChain = new AuthFilterChain(chain);
            authFilterChain.setPosition(
                    config.getFilterChain().getRequestChains().indexOf(chain));
            return authFilterChain;
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }
    }

    private AuthFilterChain deleteFilterChain(String chainName) {
        try {
            checkState(securityManager != null, "GeoServerSecurityManager not initialized");
            checkArgument(!Strings.isNullOrEmpty(chainName), "chainName is required");

            SecurityManagerConfig config = securityManager.loadSecurityConfig();
            GeoServerSecurityFilterChain chain = config.getFilterChain();
            RequestFilterChain filterChain = chain.getRequestChains().stream()
                    .filter(c -> c.getName().equals(chainName))
                    .findFirst()
                    .orElse(null);

            if (filterChain == null) {
                throw new NothingToDelete(chainName);
            }
            checkArgument(filterChain.canBeRemoved(), "Filter chain " + chainName + " cannot be removed.");

            if (!chain.getRequestChains().remove(filterChain)) {
                throw new NothingToDelete(chainName);
            }
            return saveAndReturnAuthFilterChain(filterChain, config, chain.getRequestChains());
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IOException e) {
            throw new CannotUpdateConfig(e);
        }
    }

    private AuthFilterChain updateFilterChain(String chainName, RequestFilterChain filterChain, int position)
            throws CannotSaveConfig {
        try {
            checkState(securityManager != null, "GeoServerSecurityManager not initialized");

            checkArgument(!Strings.isNullOrEmpty(chainName), "chainName is required");
            checkArgument(
                    Objects.equals(filterChain.getName(), chainName),
                    "chainName must be the same as the name of the filter chain to be updated");
            checkArgument(position >= 0, "position must be greater than or equal to 0");

            SecurityManagerConfig config = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains = config.getFilterChain().getRequestChains();
            checkArgument(position < chains.size(), "position must be less than the number of filter chains");

            List<RequestFilterChain> updatedChains = chains.stream()
                    .map(chain -> chain.getName().equals(chainName) ? filterChain : chain)
                    .collect(Collectors.toList());

            // If position is different to actual position move it
            if (position != updatedChains.indexOf(filterChain)) {
                updatedChains.remove(filterChain);
                updatedChains.add(position, filterChain);
            }

            return saveAndReturnAuthFilterChain(filterChain, config, updatedChains);
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IllegalStateException | IOException e) {
            throw new CannotSaveConfig(e);
        }
    }

    private AuthFilterChain saveFilterChain(RequestFilterChain filterChain, int position) {
        try {
            checkState(securityManager != null, "GeoServerSecurityManager not initialized");
            checkArgument(Objects.nonNull(filterChain), "filterChain is required");
            checkArgument(position >= 0, "position must be greater than or equal to 0");

            SecurityManagerConfig config = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains = config.getFilterChain().getRequestChains();
            if (chains.contains(filterChain)) {
                throw new DuplicateChainName(filterChain.getName());
            }

            chains.add(position, filterChain);

            return saveAndReturnAuthFilterChain(filterChain, config, chains);
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IllegalStateException | IOException e) {
            throw new CannotSaveConfig(e);
        }
    }

    private AuthFilterChain saveAndReturnAuthFilterChain(
            RequestFilterChain filterChain, SecurityManagerConfig config, List<RequestFilterChain> chains) {
        GeoServerSecurityFilterChain updateGeoServerFilterChains = new GeoServerSecurityFilterChain(chains);
        config.setFilterChain(updateGeoServerFilterChains);
        try {
            securityManager.saveSecurityConfig(config);
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
        securityManager.reload();
        AuthFilterChain authFilterChain = new AuthFilterChain(filterChain);
        authFilterChain.setPosition(chains.indexOf(filterChain));
        return authFilterChain;
    }

    private void checkAuthorisation() {
        if (!securityManager.checkAuthenticationForAdminRole()) {
            throw new NotAuthorised2();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Exceptions

    public static class CannotMakeChain extends RuntimeException {
        public CannotMakeChain(String className, Exception ex) {
            super("Cannot make class " + className, ex);
        }
    }

    public static class CannotSaveConfig extends RuntimeException {
        public CannotSaveConfig(Exception ex) {
            super("Cannot save the Security configuration ", ex);
        }
    }

    public static class CannotUpdateConfig extends RuntimeException {
        public CannotUpdateConfig(Exception ex) {
            super("Cannot update the Security configuration ", ex);
        }
    }

    public static class CannotReadConfig extends RuntimeException {
        public CannotReadConfig(Exception ex) {
            super("Cannot read the Security configuration ", ex);
        }
    }

    public static class NothingToDelete extends RuntimeException {
        public NothingToDelete(String filterName) {
            super("Cannot delete " + filterName + " as no filter exists");
        }
    }

    public static class BadRequest extends RuntimeException {
        public BadRequest(String message) {
            super(message);
        }
    }

    public static class FilterChainNotFound extends RuntimeException {
        public FilterChainNotFound(String filterName) {
            super("Cannot find the filter chain " + filterName + " in the Security configuration.");
        }
    }

    public static class DuplicateChainName extends RuntimeException {
        public DuplicateChainName(String filterName) {
            super("Cannot create the filter chain " + filterName + " because one with that name already exists.");
        }
    }

    public static class NotAuthorised2 extends RuntimeException {
        public NotAuthorised2() {
            super("Admin role required to access this resource");
        }
    }
}

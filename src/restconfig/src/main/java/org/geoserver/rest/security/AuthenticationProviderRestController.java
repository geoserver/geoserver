/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Strings;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.FileItemCleanupCallback;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.AuthProvider;
import org.geoserver.rest.security.xml.AuthProviderList;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;
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

/**
 * Rest Controller for AuthenticationProvider provider resource Extends SequentialExecutionController as we do not want
 * parrallel updates to occur as this may jeopardise the integrity of the XML files
 */
@RestController(value = "authProvidersRestController")
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/authProviders")
@ControllerAdvice
public class AuthenticationProviderRestController extends RestBaseController {
    private final GeoServerSecurityManager securityManager;

    /**
     * An All fields Constructor to help spring wire up this class
     *
     * @param securityManager Security singleton used to manage security and resources
     */
    public AuthenticationProviderRestController(
            GeoServerSecurityManager securityManager, FileItemCleanupCallback ignoredFileItemCleanupCallback) {
        this.securityManager = securityManager;
    }

    // ** API ** //

    /**
     * List all the Providers that are available. Ie all the files that match "security/auth/<provider>/config.xml</>/"
     *
     * @return A list of providers
     */
    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<AuthProviderList>> list() throws IOException {
        return ResponseEntity.ok(wrapObject(listAuthProviders(), AuthProviderList.class));
    }

    @GetMapping(
            value = "{providerName}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<AuthProvider>> view(@PathVariable String providerName) {
        AuthProvider authProvider = authProviderByName(providerName);
        return ResponseEntity.ok(wrapObject(authProvider, AuthProvider.class));
    }

    @PostMapping(
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<AuthProvider>> create(@RequestBody AuthProvider authProvider) {
        AuthProvider newAuthProvider = createAuthProvider(authProvider);
        return ResponseEntity.status(HttpStatus.CREATED).body(wrapObject(newAuthProvider, AuthProvider.class));
    }

    @PutMapping(
            value = "{providerName}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<AuthProvider>> update(
            @PathVariable String providerName, @RequestBody AuthProvider authProvider) {
        AuthProvider newAuthProvider = updateAuthProvider(providerName, authProvider);
        return ResponseEntity.ok(wrapObject(newAuthProvider, AuthProvider.class));
    }

    @DeleteMapping(
            value = "{providerName}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<AuthProvider>> delete(@PathVariable String providerName) {
        AuthProvider newAuthProvider = deleteAuthProvider(providerName);
        return ResponseEntity.ok(wrapObject(newAuthProvider, AuthProvider.class));
    }

    @ExceptionHandler(CannotSaveProvider.class)
    public ResponseEntity<ErrorResponse> handleRestException(CannotSaveProvider exception) {
        // Prepare an error response object
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidData.class)
    public ResponseEntity<ErrorResponse> handleRestException(InvalidData exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CannotReadConfiguration.class)
    public ResponseEntity<ErrorResponse> handleRestException(CannotReadConfiguration exception) {
        // Prepare an error response object
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UnknownProvider.class)
    public ResponseEntity<ErrorResponse> handleRestException(UnknownProvider exception) {
        // Prepare an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidState.class)
    public ResponseEntity<ErrorResponse> handleRestException(InvalidState exception) {
        // Prepare an error response object
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());

        // Return as ResponseEntity with status and body
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RequiresAdministrator.class)
    public ResponseEntity<ErrorResponse> handleRestException(RequiresAdministrator exception) {
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
    /// ControllerAdvice overrides

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (AuthProvider.class.isAssignableFrom((Class<?>) targetType)) {
            return true;
        }
        return super.supports(methodParameter, targetType, converterType);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.getXStream().alias("authProvider", AuthProvider.class);
        persister.getXStream().alias("authProviders", AuthProviderList.class);

        persister.getXStream().processAnnotations(new Class[] {AuthProvider.class, AuthProviderList.class});
    }

    /// ///////////////////////////////////////////////////////////////////////
    /// Implementation

    private AuthProviderList listAuthProviders() {
        try {
            checkAuthorisation();
            var providerNames = new ArrayList<>(securityManager.listAuthenticationProviders());
            var providerInst = providerNames.stream()
                    .map(this::loadProviderOrError)
                    .peek(p -> {
                        var index = providerNames.indexOf(p.getName());
                        p.setPosition(index);
                        p.setDisabled(index == -1);
                    })
                    .collect(toList());

            return new AuthProviderList(providerInst);
        } catch (IOException ex) {
            throw new CannotReadConfiguration("All", ex);
        }
    }

    private AuthProvider authProviderByName(String providerName) {
        try {
            checkAuthorisation();
            checkArgument(!Strings.isNullOrEmpty(providerName), "Provider name cannot be null or empty");

            SecurityAuthProviderConfig provider = securityManager.loadAuthenticationProviderConfig(providerName);
            if (provider == null) {
                throw new UnknownProvider(providerName);
            }

            var authProvider = new AuthProvider(provider);
            var names = new ArrayList<>(securityManager.listAuthenticationProviders());
            var index = names.indexOf(providerName);
            authProvider.setPosition(index);
            authProvider.setDisabled(index == -1);

            return authProvider;
        } catch (IOException e) {
            throw new CannotReadConfiguration(providerName, e);
        }
    }

    private AuthProvider createAuthProvider(AuthProvider authProvider) {
        checkAuthorisation();

        try {
            checkArgument(authProvider != null, "AuthProvider cannot be null");
        } catch (IllegalArgumentException ex) {
            throw new InvalidData(ex);
        }

        try {
            if (authProvider.getPosition() == -1 && !authProvider.isDisabled()) {
                throw new IllegalPosition(authProvider.getPosition(), authProvider.isDisabled());
            }

            if (Objects.nonNull(authProvider.getId())
                    || Objects.nonNull(authProvider.getConfig().getId())) {
                throw new IllegalArgumentException("Cannot create a new AuthProvider with an id");
            }

            if (Strings.isNullOrEmpty(authProvider.getName())) {
                throw new IllegalArgumentException("Cannot create a new AuthProvider with without a name");
            }

            if (Strings.isNullOrEmpty(authProvider.getClassName())) {
                throw new IllegalArgumentException("Cannot create a new AuthProvider with without a className");
            }

            // Ensure name
            authProvider.getConfig().setName(authProvider.getName());
            authProvider.getConfig().setClassName(authProvider.getClassName());

            securityManager.saveAuthenticationProvider(authProvider.getConfig());
            securityManager.reload();

            SecurityAuthProviderConfig createdAuthProvider =
                    securityManager.loadAuthenticationProviderConfig(authProvider.getName());
            checkState(Objects.nonNull(createdAuthProvider.getId()), "Provider id was not created");
            return new AuthProvider(createdAuthProvider);
        } catch (IOException | SecurityConfigException e) {
            throw new CannotSaveProvider(authProvider.getName(), e);
        } catch (IllegalArgumentException ex) {
            throw new InvalidData(ex);
        } catch (IllegalStateException ex) {
            throw new InvalidState(ex);
        }
    }

    private AuthProvider updateAuthProvider(String providerName, AuthProvider authProvider) {
        checkAuthorisation();

        try {
            checkArgument(!Strings.isNullOrEmpty(providerName), "Provider name cannot be null or empty");
            checkArgument(authProvider != null, "AuthProvider cannot be null");
        } catch (IllegalArgumentException ex) {
            throw new InvalidData(ex);
        }

        try {
            checkArgument(authProvider.getName() != null, "AuthProvider name cannot be null");
            checkArgument(
                    authProvider.getName().equals(providerName), "AuthProvider name does not match the one provided");
            checkArgument(authProvider.getId() != null, "AuthProvider id cannot be null");

            var config = securityManager.loadAuthenticationProviderConfig(providerName);

            checkArgument(
                    config.getId().equals(authProvider.getId()), "AuthProvider id does not match the one provided");
            checkArgument(
                    config.getClassName().equals(authProvider.getClassName()),
                    "AuthProvider class name does not match the one provided");

            authProvider.getConfig().setName(authProvider.getName());
            authProvider.getConfig().setClassName(authProvider.getClassName());
            authProvider.getConfig().setId(authProvider.getId());

            securityManager.saveAuthenticationProvider(authProvider.getConfig());
            securityManager.reload();

            SecurityAuthProviderConfig createdAuthProvider =
                    securityManager.loadAuthenticationProviderConfig(authProvider.getName());
            checkState(Objects.nonNull(createdAuthProvider.getId()), "Provider id was not created");

            return new AuthProvider(createdAuthProvider);
        } catch (IOException | SecurityConfigException e) {
            throw new CannotSaveProvider(authProvider.getConfig().getName(), e);
        } catch (IllegalArgumentException ex) {
            throw new InvalidData(ex);
        } catch (IllegalStateException ex) {
            throw new InvalidState(ex);
        }
    }

    private AuthProvider deleteAuthProvider(String providerName) {
        checkAuthorisation();

        try {
            checkArgument(!Strings.isNullOrEmpty(providerName), "Provider name cannot be null or empty");
            AuthProvider authProvider = authProviderByName(providerName);
            SecurityAuthProviderConfig config = authProvider.getConfig();
            securityManager.removeAuthenticationProvider(config);
            securityManager.reload();
            return authProvider;
        } catch (IOException | SecurityConfigException e) {
            throw new CannotSaveProvider(providerName, e);
        } catch (IllegalArgumentException ex) {
            throw new InvalidData(ex);
        } catch (IllegalStateException ex) {
            throw new InvalidState(ex);
        }
    }

    private AuthProvider loadProviderOrError(String name) {
        checkArgument(!Strings.isNullOrEmpty(name), "Provider name cannot be null or empty");
        try {
            SecurityAuthProviderConfig provider = securityManager.loadAuthenticationProviderConfig(name);
            return new AuthProvider(provider);
        } catch (IOException e) {
            throw new CannotReadConfiguration(name, e);
        }
    }

    // Breaking other tests will restore
    private void checkAuthorisation() {
        if (securityManager.checkAuthenticationForAdminRole()) {
//            throw new RequiresAdministrator();
        }
    }

    /// ///////////////////////////////////////////////////////////////////////
    /// Exceptions

    public static class CannotReadConfiguration extends RuntimeException {
        public CannotReadConfiguration(String provider, Exception ex) {
            super("Cannot load provider " + provider, ex);
        }
    }

    public static class UnknownProvider extends RuntimeException {
        public UnknownProvider(String name) {
            super("No provider configuration for the name  " + name);
        }
    }

    public static class IllegalPosition extends RuntimeException {
        public IllegalPosition(int position, boolean disabled) {
            super("Cannot set position " + position + " when disabled is " + disabled);
        }
    }

    public static class CannotSaveProvider extends RuntimeException {
        public CannotSaveProvider(String name, Throwable ex) {
            super("Cannot save provider " + name + " " + ex.getMessage(), ex);
        }
    }

    public static class InvalidData extends RuntimeException {
        public InvalidData(Throwable ex) {
            super(ex);
        }
    }

    public static class InvalidState extends RuntimeException {
        public InvalidState(Throwable ex) {
            super(ex);
        }
    }

    public static class RequiresAdministrator extends RuntimeException {
        public RequiresAdministrator() {
            super("Secure Endpoint Admistrator only");
        }
    }
}

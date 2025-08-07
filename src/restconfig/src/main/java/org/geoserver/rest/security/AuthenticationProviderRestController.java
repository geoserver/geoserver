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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.FileItemCleanupCallback;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.AuthProvider;
import org.geoserver.rest.security.xml.AuthProviderCollection;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.*;

/**
 * REST controller for the <em>Authentication Providers</em> resource.
 *
 * <p>Extends SequentialExecutionController as we do not want
 * parallel updates to occur as this may jeopardize the integrity of the XML files
 */
@RestController
@RequestMapping(RestBaseController.ROOT_PATH + "/security/authProviders")
@ControllerAdvice(assignableTypes = {AuthenticationProviderRestController.class})
public class AuthenticationProviderRestController extends RestBaseController {

    /* ------------------------------------------------------------------ *
     *  Constants & helpers
     * ------------------------------------------------------------------ */

    /** Path segments that can never be used as provider names (mirrors the chain endpoint). */
    private static final Set<String> RESERVED_NAMES = Set.of("order");

    private final GeoServerSecurityManager securityManager;

    public AuthenticationProviderRestController(
            GeoServerSecurityManager securityManager, FileItemCleanupCallback ignoredFileItemCleanupCallback) {
        this.securityManager = securityManager;
    }

    /** Small guard so we don’t expose a reserved keyword as a valid name. */
    private static void ensureNotReserved(String name) {
        if (RESERVED_NAMES.contains(name)) {
            throw new DuplicateProviderName("The name \"" + name + "\" is reserved and cannot be used.");
        }
    }

    /* ------------------------------------------------------------------ *
     *  API – list / get
     * ------------------------------------------------------------------ */

    /** GET /security/authProviders ➜ list of providers wrapped in <authProviders>. */
    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthProviderCollection> list() throws IOException {
        checkAuthorised();

        List<AuthProvider> providers = listAuthProviders();
        return wrapObject(new AuthProviderCollection(providers), AuthProviderCollection.class);
    }

    /** GET /security/authProviders/{providerName} (except “order”). */
    @GetMapping(
            value = "/{providerName:^(?!order$).+}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthProvider> view(@PathVariable String providerName) {
        checkAuthorised();
        AuthProvider authProvider = authProviderByName(providerName);
        return wrapObject(authProvider, AuthProvider.class);
    }

    /* ------------------------------------------------------------------ *
     *  API – create / update / delete
     * ------------------------------------------------------------------ */

    /** POST …/authProviders[?position=n] */
    @PostMapping(
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<AuthProvider>> create(
            @RequestBody AuthProvider authProvider,
            @RequestParam(name = "position", required = false) Integer position,
            UriComponentsBuilder builder) {

        checkAuthorised();
        ensureNotReserved(authProvider.getName());

        // honour ?position if given (preferred) – fallback to DTO field for legacy callers
        if (position != null) authProvider.setPosition(position);

        createAuthProvider(authProvider);

        // 201 Created + Location + wrapped body
        HttpHeaders headers = new HttpHeaders();
        UriComponents uri = builder.path(ROOT_PATH + "/security/authProviders/{providerName}")
                .buildAndExpand(authProvider.getName());
        headers.setLocation(uri.toUri());

        return new ResponseEntity<>(wrapObject(authProvider, AuthProvider.class), headers, HttpStatus.CREATED);
    }

    /** PUT …/authProviders/{providerName} */
    @PutMapping(
            value = "/{providerName:^(?!order$).+}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public void update(@PathVariable String providerName, @RequestBody AuthProvider authProvider) {
        checkAuthorised();
        updateAuthProvider(providerName, authProvider);
    }

    /** DELETE …/authProviders/{providerName} */
    @DeleteMapping(
            value = "/{providerName:^(?!order$).+}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable String providerName) {
        checkAuthorised();
        deleteAuthProvider(providerName);
    }

    /* ------------------------------------------------------------------ *
     *  Error handling
     * ------------------------------------------------------------------ */

    @ExceptionHandler({
        CannotSaveProvider.class,
        CannotReadConfiguration.class,
        DuplicateProviderName.class,
        ProviderNotFound.class,
        BadRequest.class,
        NotAuthorised.class
    })
    public ResponseEntity<ErrorResponse> handleRestException(RuntimeException ex) {
        HttpStatus status;
        if (ex instanceof BadRequest) status = HttpStatus.BAD_REQUEST;
        else if (ex instanceof NotAuthorised) status = HttpStatus.FORBIDDEN;
        else if (ex instanceof ProviderNotFound) status = HttpStatus.NOT_FOUND;
        else status = HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(new ErrorResponse(status.value(), ex.getMessage()), status);
    }

    /* ------------------------------------------------------------------ *
     *  Controller-advice overrides
     * ------------------------------------------------------------------ */

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (AuthProvider.class.isAssignableFrom((Class<?>) targetType)
                || AuthProviderCollection.class.isAssignableFrom((Class<?>) targetType)) {
            return true;
        }
        return super.supports(methodParameter, targetType, converterType);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.getXStream().alias("authProvider", AuthProvider.class);
        persister.getXStream().alias("authProviders", AuthProviderCollection.class);
        persister.getXStream().processAnnotations(new Class[] {AuthProvider.class, AuthProviderCollection.class});
    }

    /* ------------------------------------------------------------------ *
     *  Implementation helpers
     * ------------------------------------------------------------------ */

    private List<AuthProvider> listAuthProviders() {
        try {
            ArrayList<String> providerNames = new ArrayList<>(securityManager.listAuthenticationProviders());
            return providerNames.stream()
                    .map(this::loadProviderOrError)
                    .peek(p -> {
                        int index = providerNames.indexOf(p.getName());
                        p.setPosition(index);
                        p.setDisabled(index == -1);
                    })
                    .collect(toList());
        } catch (IOException ex) {
            throw new CannotReadConfiguration("all", ex);
        }
    }

    private AuthProvider authProviderByName(String providerName) {
        try {
            checkArgument(!Strings.isNullOrEmpty(providerName), "Provider name cannot be null or empty");
            ensureNotReserved(providerName);

            SecurityAuthProviderConfig provider = securityManager.loadAuthenticationProviderConfig(providerName);
            if (provider == null) throw new ProviderNotFound(providerName);

            AuthProvider authProvider = new AuthProvider(provider);
            ArrayList<String> names = new ArrayList<>(securityManager.listAuthenticationProviders());
            int index = names.indexOf(providerName);
            authProvider.setPosition(index);
            authProvider.setDisabled(index == -1);
            return authProvider;
        } catch (IOException e) {
            throw new CannotReadConfiguration(providerName, e);
        }
    }

    private void createAuthProvider(AuthProvider authProvider) {
        try {
            checkArgument(authProvider != null, "AuthProvider cannot be null");
            ensureNotReserved(authProvider.getName());

            if (authProvider.getPosition() == -1 && !authProvider.isDisabled()) {
                throw new IllegalPosition(authProvider.getPosition(), authProvider.isDisabled());
            }
            if (Objects.nonNull(authProvider.getId())
                    || Objects.nonNull(authProvider.getConfig().getId())) {
                throw new IllegalArgumentException("Cannot create a new AuthProvider with an id");
            }

            // Ensure name & class are mirrored on the config before save
            authProvider.getConfig().setName(authProvider.getName());
            authProvider.getConfig().setClassName(authProvider.getClassName());

            securityManager.saveAuthenticationProvider(authProvider.getConfig());
            securityManager.reload();

            SecurityAuthProviderConfig created =
                    securityManager.loadAuthenticationProviderConfig(authProvider.getName());
            checkState(created.getId() != null, "Provider id was not set by security manager");

        } catch (IOException | SecurityConfigException e) {
            throw new CannotSaveProvider(authProvider.getName(), e);
        } catch (IllegalArgumentException ex) {
            throw new BadRequest(ex);
        } catch (IllegalStateException ex) {
            throw new InvalidState(ex);
        }
    }

    private void updateAuthProvider(String providerName, AuthProvider authProvider) {
        try {
            checkArgument(!Strings.isNullOrEmpty(providerName), "Provider name cannot be null or empty");
            checkArgument(authProvider != null, "AuthProvider cannot be null");

            if (!providerName.equals(authProvider.getName())) throw new BadRequest("Path name and payload name differ");

            SecurityAuthProviderConfig config = securityManager.loadAuthenticationProviderConfig(providerName);
            if (config == null) throw new ProviderNotFound(providerName);

            // Immutable properties
            if (!Objects.equals(config.getId(), authProvider.getId()))
                throw new BadRequest("AuthProvider id cannot be modified");
            if (!Objects.equals(config.getClassName(), authProvider.getClassName()))
                throw new BadRequest("AuthProvider className cannot be modified");

            authProvider.getConfig().setName(authProvider.getName());
            authProvider.getConfig().setClassName(authProvider.getClassName());
            authProvider.getConfig().setId(authProvider.getId());

            securityManager.saveAuthenticationProvider(authProvider.getConfig());
            securityManager.reload();
        } catch (IOException | SecurityConfigException e) {
            throw new CannotSaveProvider(providerName, e);
        } catch (IllegalArgumentException ex) {
            throw new BadRequest(ex);
        }
    }

    private void deleteAuthProvider(String providerName) {
        try {
            checkArgument(!Strings.isNullOrEmpty(providerName), "Provider name cannot be null or empty");
            SecurityAuthProviderConfig cfg = securityManager.loadAuthenticationProviderConfig(providerName);
            if (cfg == null) throw new ProviderNotFound(providerName);

            securityManager.removeAuthenticationProvider(cfg);
            securityManager.reload();
        } catch (IOException | SecurityConfigException e) {
            throw new CannotSaveProvider(providerName, e);
        } catch (IllegalArgumentException ex) {
            throw new BadRequest(ex);
        }
    }

    private AuthProvider loadProviderOrError(String name) {
        checkArgument(!Strings.isNullOrEmpty(name), "Provider name cannot be null or empty");
        try {
            SecurityAuthProviderConfig provider = securityManager.loadAuthenticationProviderConfig(name);
            if (provider == null) throw new ProviderNotFound(name);
            return new AuthProvider(provider);
        } catch (IOException e) {
            throw new CannotReadConfiguration(name, e);
        }
    }

    /* ------------------------------------------------------------------ *
     *  Authorisation guard
     * ------------------------------------------------------------------ */

    private void checkAuthorised() {
        if (!securityManager.checkAuthenticationForAdminRole()) {
            throw new NotAuthorised();
        }
    }

    /* ------------------------------------------------------------------ *
     *  Exceptions – aligned with filter-chain endpoint
     * ------------------------------------------------------------------ */

    public static class CannotReadConfiguration extends RuntimeException {
        public CannotReadConfiguration(String name, Exception ex) {
            super("Cannot read configuration for provider \"" + name + "\"", ex);
        }
    }

    public static class ProviderNotFound extends RuntimeException {
        public ProviderNotFound(String name) {
            super("No provider named \"" + name + "\" exists");
        }
    }

    public static class IllegalPosition extends RuntimeException {
        public IllegalPosition(int position, boolean disabled) {
            super("Cannot set position " + position + " when disabled = " + disabled);
        }
    }

    public static class DuplicateProviderName extends RuntimeException {
        public DuplicateProviderName(String message) {
            super(message);
        }
    }

    public static class CannotSaveProvider extends RuntimeException {
        public CannotSaveProvider(String name, Throwable ex) {
            super("Cannot save provider \"" + name + "\": " + ex.getMessage(), ex);
        }
    }

    public static class BadRequest extends RuntimeException {
        public BadRequest(Throwable ex) {
            super(ex);
        }

        public BadRequest(String message) {
            super(message);
        }
    }

    public static class InvalidState extends RuntimeException {
        public InvalidState(Throwable ex) {
            super(ex);
        }
    }

    public static class NotAuthorised extends RuntimeException {
        public NotAuthorised() {
            super("Administrator role required");
        }
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
}

/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.AuthProviderCollection;
import org.geoserver.rest.security.xml.AuthProviderOrder;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for managing <em>Authentication Providers</em>.
 *
 * <h3>Resources</h3>
 *
 * <ul>
 *   <li><code>GET /security/authproviders</code> – list enabled providers (in effective order).
 *   <li><code>GET /security/authproviders/{name}</code> – fetch a single provider config.
 *   <li><code>POST /security/authproviders?position={pos}</code> – create a provider and insert it into the enabled
 *       order at the specified position (default: append).
 *   <li><code>PUT /security/authproviders/{name}?position={pos}</code> – update an existing provider and optionally
 *       move it to a new position.
 *   <li><code>DELETE /security/authproviders/{name}</code> – remove from order (disable) then delete the provider
 *       configuration from disk.
 *   <li><code>PUT /security/authproviders/order</code> – set the <em>entire</em> enabled order. The list provided
 *       becomes the canonical {@code <authproviderNames>} in the global security config. Names omitted here remain
 *       saved on disk but are considered <strong>disabled</strong>.
 * </ul>
 *
 * <h3>Formats</h3>
 *
 * All endpoints accept / return XML or JSON. For create/update you may send:
 *
 * <ul>
 *   <li>JSON: a single provider under <code>{"authprovider": {...}}</code>, or <code>{"authproviders":[{...}]}</code>
 *       with exactly one item.
 *   <li>XML: a single {@link SecurityAuthProviderConfig} element, or an <code>&lt;authproviders&gt;</code> wrapper with
 *       exactly one provider.
 * </ul>
 *
 * <h3>Type resolution</h3>
 *
 * JSON requires a <code>className</code>. The controller resolves a concrete config type by:
 *
 * <ol>
 *   <li>If <code>className</code> is itself a {@link SecurityAuthProviderConfig} subclass FQN, it is used.
 *   <li>Otherwise it is treated as the provider class FQN and we try conventional config classes: <code>
 *       *.auth.FooProvider → *.config.FooProviderConfig</code>, <code>org.geoserver.security.config.FooProviderConfig
 *       </code>, and <code>same.package.FooProviderConfig</code>.
 * </ol>
 *
 * <h3>Authorization</h3>
 *
 * All operations require an authenticated admin; otherwise HTTP 403 is returned.
 */
@RestController
@RequestMapping(RestBaseController.ROOT_PATH + "/security/authproviders")
public class AuthenticationProviderRestController extends RestBaseController {

    /**
     * Accept provider names with optional ".xml" / ".json" extension, but block "order(.ext)" which is reserved for the
     * reorder endpoint.
     */
    private static final String PROVIDER_PATH = "/{providerName:^(?!order(?:\\.(?:json|xml))?$).+}";

    /** Reserved logical names that cannot be used for provider ids. */
    private static final Set<String> RESERVED = Set.of("order");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GeoServerSecurityManager securityManager;

    public AuthenticationProviderRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    // ------------------------------------------------------------------------------------ XStream
    @Override
    public void configurePersister(XStreamPersister xp, XStreamMessageConverter conv) {
        super.configurePersister(xp, conv);
        XStream xs = xp.getXStream();

        xs.allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});

        xs.alias("authproviders", AuthProviderCollection.class);
        xs.addImplicitCollection(AuthProviderCollection.class, "providers", null, SecurityAuthProviderConfig.class);

        xs.alias("order", AuthProviderOrder.class);
        xs.addImplicitCollection(AuthProviderOrder.class, "order", "order", String.class);
    }

    // ------------------------------------------------------------------------ LIST + ITEM --------

    /** List the enabled providers in the current effective order (i.e. the global {@code <authproviderNames>}). */
    @GetMapping(
            path = {"", ".{ext:xml|json}"},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthProviderCollection> list() {
        checkAuthorised();
        try {
            List<SecurityAuthProviderConfig> cfgs =
                    getConfigOrder().stream().map(this::loadConfigOrError).collect(Collectors.toList());
            return wrapObject(new AuthProviderCollection(cfgs), AuthProviderCollection.class);
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }
    }

    /** Fetch a single provider configuration by name. */
    @GetMapping(
            path = PROVIDER_PATH,
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<SecurityAuthProviderConfig> one(@PathVariable String providerName) {
        providerName = normalizeName(providerName);
        checkAuthorised();
        return wrapObject(loadConfigOrError(providerName), SecurityAuthProviderConfig.class);
    }

    // ----------------------------------------------------------------------------- CREATE --------

    /**
     * Create a new provider and insert its name into the enabled order. If {@code position} is not provided, the
     * provider is appended (enabled at the end).
     */
    @PostMapping(
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<SecurityAuthProviderConfig>> create(
            HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position,
            UriComponentsBuilder builder) {

        checkAuthorised();

        // Parse first so BadRequest/parse errors are mapped correctly.
        final SecurityAuthProviderConfig cfg;
        try {
            cfg = parseConfig(request);
        } catch (BadRequest e) {
            throw e;
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }

        try {
            ensureNotReserved(cfg.getName());

            List<String> order = getConfigOrder();
            if (order.contains(cfg.getName())) {
                throw new DuplicateProviderName(cfg.getName());
            }

            int pos = (position != null) ? position : order.size();
            validatePosition(pos, order.size());

            securityManager.saveAuthenticationProvider(cfg);
            order.add(pos, cfg.getName());
            saveConfigOrder(order);
            securityManager.reload();

            HttpHeaders h = new HttpHeaders();
            h.setLocation(builder.path("/security/authproviders/{name}")
                    .buildAndExpand(cfg.getName())
                    .toUri());
            return new ResponseEntity<>(wrapObject(cfg, SecurityAuthProviderConfig.class), h, HttpStatus.CREATED);

        } catch (DuplicateProviderName e) {
            throw e; // <-- let it propagate as 400 via @ExceptionHandler
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    // ----------------------------------------------------------------------------- UPDATE --------

    /**
     * Update an existing provider and optionally move it within the enabled order.
     *
     * <p>The path name and payload name must match. The provider's {@code className} is immutable; if omitted, it is
     * preserved; if provided and different, the request is rejected.
     */
    @PutMapping(
            path = PROVIDER_PATH,
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<SecurityAuthProviderConfig> update(
            @PathVariable String providerName,
            HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position) {

        checkAuthorised();

        providerName = normalizeName(providerName);

        final SecurityAuthProviderConfig incoming;
        try {
            incoming = parseConfig(request);
        } catch (BadRequest e) {
            throw e;
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }

        ensureNotReserved(incoming.getName());
        if (!Objects.equals(providerName, incoming.getName())) {
            throw new BadRequest("path name and payload name differ");
        }

        // Load existing, preserve identity/class
        SecurityAuthProviderConfig existing = loadConfigOrError(providerName);
        if (incoming.getClassName() == null) {
            incoming.setClassName(existing.getClassName());
        } else if (!incoming.getClassName().equals(existing.getClassName())) {
            throw new BadRequest("className cannot change");
        }
        if (incoming.getId() == null) {
            incoming.setId(existing.getId());
        }

        try {
            List<String> order = getConfigOrder();
            int currentIdx = order.indexOf(providerName);
            if (currentIdx < 0) {
                throw new ProviderNotFound(providerName);
            }

            securityManager.saveAuthenticationProvider(incoming);

            if (position != null) {
                validatePosition(position, order.size());
                if (currentIdx != position) {
                    order.remove(currentIdx);
                    order.add(position, providerName);
                    saveConfigOrder(order);
                }
            }

            securityManager.reload();
            return wrapObject(incoming, SecurityAuthProviderConfig.class);
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    // ----------------------------------------------------------------------------- DELETE --------

    /**
     * Delete a provider: first remove its name from the enabled order (disabling it), then remove its configuration
     * file from disk, and reload security.
     */
    @DeleteMapping(path = PROVIDER_PATH)
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable String providerName) {
        checkAuthorised();
        providerName = normalizeName(providerName);

        try {
            SecurityManagerConfig smc = securityManager.loadSecurityConfig();
            List<String> order = smc.getAuthProviderNames();
            if (!order.remove(providerName)) {
                throw new NothingToDelete("No provider '" + providerName + "' to delete");
            }
            // Persist the order removal first (disables the provider in config).
            securityManager.saveSecurityConfig(smc);

            SecurityAuthProviderConfig cfg = securityManager.loadAuthenticationProviderConfig(providerName);
            securityManager.removeAuthenticationProvider(cfg);

            securityManager.reload();
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    // --------------------------------------------------------------------------- /order API -------

    /**
     * Replace the entire enabled order. The supplied list becomes the exact content of {@code <authproviderNames>} in
     * the global security configuration. Providers not listed remain saved on disk but are disabled.
     *
     * <p>Payloads:
     *
     * <ul>
     *   <li>JSON: <code>{"order":["name1","name2"]}</code>
     *   <li>XML: <code>&lt;order&gt;&lt;order&gt;name1&lt;/order&gt;...&lt;/order&gt;</code>
     * </ul>
     */
    @PutMapping(
            path = {"/order", "/order.{ext:xml|json}"},
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> reorder(HttpServletRequest request) {
        checkAuthorised();
        try {
            List<String> wanted = normalizeNames(parseOrder(request));
            checkArgument(!wanted.isEmpty(), "`order` array required");

            // ensure no duplicates (preserving first occurrence order)
            LinkedHashSet<String> dedup = new LinkedHashSet<>(wanted);
            if (dedup.size() != wanted.size()) {
                throw new BadRequest("Duplicate entries in order");
            }

            // known configs on disk
            Set<String> known = securityManager.listAuthenticationProviders();
            for (String n : wanted) {
                if (!known.contains(n)) throw new BadRequest("Unknown provider: " + n);
            }

            saveConfigOrder(wanted);
            securityManager.reload();
            return ResponseEntity.ok().build();

        } catch (IOException e) {
            throw new CannotReadConfig(e);
        } catch (Exception e) {
            throw new BadRequest(e.getMessage());
        }
    }

    // Methods not allowed on /order
    @GetMapping(path = {"/order", "/order.{ext:xml|json}"})
    public ResponseEntity<Void> orderGetNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @PostMapping(path = {"/order", "/order.{ext}"})
    public ResponseEntity<Void> orderPostNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @DeleteMapping(path = {"/order", "/order.{ext}"})
    public ResponseEntity<Void> orderDeleteNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    // ------------------------------------------------------------------------------------ helpers

    /** Strip a trailing ".xml" or ".json" (case-insensitive) from a provider name. */
    private static String normalizeName(String n) {
        return n == null ? null : n.replaceFirst("\\.(?i)(xml|json)$", "");
    }

    private static List<String> normalizeNames(List<String> names) {
        List<String> out = new ArrayList<>(names.size());
        for (String n : names) {
            out.add(normalizeName(n));
        }
        return out;
    }

    /**
     * Cache of resolved config classes keyed by 'className' values from payloads. Values are either a
     * {@link SecurityAuthProviderConfig} subclass FQN or a provider FQN which is then mapped to a conventional config
     * class.
     */
    private static final ConcurrentMap<String, Class<? extends SecurityAuthProviderConfig>> CONFIG_CACHE =
            new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private Class<? extends SecurityAuthProviderConfig> resolveConfigClass(String className) {
        return CONFIG_CACHE.computeIfAbsent(className, cn -> {
            // A) className is already a config class
            try {
                Class<?> c = Class.forName(cn);
                if (SecurityAuthProviderConfig.class.isAssignableFrom(c)) {
                    return (Class<? extends SecurityAuthProviderConfig>) c;
                }
            } catch (ClassNotFoundException ignore) {
                // fall through
            }

            // B) className is a provider class – derive plausible config classes
            try {
                Class<?> provider = Class.forName(cn);
                String simple = provider.getSimpleName() + "Config";
                String pkg = provider.getPackage().getName();

                String[] candidates = {
                    pkg.replace(".auth", ".config") + "." + simple, // typical
                    "org.geoserver.security.config." + simple, // fallback
                    pkg + "." + simple // same package (defensive)
                };

                for (String fqn : candidates) {
                    try {
                        Class<?> cfg = Class.forName(fqn);
                        if (SecurityAuthProviderConfig.class.isAssignableFrom(cfg)) {
                            return (Class<? extends SecurityAuthProviderConfig>) cfg;
                        }
                    } catch (ClassNotFoundException ignore2) {
                        // try the next candidate
                    }
                }
            } catch (ClassNotFoundException ignore) {
                // not a provider class either
            }

            return null; // not resolvable
        });
    }

    private SecurityAuthProviderConfig parseConfig(HttpServletRequest req) throws IOException {
        byte[] body = read(req);

        if (isXml(req)) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configurePersister(xp, null);
            Object o = xp.load(new ByteArrayInputStream(body), Object.class);
            if (o instanceof SecurityAuthProviderConfig cfg) return cfg;
            if (o instanceof AuthProviderCollection c && c.first() != null) return c.first();
            throw new BadRequest("Malformed XML payload");
        }

        // JSON
        JsonNode n = MAPPER.readTree(body);
        if (n == null || n.isNull()) throw new BadRequest("Empty JSON payload");

        // unwrap "authprovider" or single-item "authproviders"
        if (n.has("authprovider")) n = n.get("authprovider");
        if (n.has("authproviders")
                && n.get("authproviders").isArray()
                && n.get("authproviders").size() == 1) {
            n = n.get("authproviders").get(0);
        }
        if (!n.isObject()) throw new BadRequest("Malformed JSON payload");

        String className = n.path("className").asText(null);
        if (className == null || className.isBlank()) {
            throw new BadRequest("Missing 'className' in JSON payload");
        }
        Class<? extends SecurityAuthProviderConfig> type = resolveConfigClass(className);
        if (type == null) {
            throw new BadRequest("Unsupported className: " + className);
        }

        SecurityAuthProviderConfig cfg = MAPPER.treeToValue(n, type);
        // Light validation here; detailed checks are performed by GeoServerSecurityManager validators
        ensureNotReserved(cfg.getName());
        return cfg;
    }

    private List<String> parseOrder(HttpServletRequest req) throws IOException {
        byte[] body = read(req);
        if (isXml(req)) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configurePersister(xp, null);
            Object o = xp.load(new ByteArrayInputStream(body), Object.class);
            if (o instanceof AuthProviderOrder ord
                    && ord.getOrder() != null
                    && !ord.getOrder().isEmpty()) {
                return ord.getOrder();
            }
            throw new BadRequest("`order` array required");
        } else {
            JsonNode root = MAPPER.readTree(body);
            if (root.has("order")) root = root.get("order");
            if (!root.isArray() || root.isEmpty()) throw new BadRequest("`order` array required");
            List<String> out = new ArrayList<>();
            root.forEach(x -> out.add(x.asText()));
            return out;
        }
    }

    private static byte[] read(HttpServletRequest r) throws IOException {
        try (ServletInputStream in = r.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static boolean isXml(HttpServletRequest r) {
        String ct = Optional.ofNullable(r.getContentType()).orElse("");
        // keep simple: "contains" allows for charset suffixes
        return ct.contains(MediaType.APPLICATION_XML_VALUE) || ct.contains(MediaType.TEXT_XML_VALUE);
    }

    private List<String> getConfigOrder() throws IOException {
        return new ArrayList<>(securityManager.loadSecurityConfig().getAuthProviderNames());
    }

    private void saveConfigOrder(List<String> order) {
        try {
            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            cfg.getAuthProviderNames().clear();
            cfg.getAuthProviderNames().addAll(order);
            securityManager.saveSecurityConfig(cfg);
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    private SecurityAuthProviderConfig loadConfigOrError(String n) {
        try {
            SecurityAuthProviderConfig c = securityManager.loadAuthenticationProviderConfig(n);
            if (c == null) throw new ProviderNotFound(n);
            return c;
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }
    }

    private static void validatePosition(int position, int size) {
        checkArgument(
                position >= 0 && position <= size - (size == 0 ? 0 : 1) + (position == size ? 1 : 0),
                "position out of range");
        // The above condition accepts [0..size] when inserting (create), and [0..size-1] for
        // update where we compare/remove before insert. To keep semantics clear, callers
        // pass the correct 'size' (current size for update, current size for create
        // when position==size is allowed). If invalid, an IAE is thrown and mapped to 400.
    }

    private void ensureNotReserved(String n) {
        checkArgument(n != null && !n.isBlank(), "name required");
        checkArgument(!RESERVED.contains(n.toLowerCase()), "'%s' is reserved", n);
    }

    private void checkAuthorised() {
        if (!securityManager.checkAuthenticationForAdminRole()) throw new NotAuthorised();
    }

    // --------------------------------------------------------------------------- exceptions -------

    @ResponseStatus(HttpStatus.FORBIDDEN)
    static class NotAuthorised extends RuntimeException {
        NotAuthorised() {
            super("Admin role required");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class BadRequest extends RuntimeException {
        BadRequest(String m) {
            super(m);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    static class CannotSaveConfig extends RuntimeException {
        CannotSaveConfig(Exception e) {
            super("Cannot save security configuration", e);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    static class CannotReadConfig extends RuntimeException {
        CannotReadConfig(Exception e) {
            super("Cannot read security configuration", e);
        }
    }

    static class DuplicateProviderName extends RuntimeException {
        DuplicateProviderName(String n) {
            super("Provider '" + n + "' already exists");
        }
    }

    static class ProviderNotFound extends RuntimeException {
        ProviderNotFound(String n) {
            super("Provider '" + n + "' not found");
        }
    }

    static class NothingToDelete extends RuntimeException {
        NothingToDelete(String n) {
            super("No provider '" + n + "' to delete");
        }
    }

    /** Simple error envelope used by {@link #handle(RuntimeException)}. */
    static class ErrorResponse {
        private final int status;
        private final String message;

        ErrorResponse(int s, String m) {
            status = s;
            message = m;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    /** Map known exceptions to stable HTTP codes and payload. */
    @ExceptionHandler({
        CannotSaveConfig.class,
        CannotReadConfig.class,
        BadRequest.class,
        ProviderNotFound.class,
        DuplicateProviderName.class,
        NothingToDelete.class,
        NotAuthorised.class,
        IllegalArgumentException.class // <-- add this
    })
    public ResponseEntity<ErrorResponse> handle(RuntimeException ex) {
        HttpStatus st = ex instanceof BadRequest || ex instanceof IllegalArgumentException
                ? HttpStatus.BAD_REQUEST
                : ex instanceof ProviderNotFound
                        ? HttpStatus.NOT_FOUND
                        : ex instanceof DuplicateProviderName
                                ? HttpStatus.BAD_REQUEST
                                : ex instanceof NothingToDelete
                                        ? HttpStatus.GONE
                                        : ex instanceof NotAuthorised
                                                ? HttpStatus.FORBIDDEN
                                                : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(new ErrorResponse(st.value(), ex.getMessage()), st);
    }
}

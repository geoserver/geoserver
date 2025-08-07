/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
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
import org.geoserver.security.validation.SecurityConfigException;
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
 * REST controller for the <em>Authentication Providers</em> resource.
 *
 * <p>Extends SequentialExecutionController as we do not want parallel updates to occur as this may jeopardize the
 * integrity of the XML files
 */
@RestController
@RequestMapping(RestBaseController.ROOT_PATH + "/security/authProviders")
public class AuthenticationProviderRestController extends RestBaseController {

    private static final Set<String> RESERVED = Set.of("order");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final GeoServerSecurityManager securityManager;

    public AuthenticationProviderRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    // ------------------------------------------------------------------ XStream
    @Override
    public void configurePersister(XStreamPersister xp, XStreamMessageConverter conv) {
        super.configurePersister(xp, conv);
        XStream xs = xp.getXStream();

        xs.allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});

        xs.alias("authProviders", AuthProviderCollection.class);
        xs.addImplicitCollection(AuthProviderCollection.class, "providers", null, SecurityAuthProviderConfig.class);

        xs.alias("order", AuthProviderOrder.class);
        xs.addImplicitCollection(AuthProviderOrder.class, "order", "order", String.class);
    }

    // --------------------------------------------------------------------- LIST + ITEM
    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
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

    @GetMapping(
            path = "/{providerName:^(?!order$).+}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<SecurityAuthProviderConfig> one(@PathVariable String providerName) {
        checkAuthorised();
        return wrapObject(loadConfigOrError(providerName), SecurityAuthProviderConfig.class);
    }

    // --------------------------------------------------------------------- CREATE
    @PostMapping(
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<SecurityAuthProviderConfig>> create(
            HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position,
            UriComponentsBuilder builder) {

        checkAuthorised();
        try {
            SecurityAuthProviderConfig cfg = parseConfig(request);
            ensureNotReserved(cfg.getName());

            List<String> order = getConfigOrder();
            if (order.contains(cfg.getName())) throw new DuplicateProviderName(cfg.getName());

            int pos = (position != null) ? position : order.size();
            checkArgument(pos >= 0 && pos <= order.size(), "position out of range");

            securityManager.saveAuthenticationProvider(cfg);
            order.add(pos, cfg.getName());
            saveConfigOrder(order);
            securityManager.reload();

            HttpHeaders h = new HttpHeaders();
            h.setLocation(builder.path("/security/authProviders/{name}")
                    .buildAndExpand(cfg.getName())
                    .toUri());
            return new ResponseEntity<>(wrapObject(cfg, SecurityAuthProviderConfig.class), h, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    // --------------------------------------------------------------------- UPDATE
    @PutMapping(
            path = "/{providerName:^(?!order$).+}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<SecurityAuthProviderConfig> update(
            @PathVariable String providerName,
            HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position) {

        checkAuthorised();
        try {
            SecurityAuthProviderConfig incoming = parseConfig(request);
            ensureNotReserved(incoming.getName());
            if (!Objects.equals(providerName, incoming.getName()))
                throw new BadRequest("path name and payload name differ");

            List<String> order = getConfigOrder();
            int idx = order.indexOf(providerName);
            if (idx < 0) throw new ProviderNotFound(providerName);

            securityManager.saveAuthenticationProvider(incoming);

            if (position != null && position >= 0 && position < order.size() && idx != position) {
                order.remove(idx);
                order.add(position, providerName);
                saveConfigOrder(order);
            }
            securityManager.reload();
            return wrapObject(incoming, SecurityAuthProviderConfig.class);

        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    // --------------------------------------------------------------------- DELETE
    @DeleteMapping(path = "/{providerName:^(?!order$).+}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable String providerName) {
        checkAuthorised();
        try {
            List<String> order = new ArrayList<>(getConfigOrder());
            if (!order.remove(providerName)) throw new NothingToDelete(providerName);

            securityManager.removeAuthenticationProvider(loadConfigOrError(providerName));
            saveConfigOrder(order);
            securityManager.reload();

        } catch (IOException | SecurityConfigException e) {
            throw new CannotSaveConfig(e);
        }
    }

    // ===================================================================== /order API
    // -- PUT /order or /order.{ext} ---------------------------------------
    @PutMapping(
            path = {"/order", "/order.{ext}"},
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> reorder(HttpServletRequest request) {
        checkAuthorised();
        try {
            List<String> wanted = parseOrder(request);
            checkArgument(!wanted.isEmpty(), "`order` array required");

            // known configs on disk:
            Set<String> known = securityManager.listAuthenticationProviders();
            // ensure payload contains only known names
            for (String n : wanted) if (!known.contains(n)) throw new BadRequest("Unknown provider: " + n);

            saveConfigOrder(wanted); // may enable/disable providers
            securityManager.reload();
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    // -- GET/POST/DELETE /order respond 405 --------------------------------
    @GetMapping(path = {"/order", "/order.{ext}"})
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

    // --------------------------------------------------------------------- helpers
    private SecurityAuthProviderConfig parseConfig(HttpServletRequest req) throws IOException {
        byte[] body = read(req);
        if (isXml(req)) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configurePersister(xp, null);
            Object o = xp.load(new ByteArrayInputStream(body), Object.class);
            if (o instanceof SecurityAuthProviderConfig cfg) return cfg;
            if (o instanceof AuthProviderCollection c && c.first() != null) return c.first();
            throw new BadRequest("Malformed XML payload");
        } else {
            JsonNode n = MAPPER.readTree(body);
            if (n.has("authProvider")) n = n.get("authProvider");
            if (n.has("authProviders")
                    && n.get("authProviders").isArray()
                    && n.get("authProviders").size() == 1)
                n = n.get("authProviders").get(0);
            if (!n.isObject()) throw new BadRequest("Malformed JSON payload");
            return MAPPER.treeToValue(n, SecurityAuthProviderConfig.class);
        }
    }

    private List<String> parseOrder(HttpServletRequest req) throws IOException {
        byte[] body = read(req);
        if (isXml(req)) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configurePersister(xp, null);
            Object o = xp.load(new ByteArrayInputStream(body), Object.class);
            if (o instanceof AuthProviderOrder ord
                    && ord.getOrder() != null
                    && !ord.getOrder().isEmpty()) return ord.getOrder();
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

    private void ensureNotReserved(String n) {
        checkArgument(n != null && !n.isBlank(), "name required");
        checkArgument(!RESERVED.contains(n.toLowerCase()), "'%s' is reserved", n);
    }

    private void checkAuthorised() {
        if (!securityManager.checkAuthenticationForAdminRole()) throw new NotAuthorised();
    }

    // --------------------------------------------------------------------- exceptions
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

    static class ErrorResponse {
        private int status;
        private String message;

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

    @ExceptionHandler({
        CannotSaveConfig.class,
        CannotReadConfig.class,
        BadRequest.class,
        ProviderNotFound.class,
        DuplicateProviderName.class,
        NothingToDelete.class,
        NotAuthorised.class
    })
    public ResponseEntity<ErrorResponse> handle(RuntimeException ex) {
        HttpStatus st = ex instanceof BadRequest
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

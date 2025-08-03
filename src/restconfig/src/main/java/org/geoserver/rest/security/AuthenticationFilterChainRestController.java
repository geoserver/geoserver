/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static com.google.common.base.Preconditions.checkArgument;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.security.xml.AuthFilterChainCollection;
import org.geoserver.rest.security.xml.AuthFilterChainFilters;
import org.geoserver.rest.security.xml.AuthFilterChainOrder;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
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

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/filterChain")
public class AuthenticationFilterChainRestController extends RestBaseController {

    private final GeoServerSecurityManager securityManager;

    public AuthenticationFilterChainRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    // ---------- XStream wiring ----------
    private static void configureAliases(XStreamPersister persister) {
        XStream xs = persister.getXStream();

        xs.allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});

        // root
        xs.alias("filterChain", AuthFilterChainCollection.class);

        // collection: no <chains> wrapper; each item is <filters>
        xs.addImplicitCollection(AuthFilterChainCollection.class, "chains", "filters", AuthFilterChainFilters.class);

        // item
        xs.alias("filters", AuthFilterChainFilters.class);
        xs.aliasField("class", AuthFilterChainFilters.class, "clazz");
        xs.aliasAttribute(AuthFilterChainFilters.class, "requireSSL", "ssl");
        // Disable XStream's special meaning for the "class" attribute
        xs.aliasSystemAttribute(null, "class");
        xs.aliasSystemAttribute(null, "resolves-to"); // (optional, belts & braces)

        xs.useAttributeFor(AuthFilterChainFilters.class, "name");
        xs.useAttributeFor(AuthFilterChainFilters.class, "clazz");
        xs.useAttributeFor(AuthFilterChainFilters.class, "path");
        xs.useAttributeFor(AuthFilterChainFilters.class, "disabled");
        xs.useAttributeFor(AuthFilterChainFilters.class, "allowSessionCreation");
        xs.useAttributeFor(AuthFilterChainFilters.class, "requireSSL");
        xs.useAttributeFor(AuthFilterChainFilters.class, "matchHTTPMethod");
        xs.useAttributeFor(AuthFilterChainFilters.class, "interceptorName");
        xs.useAttributeFor(AuthFilterChainFilters.class, "exceptionTranslationName");
        xs.useAttributeFor(AuthFilterChainFilters.class, "roleFilterName");

        xs.addImplicitCollection(AuthFilterChainFilters.class, "filters", "filter", String.class);
    }

    // ---------- Endpoints ----------

    @GetMapping(
            path = "",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getAllXml() {
        checkAuthorised();
        try {
            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains = cfg.getFilterChain().getRequestChains();

            AuthFilterChainCollection col = new AuthFilterChainCollection();
            col.setChains(chains.stream().map(this::toDTO).collect(Collectors.toList()));

            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xp.save(col, baos);
            String xml = baos.toString(StandardCharsets.UTF_8);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml);

        } catch (IOException e) {
            throw new CannotReadConfig(e);
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    @PutMapping(
            path = "",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> replaceAllXml(HttpServletRequest request) {
        checkAuthorised();
        try {
            byte[] body = request.getInputStream().readAllBytes();
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp);

            AuthFilterChainCollection incoming =
                    xp.load(new ByteArrayInputStream(body), AuthFilterChainCollection.class);

            List<AuthFilterChainFilters> dtos =
                    Optional.ofNullable(incoming.getChains()).orElse(Collections.emptyList());
            checkArgument(!dtos.isEmpty(), "At least one chain must be provided");

            Set<String> names = new HashSet<>();
            for (AuthFilterChainFilters dto : dtos) {
                checkArgument(dto.getName() != null && !dto.getName().isEmpty(), "Each chain needs a name");
                checkArgument(dto.getClazz() != null && !dto.getClazz().isEmpty(), "Each chain needs a class");
                checkArgument(names.add(dto.getName()), "Duplicate chain name: %s", dto.getName());
            }

            List<RequestFilterChain> rewritten =
                    dtos.stream().map(this::toModel).collect(Collectors.toList());

            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            cfg.setFilterChain(new GeoServerSecurityFilterChain(rewritten));
            securityManager.saveSecurityConfig(cfg);
            securityManager.reload();

            AuthFilterChainCollection out = new AuthFilterChainCollection();
            out.setChains(rewritten.stream().map(this::toDTO).collect(Collectors.toList()));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xp.save(out, baos);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(baos.toString(StandardCharsets.UTF_8));

        } catch (IllegalArgumentException ex) {
            throw new BadRequest(ex.getMessage());
        } catch (IOException ex) {
            throw new CannotReadConfig(ex);
        } catch (Exception ex) {
            throw new CannotSaveConfig(ex);
        }
    }

    // ---------- Per-chain: XML-only, manual marshalling ----------

    @PutMapping(
            path = "/order",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Void> reorder(HttpServletRequest request) {
        checkAuthorised();
        try {
            AuthFilterChainOrder order = parseOrder(request);
            List<String> wanted = Optional.ofNullable(order.getOrder()).orElse(Collections.emptyList());
            checkArgument(!wanted.isEmpty(), "`order` list required");

            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            List<RequestFilterChain> current =
                    new ArrayList<>(cfg.getFilterChain().getRequestChains());

            Set<String> currentNames =
                    current.stream().map(RequestFilterChain::getName).collect(Collectors.toSet());
            checkArgument(
                    currentNames.equals(new HashSet<>(wanted)), "Order must include exactly the current set of chains");

            Map<String, RequestFilterChain> byName =
                    current.stream().collect(Collectors.toMap(RequestFilterChain::getName, c -> c));
            List<RequestFilterChain> reordered =
                    wanted.stream().map(byName::get).collect(Collectors.toList());

            cfg.setFilterChain(new GeoServerSecurityFilterChain(reordered));
            securityManager.saveSecurityConfig(cfg);
            securityManager.reload();

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException ex) {
            throw new BadRequest(ex.getMessage());
        } catch (IOException ex) {
            throw new CannotReadConfig(ex);
        } catch (Exception ex) {
            throw new CannotSaveConfig(ex);
        }
    }

    @GetMapping(path = "/order")
    public ResponseEntity<Void> orderGetNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @PostMapping(path = "/order")
    public ResponseEntity<Void> orderPostNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @DeleteMapping(path = "/order")
    public ResponseEntity<Void> orderDeleteNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @GetMapping(
            path = "/{chainName:^(?!order$).+}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getOneXml(@PathVariable String chainName) {
        String finalChainName = normalizeChainName(chainName);
        checkAuthorised();
        try {
            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            RequestFilterChain chain = Optional.ofNullable(cfg.getFilterChain().getRequestChainByName(chainName))
                    .orElseThrow(() -> new FilterChainNotFound(finalChainName));

            AuthFilterChainFilters dto = toDTO(chain);

            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp); // already defines alias "filters" -> AuthFilterChainFilters as element
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xp.save(dto, baos); // root element will be <filters ...>
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(baos.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }
    }

    @PostMapping(
            path = "",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> createOneXml(
            HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position,
            UriComponentsBuilder builder) {
        checkAuthorised();
        try {
            byte[] body = request.getInputStream().readAllBytes();
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp);

            AuthFilterChainFilters dto = xp.load(new ByteArrayInputStream(body), AuthFilterChainFilters.class);
            ensureNotReserved(dto.getName());
            RequestFilterChain model = toModel(dto);

            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains =
                    new ArrayList<>(cfg.getFilterChain().getRequestChains());

            if (chains.stream().anyMatch(c -> Objects.equals(c.getName(), model.getName()))) {
                throw new DuplicateChainName(model.getName());
            }

            int pos = (position != null) ? position : chains.size();
            checkArgument(pos >= 0 && pos <= chains.size(), "position out of range");
            chains.add(pos, model);

            cfg.setFilterChain(new GeoServerSecurityFilterChain(chains));
            securityManager.saveSecurityConfig(cfg);
            securityManager.reload();

            // echo created resource as XML
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xp.save(dto, baos);

            UriComponentsBuilder ub = (builder != null ? builder : UriComponentsBuilder.newInstance());
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ub.path("/security/filterChain/{name}")
                    .buildAndExpand(model.getName())
                    .toUri());
            headers.setContentType(MediaType.APPLICATION_XML);
            return new ResponseEntity<>(baos.toString(StandardCharsets.UTF_8), headers, HttpStatus.CREATED);
        } catch (com.thoughtworks.xstream.converters.ConversionException | ClassCastException ex) {
            // bad/mismatched XML => 400
            throw new BadRequest("Malformed XML: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            // validation (duplicate name, bad position, mismatch name, etc.) => 400
            throw new BadRequest(ex.getMessage());
        } catch (IOException ex) {
            // persistence issues reading current config
            throw new CannotReadConfig(ex);
        } catch (ReflectiveOperationException ex) {
            // building chain from class name failed
            throw new CannotMakeChain("dto.getClazz()", ex);
        } catch (Exception e) {
            // 1) keep the specific exceptions visible to tests/clients
            rethrowIfDomain(e);

            // 2) map parse/marshalling problems to 400
            if (e instanceof com.thoughtworks.xstream.converters.ConversionException
                    || e instanceof ClassCastException
                    || causedBy(e, com.thoughtworks.xstream.converters.ConversionException.class)
                    || causedBy(e, ClassCastException.class)) {
                throw new BadRequest("Malformed request: " + e.getMessage());
            }

            // 3) IO/persistence -> 500 CannotSaveConfig (or CannotUpdateConfig in PUT)
            if (e instanceof java.io.IOException || causedBy(e, java.io.IOException.class)) {
                throw new CannotSaveConfig(e);
            }

            // 4) last resort
            throw new CannotSaveConfig(e);
        }
    }

    @PutMapping(
            path = "/{chainName:^(?!order$).+}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> updateOneXml(
            @PathVariable String chainName,
            HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position) {

        chainName = normalizeChainName(chainName);
        checkAuthorised();
        try {
            byte[] body = request.getInputStream().readAllBytes();
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp);

            AuthFilterChainFilters dto = xp.load(new ByteArrayInputStream(body), AuthFilterChainFilters.class);
            ensureNotReserved(dto.getName());
            RequestFilterChain incoming = toModel(dto);

            checkArgument(Objects.equals(chainName, incoming.getName()), "chainName must match payload name");

            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains =
                    new ArrayList<>(cfg.getFilterChain().getRequestChains());
            int existingIdx = indexOf(chains, chainName);
            checkArgument(existingIdx >= 0, "Filter chain %s not found", chainName);

            chains.set(existingIdx, incoming);
            if (position != null && position >= 0 && position < chains.size() && position != existingIdx) {
                chains.remove(existingIdx);
                chains.add(position, incoming);
            }

            cfg.setFilterChain(new GeoServerSecurityFilterChain(chains));
            securityManager.saveSecurityConfig(cfg);
            securityManager.reload();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xp.save(dto, baos);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(baos.toString(StandardCharsets.UTF_8));

        } catch (com.thoughtworks.xstream.converters.ConversionException | ClassCastException ex) {
            throw new BadRequest("Malformed XML: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new BadRequest(ex.getMessage());
        } catch (IOException ex) {
            throw new CannotReadConfig(ex);
        } catch (ReflectiveOperationException ex) {
            throw new CannotMakeChain("dto.getClazz()", ex);
        } catch (Exception e) {
            rethrowIfDomain(e);
            if (e instanceof IOException || causedBy(e, IOException.class)) throw new CannotSaveConfig(e);
            throw new CannotSaveConfig(e);
        }
    }

    @DeleteMapping(path = "/{chainName:^(?!order$).+}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteOne(@PathVariable String chainName) {
        chainName = normalizeChainName(chainName);
        checkAuthorised();
        try {
            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            GeoServerSecurityFilterChain chainCfg = cfg.getFilterChain();

            List<RequestFilterChain> chains = new ArrayList<>(chainCfg.getRequestChains());
            int idx = indexOf(chains, chainName);
            if (idx < 0) throw new NothingToDelete(chainName);

            RequestFilterChain toRemove = chains.get(idx);
            checkArgument(toRemove.canBeRemoved(), "Filter chain %s cannot be removed.", chainName);
            chains.remove(idx);

            cfg.setFilterChain(new GeoServerSecurityFilterChain(chains));
            securityManager.saveSecurityConfig(cfg);
            securityManager.reload();
        } catch (Exception e) {
            rethrowIfDomain(e); // lets NothingToDelete/BadRequest/etc. bubble out

            // optional: classify common infrastructural errors
            if (e instanceof java.io.IOException || causedBy(e, java.io.IOException.class)) {
                throw new CannotSaveConfig(e);
            }
            // fallback
            throw new CannotSaveConfig(e);
        }
    }

    private static int indexOf(List<RequestFilterChain> chains, String name) {
        for (int i = 0; i < chains.size(); i++) {
            if (Objects.equals(chains.get(i).getName(), name)) return i;
        }
        return -1;
    }

    // ---------- Mapping helpers ----------

    private static final Set<String> RESERVED = Set.of("order");

    private static String normalizeChainName(String n) {
        return n == null ? null : n.replaceFirst("\\.xml$", "");
    }

    private static void ensureNotReserved(String name) {
        checkArgument(!RESERVED.contains(name.toLowerCase()), "'%s' is reserved", name);
    }

    private AuthFilterChainOrder parseOrder(HttpServletRequest req) throws IOException {
        byte[] body = req.getInputStream().readAllBytes();
        String ct = Optional.ofNullable(req.getContentType()).orElse("");
        if (ct.contains(MediaType.APPLICATION_XML_VALUE) || ct.contains(MediaType.TEXT_XML_VALUE)) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp);
            return xp.load(new ByteArrayInputStream(body), AuthFilterChainOrder.class);
        } else {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, AuthFilterChainOrder.class);
        }
    }

    private AuthFilterChainFilters toDTO(RequestFilterChain c) {
        AuthFilterChainFilters dto = new AuthFilterChainFilters();
        dto.setName(c.getName());
        dto.setClazz(c.getClass().getName());
        // path is a CSV in XML — join patterns
        List<String> patterns = Optional.ofNullable(c.getPatterns()).orElse(Collections.emptyList());
        dto.setPath(String.join(",", patterns));
        dto.setDisabled(c.isDisabled());
        dto.setAllowSessionCreation(c.isAllowSessionCreation());
        dto.setRequireSSL(c.isRequireSSL());
        dto.setMatchHTTPMethod(c.isMatchHTTPMethod());
        dto.setRoleFilterName(c.getRoleFilterName());
        dto.setFilters(new ArrayList<>(Optional.ofNullable(c.getFilterNames()).orElse(Collections.emptyList())));

        // optional subclass fields via reflection (if present)
        dto.setInterceptorName(invokeStringGetter(c, "getInterceptorName"));
        dto.setExceptionTranslationName(invokeStringGetter(c, "getExceptionTranslationName"));
        return dto;
    }

    private RequestFilterChain toModel(AuthFilterChainFilters dto) {
        try {
            Class<?> raw = Class.forName(dto.getClazz());
            checkArgument(
                    RequestFilterChain.class.isAssignableFrom(raw),
                    "Class %s is not a RequestFilterChain",
                    dto.getClazz());
            Class<?> chainClass = Class.forName(dto.getClazz());
            RequestFilterChain chain = instantiateChain(chainClass.asSubclass(RequestFilterChain.class), dto);

            chain.setName(dto.getName());
            chain.setPatterns(splitCSV(dto.getPath()));
            if (dto.getDisabled() != null) chain.setDisabled(dto.getDisabled());
            if (dto.getAllowSessionCreation() != null) chain.setAllowSessionCreation(dto.getAllowSessionCreation());
            if (dto.getRequireSSL() != null) chain.setRequireSSL(dto.getRequireSSL());
            if (dto.getMatchHTTPMethod() != null) chain.setMatchHTTPMethod(dto.getMatchHTTPMethod());
            if (dto.getRoleFilterName() != null) chain.setRoleFilterName(dto.getRoleFilterName());
            chain.setFilterNames(Optional.ofNullable(dto.getFilters()).orElse(Collections.emptyList()));

            // optional subclass setters if present
            invokeStringSetter(chain, "setInterceptorName", dto.getInterceptorName());
            invokeStringSetter(chain, "setExceptionTranslationName", dto.getExceptionTranslationName());

            return chain;

        } catch (IllegalArgumentException e) {
            throw e; // let controller wrap as BadRequest
        } catch (Exception e) {
            throw new CannotMakeChain(dto.getClazz(), e);
        }
    }

    @SuppressWarnings("PMD.UseExplicitTypes")
    private RequestFilterChain instantiateChain(Class<? extends RequestFilterChain> type, AuthFilterChainFilters dto)
            throws Exception {

        // Prefer a no-arg if it exists
        try {
            var c0 = type.getDeclaredConstructor();
            c0.setAccessible(true);
            return c0.newInstance();
        } catch (NoSuchMethodException ignore) {
        }

        // Prefer a single var-arg String[] (VariableFilterChain pattern-ctor)
        for (var ctor : type.getDeclaredConstructors()) {
            Class<?>[] p = ctor.getParameterTypes();
            if (p.length == 1 && p[0].isArray() && p[0].getComponentType() == String.class) {
                ctor.setAccessible(true);
                String[] patterns = splitCSVToArray(dto.getPath()); // never null
                return (RequestFilterChain) ctor.newInstance(new Object[] {patterns});
            }
        }

        // Generic fallback: try each ctor with best-effort args
        Exception last = null;
        for (var ctor : type.getDeclaredConstructors()) {
            try {
                ctor.setAccessible(true);
                Class<?>[] p = ctor.getParameterTypes();
                Object[] args = new Object[p.length];
                for (int i = 0; i < p.length; i++) args[i] = defaultArgFor(p[i], dto);
                return (RequestFilterChain) ctor.newInstance(args);
            } catch (Exception e) {
                last = e;
            }
        }

        throw new NoSuchMethodException("No suitable constructor for " + type.getName()
                + (last != null ? " (last: " + last.getClass().getSimpleName() + ": " + last.getMessage() + ")" : ""));
    }

    private Object defaultArgFor(Class<?> t, AuthFilterChainFilters dto) {
        if (t.isArray() && t.getComponentType() == String.class) {
            return splitCSVToArray(dto.getPath()); // [] not null
        }
        if (!t.isPrimitive()) {
            if (t == String.class) return null;
            if (List.class.isAssignableFrom(t) || Collection.class.isAssignableFrom(t)) return new ArrayList<>();
            if (Set.class.isAssignableFrom(t)) return new HashSet<>();
            if (Map.class.isAssignableFrom(t)) return new HashMap<>();
            return null;
        }
        if (t == boolean.class) return false;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == double.class) return 0d;
        if (t == float.class) return 0f;
        if (t == short.class) return (short) 0;
        if (t == byte.class) return (byte) 0;
        if (t == char.class) return (char) 0;
        return null;
    }

    private static String[] splitCSVToArray(String csv) {
        if (csv == null || csv.trim().isEmpty()) return new String[0];
        return Arrays.stream(csv.split("\\s*,\\s*")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private static List<String> splitCSV(String csv) {
        if (csv == null || csv.trim().isEmpty()) return Collections.emptyList();
        String[] parts = csv.split("\\s*,\\s*");
        List<String> list = new ArrayList<>(parts.length);
        for (String p : parts) if (!p.isEmpty()) list.add(p);
        return list;
    }

    private static String invokeStringGetter(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            Object val = m.invoke(target);
            return (val instanceof String) ? (String) val : null;
        } catch (Exception ignored) {
            return null; // method not present; fine
        }
    }

    private static void invokeStringSetter(Object target, String method, String value) {
        if (value == null) return;
        try {
            Method m = target.getClass().getMethod(method, String.class);
            m.invoke(target, value);
        } catch (Exception ignored) {
            // setter not present on this subclass; ignore
        }
    }

    // ---------- Auth & errors ----------

    private void checkAuthorised() {
        if (!securityManager.checkAuthenticationForAdminRole()) {
            throw new NotAuthorised();
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class NotAuthorised extends RuntimeException {
        public NotAuthorised() {
            super("Admin role required to access this resource");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class BadRequest extends RuntimeException {
        public BadRequest(String m) {
            super(m);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class CannotSaveConfig extends RuntimeException {
        public CannotSaveConfig(Exception e) {
            super("Cannot save the Security configuration", e);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class CannotReadConfig extends RuntimeException {
        public CannotReadConfig(Exception e) {
            super("Cannot read the Security configuration", e);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class CannotMakeChain extends RuntimeException {
        public CannotMakeChain(String cn, Exception e) {
            super("Cannot make class " + cn, e);
        }
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

    @ExceptionHandler(NotAuthorised.class)
    public ResponseEntity<ErrorResponse> handleRestException(NotAuthorised exception) {
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
    private static boolean causedBy(Throwable t, Class<? extends Throwable> type) {
        while (t != null) {
            if (type.isInstance(t)) return true;
            t = t.getCause();
        }
        return false;
    }

    private void rethrowIfDomain(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof DuplicateChainName) throw (DuplicateChainName) t;
            if (t instanceof NothingToDelete) throw (NothingToDelete) t;
            if (t instanceof BadRequest) throw (BadRequest) t;
            if (t instanceof CannotMakeChain) throw (CannotMakeChain) t;
            if (t instanceof IllegalArgumentException) throw new BadRequest(t.getMessage());
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Exceptions

    public static class CannotUpdateConfig extends RuntimeException {
        public CannotUpdateConfig(Exception ex) {
            super("Cannot update the Security configuration ", ex);
        }
    }

    public static class NothingToDelete extends RuntimeException {
        public NothingToDelete(String filterName) {
            super("Cannot delete " + filterName + " as no filter exists");
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
}

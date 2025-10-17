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
import java.lang.reflect.Method;
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
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.security.xml.AuthFilterChainCollection;
import org.geoserver.rest.security.xml.AuthFilterChainFilters;
import org.geoserver.rest.security.xml.AuthFilterChainOrder;
import org.geoserver.rest.wrapper.RestWrapper;
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
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/filterchain")
public class AuthenticationFilterChainRestController extends RestBaseController {

    private static final Set<String> RESERVED = Set.of("order");

    // --- JSON/XML parsing ----------------------------------------------------
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private static final String CHAIN_PATH = "/{chainName:^(?!order(?:\\.(?:json|xml))?$).+}";

    private final GeoServerSecurityManager securityManager;

    public AuthenticationFilterChainRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    // ---------------------------------------------------------------------
    // RestBaseController integration: let RestWrapper/XStream know our aliases
    // ---------------------------------------------------------------------
    @Override
    public void configurePersister(XStreamPersister xp, XStreamMessageConverter converter) {
        super.configurePersister(xp, converter);
        configureAliases(xp);
    }

    private static void configureAliases(XStreamPersister persister) {
        XStream xs = persister.getXStream();

        xs.allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});

        // root
        xs.alias("filterchain", AuthFilterChainCollection.class);

        // collection: no <chains> wrapper; each item is <filters>
        xs.addImplicitCollection(AuthFilterChainCollection.class, "chains", "filters", AuthFilterChainFilters.class);

        // item
        xs.alias("filters", AuthFilterChainFilters.class);
        xs.aliasField("class", AuthFilterChainFilters.class, "clazz");
        xs.aliasAttribute(AuthFilterChainFilters.class, "requireSSL", "ssl");

        // Disable XStream's special meaning for the "class" attribute
        xs.aliasSystemAttribute(null, "class");
        xs.aliasSystemAttribute(null, "resolves-to"); // optional

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

        xs.alias("order", AuthFilterChainOrder.class);
        xs.addImplicitCollection(AuthFilterChainOrder.class, "order", "order", String.class);
    }

    // ---------------------------------------------------------------------
    // Endpoints
    // ---------------------------------------------------------------------

    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthFilterChainCollection> list() {
        checkAuthorised();
        try {
            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains = cfg.getFilterChain().getRequestChains();

            AuthFilterChainCollection col = new AuthFilterChainCollection();
            col.setChains(chains.stream().map(this::toDTO).collect(Collectors.toList()));

            return wrapObject(col, AuthFilterChainCollection.class);
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        } catch (Exception e) {
            throw new CannotSaveConfig(e);
        }
    }

    @PutMapping(
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthFilterChainCollection> replaceAll(HttpServletRequest request) {
        checkAuthorised();
        try {
            AuthFilterChainCollection incoming = parseCollection(request);

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
            return wrapObject(out, AuthFilterChainCollection.class);

        } catch (IllegalArgumentException ex) {
            throw new BadRequest(ex.getMessage());
        } catch (IOException ex) {
            throw new CannotReadConfig(ex);
        } catch (Exception ex) {
            throw new CannotSaveConfig(ex);
        }
    }

    // ---- Order -----------------------------------------------------------

    @PutMapping(
            path = {"/order", "/order.{ext}"},
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Void> reorder(jakarta.servlet.http.HttpServletRequest request) {
        checkAuthorised();
        try {
            List<String> wanted = parseOrderFromRequest(request);
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
                    wanted.stream().map(byName::get).toList();

            // IMPORTANT: persist in engine order (appears reversed when listed).
            List<RequestFilterChain> engineOrder = new ArrayList<>(reordered);
            cfg.setFilterChain(new GeoServerSecurityFilterChain(engineOrder));
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

    // ---- Per-chain -------------------------------------------------------

    @GetMapping(
            path = "/{chainName:^(?!order$).+}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthFilterChainFilters> getOneXml(@PathVariable String chainName) {
        String finalChainName = normalizeChainName(chainName);
        checkAuthorised();
        try {
            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            RequestFilterChain chain = Optional.ofNullable(cfg.getFilterChain().getRequestChainByName(finalChainName))
                    .orElseThrow(() -> new FilterChainNotFound(finalChainName));

            AuthFilterChainFilters dto = toDTO(chain);
            return wrapObject(dto, AuthFilterChainFilters.class);
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }
    }

    @PostMapping(
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestWrapper<AuthFilterChainFilters>> create(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position,
            UriComponentsBuilder builder) {

        checkAuthorised();
        try {
            AuthFilterChainFilters dto = parseFiltersFromRequest(request);
            ensureNotReserved(dto.getName());
            RequestFilterChain model = toModel(dto);

            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains =
                    new ArrayList<>(cfg.getFilterChain().getRequestChains());
            if (chains.stream().anyMatch(c -> Objects.equals(c.getName(), model.getName())))
                throw new DuplicateChainName(model.getName());

            int pos = (position != null) ? position : chains.size();
            checkArgument(pos >= 0 && pos <= chains.size(), "position out of range");
            chains.add(pos, model);

            cfg.setFilterChain(new GeoServerSecurityFilterChain(chains));
            securityManager.saveSecurityConfig(cfg);
            securityManager.reload();

            HttpHeaders headers = new HttpHeaders();
            UriComponentsBuilder ub = (builder != null ? builder : UriComponentsBuilder.newInstance());
            headers.setLocation(ub.path("/security/filterchain/{name}")
                    .buildAndExpand(model.getName())
                    .toUri());

            return new ResponseEntity<>(wrapObject(dto, AuthFilterChainFilters.class), headers, HttpStatus.CREATED);

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

    @PutMapping(
            path = CHAIN_PATH,
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public RestWrapper<AuthFilterChainFilters> update(
            @PathVariable String chainName,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(name = "position", required = false) Integer position) {

        chainName = normalizeChainName(chainName);
        checkAuthorised();
        try {
            AuthFilterChainFilters dto = parseFiltersFromRequest(request);
            ensureNotReserved(dto.getName());
            RequestFilterChain incoming = toModel(dto);

            if (!Objects.equals(chainName, incoming.getName()))
                throw new BadRequest("chainName must match payload name");

            SecurityManagerConfig cfg = securityManager.loadSecurityConfig();
            List<RequestFilterChain> chains =
                    new ArrayList<>(cfg.getFilterChain().getRequestChains());

            int existingIdx = indexOf(chains, chainName);
            if (existingIdx < 0) throw new FilterChainNotFound(chainName);

            chains.set(existingIdx, incoming);
            if (position != null && position >= 0 && position < chains.size() && position != existingIdx) {
                chains.remove(existingIdx);
                chains.add(position, incoming);
            }

            cfg.setFilterChain(new GeoServerSecurityFilterChain(chains));
            securityManager.saveSecurityConfig(cfg);
            securityManager.reload();

            return wrapObject(dto, AuthFilterChainFilters.class);

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
            if (e instanceof IOException || causedBy(e, IOException.class)) throw new CannotSaveConfig(e);
            throw new CannotSaveConfig(e);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static int indexOf(List<RequestFilterChain> chains, String name) {
        for (int i = 0; i < chains.size(); i++) {
            if (Objects.equals(chains.get(i).getName(), name)) return i;
        }
        return -1;
    }

    private static String normalizeChainName(String n) {
        return n == null ? null : n.replaceFirst("\\.(xml|json)$", "");
    }

    private static void ensureNotReserved(String name) {
        checkArgument(name != null && !name.isEmpty(), "name is required");
        checkArgument(!RESERVED.contains(name.toLowerCase()), "'%s' is reserved", name);
    }

    private AuthFilterChainFilters toDTO(RequestFilterChain c) {
        AuthFilterChainFilters dto = new AuthFilterChainFilters();
        dto.setName(c.getName());
        dto.setClazz(c.getClass().getName());
        List<String> patterns = Optional.ofNullable(c.getPatterns()).orElse(Collections.emptyList());
        dto.setPath(String.join(",", patterns));
        dto.setDisabled(c.isDisabled());
        dto.setAllowSessionCreation(c.isAllowSessionCreation());
        dto.setRequireSSL(c.isRequireSSL());
        dto.setMatchHTTPMethod(c.isMatchHTTPMethod());
        dto.setRoleFilterName(c.getRoleFilterName());
        dto.setFilters(new ArrayList<>(Optional.ofNullable(c.getFilterNames()).orElse(Collections.emptyList())));
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
            @SuppressWarnings("unchecked")
            Class<? extends RequestFilterChain> chainClass = (Class<? extends RequestFilterChain>) raw;
            RequestFilterChain chain = instantiateChain(chainClass, dto);

            chain.setName(dto.getName());
            chain.setPatterns(splitCSV(dto.getPath()));
            if (dto.getDisabled() != null) chain.setDisabled(dto.getDisabled());
            if (dto.getAllowSessionCreation() != null) chain.setAllowSessionCreation(dto.getAllowSessionCreation());
            if (dto.getRequireSSL() != null) chain.setRequireSSL(dto.getRequireSSL());
            if (dto.getMatchHTTPMethod() != null) chain.setMatchHTTPMethod(dto.getMatchHTTPMethod());
            if (dto.getRoleFilterName() != null) chain.setRoleFilterName(dto.getRoleFilterName());
            chain.setFilterNames(Optional.ofNullable(dto.getFilters()).orElse(Collections.emptyList()));
            invokeStringSetter(chain, "setInterceptorName", dto.getInterceptorName());
            invokeStringSetter(chain, "setExceptionTranslationName", dto.getExceptionTranslationName());
            return chain;

        } catch (IllegalArgumentException e) {
            throw e;
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
            return (val instanceof String s) ? s : null;
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

    // ---------- Body parsing helpers (minimal, robust to wrapped/bare JSON) ----------

    private AuthFilterChainFilters parseFiltersFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String ct = Optional.ofNullable(request.getContentType()).orElse("");
        try (ServletInputStream in = request.getInputStream()) {
            if (ct.contains("json")) {
                com.fasterxml.jackson.databind.JsonNode root = MAPPER.readTree(in);
                // Accept any of these shapes:
                //   { "filters": { "@name": ... } }
                //   { "@name": ... }                      (bare)
                //   { "filterchain": { "filters":[{...}] } }  (collection with one element)
                com.fasterxml.jackson.databind.JsonNode node = root;
                if (node.has("filters") && node.get("filters").isObject()) {
                    node = node.get("filters");
                } else if (node.has("filterchain")) {
                    JsonNode fc = node.get("filterchain");
                    if (fc != null
                            && fc.has("filters")
                            && fc.get("filters").isArray()
                            && fc.get("filters").size() == 1) {
                        node = fc.get("filters").get(0);
                    }
                }
                // Expect single chain object now
                if (!node.isObject()) throw new BadRequest("Malformed payload: expected a single filter chain object");

                // Map JSON shape (attributes use '@' and 'class' -> 'clazz', 'filter' -> 'filters')
                AuthFilterChainFilters dto = new AuthFilterChainFilters();
                dto.setName(getText(node, "@name", "name"));
                dto.setClazz(getText(node, "@class", "class", "clazz"));
                dto.setPath(getText(node, "@path", "path"));

                if (node.has("@disabled")) dto.setDisabled(node.get("@disabled").asBoolean());
                if (node.has("@allowSessionCreation"))
                    dto.setAllowSessionCreation(
                            node.get("@allowSessionCreation").asBoolean());
                if (node.has("@ssl")) dto.setRequireSSL(node.get("@ssl").asBoolean());
                if (node.has("@matchHTTPMethod"))
                    dto.setMatchHTTPMethod(node.get("@matchHTTPMethod").asBoolean());
                dto.setRoleFilterName(getText(node, "@roleFilterName", "roleFilterName"));
                dto.setInterceptorName(getText(node, "@interceptorName", "interceptorName"));
                dto.setExceptionTranslationName(getText(node, "@exceptionTranslationName", "exceptionTranslationName"));

                List<String> filters = new ArrayList<>();
                JsonNode f = node.get("filter");
                if (f != null) {
                    if (f.isArray()) f.forEach(n -> filters.add(n.asText()));
                    else filters.add(f.asText());
                }
                dto.setFilters(filters);
                return dto;
            } else {
                // XML via XStreamPersister with your aliases
                XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
                configureAliases(xp);
                // Accept either <filters> ... or <filterchain><filters>...</filters></filterchain> with one item
                Object o = xp.load(in, Object.class);
                if (o instanceof AuthFilterChainFilters filters) return filters;
                if (o instanceof AuthFilterChainCollection col) {
                    List<AuthFilterChainFilters> list =
                            Optional.ofNullable(col.getChains()).orElse(List.of());
                    if (list.size() == 1) return list.get(0);
                }
                throw new BadRequest("Malformed payload: expected a single <filters> element");
            }
        } catch (BadRequest e) {
            throw e;
        } catch (com.fasterxml.jackson.core.JsonParseException
                | com.fasterxml.jackson.databind.JsonMappingException e) {
            throw new BadRequest("Malformed payload: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }
    }

    private List<String> parseOrderFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String ct = Optional.ofNullable(request.getContentType()).orElse("");
        try (ServletInputStream in = request.getInputStream()) {
            if (ct.contains("json")) {
                JsonNode root = MAPPER.readTree(in);
                // Accept:
                //   { "order":[ ... ] }
                //   { "filterchain": { "order":[ ... ] } }
                com.fasterxml.jackson.databind.JsonNode n = root.has("order")
                        ? root.get("order")
                        : (root.has("filterchain") ? root.get("filterchain").get("order") : null);
                if (n == null || !n.isArray()) throw new BadRequest("`order` array required");
                List<String> out = new ArrayList<>();
                n.forEach(x -> out.add(x.asText()));
                return out;
            } else {
                // XML: <order><order>name</order>...</order>  OR  <filterchain><order>...</order></filterchain>
                XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
                configureAliases(xp);
                Object o = xp.load(in, Object.class);
                if (o instanceof AuthFilterChainOrder order) {
                    List<String> list = order.getOrder();
                    if (list == null || list.isEmpty()) throw new BadRequest("`order` array required");
                    return list;
                }
                throw new BadRequest("`order` array required");
            }
        } catch (BadRequest e) {
            throw e;
        } catch (IOException e) {
            throw new CannotReadConfig(e);
        }
    }

    private static String getText(com.fasterxml.jackson.databind.JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }

    private static byte[] readBody(HttpServletRequest req) throws IOException {
        try (jakarta.servlet.ServletInputStream in = req.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static boolean isXmlContent(HttpServletRequest req) {
        String ct = java.util.Optional.ofNullable(req.getContentType()).orElse("");
        return ct.contains(org.springframework.http.MediaType.APPLICATION_XML_VALUE)
                || ct.contains(org.springframework.http.MediaType.TEXT_XML_VALUE);
    }

    private AuthFilterChainCollection parseCollection(HttpServletRequest req) throws IOException {
        byte[] body = readBody(req);
        if (isXmlContent(req)) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp);
            return xp.load(new ByteArrayInputStream(body), AuthFilterChainCollection.class);
        } else {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(body);

            // Unwrap { "filterchain": { ... } } if present
            if (root.has("filterchain")) {
                root = root.get("filterchain");
            }

            // Accept either "filters": [ ... ]  OR "chains": [ ... ]  OR bare [ ... ]
            JsonNode array = null;
            if (root.isArray()) {
                array = root;
            } else if (root.has("filters") && root.get("filters").isArray()) {
                array = root.get("filters");
            } else if (root.has("chains") && root.get("chains").isArray()) {
                array = root.get("chains");
            } else if (root.has("filters") && root.get("filters").isObject()) {
                // Single object wrapped under "filters": turn it into a singleton array
                array = om.createArrayNode().add(root.get("filters"));
            } else if (root.isObject()) {
                // Possibly a single object: accept it as collection of size 1
                array = om.createArrayNode().add(root);
            }

            if (array == null || !array.isArray() || array.isEmpty()) {
                throw new BadRequest("Malformed payload: expected a non-empty array of filter chains");
            }

            List<AuthFilterChainFilters> chains = new ArrayList<>();
            for (JsonNode n : array) {
                chains.add(om.treeToValue(n, AuthFilterChainFilters.class));
            }

            AuthFilterChainCollection col = new AuthFilterChainCollection();
            col.setChains(chains);
            return col;
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
    // Exception handlers (scoped here; avoid leaking via @ControllerAdvice)

    @ExceptionHandler(CannotMakeChain.class)
    public ResponseEntity<ErrorResponse> handle(CannotMakeChain ex) {
        return new ResponseEntity<>(new ErrorResponse(500, ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CannotSaveConfig.class)
    public ResponseEntity<ErrorResponse> handle(CannotSaveConfig ex) {
        return new ResponseEntity<>(new ErrorResponse(500, ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CannotUpdateConfig.class)
    public ResponseEntity<ErrorResponse> handle(CannotUpdateConfig ex) {
        return new ResponseEntity<>(new ErrorResponse(500, ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CannotReadConfig.class)
    public ResponseEntity<ErrorResponse> handle(CannotReadConfig ex) {
        return new ResponseEntity<>(new ErrorResponse(500, ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<ErrorResponse> handle(BadRequest ex) {
        return new ResponseEntity<>(new ErrorResponse(400, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NothingToDelete.class)
    public ResponseEntity<ErrorResponse> handle(NothingToDelete ex) {
        return new ResponseEntity<>(new ErrorResponse(410, ex.getMessage()), HttpStatus.GONE);
    }

    @ExceptionHandler(DuplicateChainName.class)
    public ResponseEntity<ErrorResponse> handle(DuplicateChainName ex) {
        return new ResponseEntity<>(new ErrorResponse(400, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FilterChainNotFound.class)
    public ResponseEntity<ErrorResponse> handle(FilterChainNotFound ex) {
        return new ResponseEntity<>(new ErrorResponse(404, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NotAuthorised.class)
    public ResponseEntity<ErrorResponse> handle(NotAuthorised ex) {
        return new ResponseEntity<>(new ErrorResponse(403, ex.getMessage()), HttpStatus.FORBIDDEN);
    }

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

    private static boolean causedBy(Throwable t, Class<? extends Throwable> type) {
        while (t != null) {
            if (type.isInstance(t)) return true;
            t = t.getCause();
        }
        return false;
    }

    private void rethrowIfDomain(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof DuplicateChainName name) throw name;
            if (t instanceof NothingToDelete delete) throw delete;
            if (t instanceof BadRequest request) throw request;
            if (t instanceof CannotMakeChain chain) throw chain;
            if (t instanceof IllegalArgumentException) throw new BadRequest(t.getMessage());
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // Domain exceptions

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

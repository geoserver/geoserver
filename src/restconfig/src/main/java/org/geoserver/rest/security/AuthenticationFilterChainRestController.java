/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Strings;
import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.security.xml.AuthFilterChain;
import org.geoserver.rest.security.xml.FilterChainCollection;
import org.geoserver.rest.security.xml.FilterChainDTO;
import org.geoserver.rest.security.xml.FilterChainOrderDTO;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
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
        xs.alias("filterChain", org.geoserver.rest.security.xml.FilterChainCollection.class);

        // collection: no <chains> wrapper; each item is <filters>
        xs.addImplicitCollection(
                org.geoserver.rest.security.xml.FilterChainCollection.class,
                "chains",
                "filters",
                org.geoserver.rest.security.xml.FilterChainDTO.class);

        // item
        xs.alias("filters", org.geoserver.rest.security.xml.FilterChainDTO.class);
        xs.aliasField("class", org.geoserver.rest.security.xml.FilterChainDTO.class, "clazz");
        xs.aliasAttribute(org.geoserver.rest.security.xml.FilterChainDTO.class, "requireSSL", "ssl");
        // Disable XStream's special meaning for the "class" attribute
        xs.aliasSystemAttribute(null, "class");
        xs.aliasSystemAttribute(null, "resolves-to"); // (optional, belts & braces)

        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "name");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "clazz");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "path");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "disabled");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "allowSessionCreation");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "requireSSL");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "matchHTTPMethod");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "interceptorName");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "exceptionTranslationName");
        xs.useAttributeFor(org.geoserver.rest.security.xml.FilterChainDTO.class, "roleFilterName");

        xs.addImplicitCollection(
                org.geoserver.rest.security.xml.FilterChainDTO.class, "filters", "filter", String.class);
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

            FilterChainCollection col = new FilterChainCollection();
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

            FilterChainCollection incoming = xp.load(new ByteArrayInputStream(body), FilterChainCollection.class);

            List<FilterChainDTO> dtos =
                    Optional.ofNullable(incoming.getChains()).orElse(Collections.emptyList());
            checkArgument(!dtos.isEmpty(), "At least one chain must be provided");

            Set<String> names = new HashSet<>();
            for (FilterChainDTO dto : dtos) {
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

            FilterChainCollection out = new FilterChainCollection();
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
            FilterChainOrderDTO order = parseOrder(request);
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

            FilterChainDTO dto = toDTO(chain);

            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp); // already defines alias "filters" -> FilterChainDTO as element
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

            FilterChainDTO dto = xp.load(new ByteArrayInputStream(body), FilterChainDTO.class);
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

            FilterChainDTO dto = xp.load(new ByteArrayInputStream(body), FilterChainDTO.class);
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
            // >>> ADD THESE LINES FIRST <<<
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

    private FilterChainOrderDTO parseOrder(HttpServletRequest req) throws IOException {
        byte[] body = req.getInputStream().readAllBytes();
        String ct = Optional.ofNullable(req.getContentType()).orElse("");
        if (ct.contains(MediaType.APPLICATION_XML_VALUE) || ct.contains(MediaType.TEXT_XML_VALUE)) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            configureAliases(xp);
            return xp.load(new ByteArrayInputStream(body), FilterChainOrderDTO.class);
        } else {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, FilterChainOrderDTO.class);
        }
    }

    private FilterChainDTO toDTO(RequestFilterChain c) {
        FilterChainDTO dto = new FilterChainDTO();
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

    private RequestFilterChain toModel(FilterChainDTO dto) {
        try {
            Class<?> raw = Class.forName(dto.getClazz());
            checkArgument(
                    RequestFilterChain.class.isAssignableFrom(raw),
                    "Class %s is not a RequestFilterChain",
                    dto.getClazz());
            @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    private RequestFilterChain instantiateChain(Class<? extends RequestFilterChain> type, FilterChainDTO dto)
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

    private Object defaultArgFor(Class<?> t, FilterChainDTO dto) {
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

    private void deleteFilterChain(String chainName) {
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
            saveAndReturnAuthFilterChain(filterChain, config, chain.getRequestChains());
        } catch (IllegalArgumentException e) {
            throw new BadRequest(e.getMessage());
        } catch (IOException e) {
            throw new CannotUpdateConfig(e);
        }
    }

    private void updateFilterChain(String chainName, RequestFilterChain filterChain, int position)
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
            saveAndReturnAuthFilterChain(filterChain, config, updatedChains);
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
            throw new NotAuthorised();
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

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/crs")
/** REST endpoint to list supported CoordinateReferenceSystems codes and get CRS definition by code */
public class CRSController extends RestBaseController {

    static final int PAGE_LIMIT = Integer.getInteger("org.geoserver.rest.crs.page.limit", 200);

    private record CRSLink(String id, String href) {}

    private record PageInfo(int offset, int limit, int returned, int total) {}

    public record Codes(List<CRSLink> crs, PageInfo page) {}

    public record Authority(String name, String href) {}

    public record AuthoritiesResponse(List<Authority> authorities) {}

    public record DefinitionResponse(String id, String format, String definition) {}

    private static final Set<String> EXCLUDED_AUTHORITIES = Set.of(
            "http://www.opengis.net/gml",
            "http://www.opengis.net/def",
            "AUTO", // only suitable in WMS service
            "AUTO2", // only suitable in WMS service
            "urn:ogc:def",
            "urn:x-ogc:def");

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Codes list(
            @RequestParam(name = "authority", required = false) String authority,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        validatePaging(offset, limit);
        List<String> codes = new ArrayList<>(getCodes(authority));
        int total = codes.size();
        int from = Math.min(offset, total);
        int to = Math.min(offset + limit, total);

        String crslink = Objects.requireNonNull(RequestInfo.get()).servletURI("crs/");
        List<CRSLink> items = codes.subList(from, to).stream()
                .map(id -> new CRSLink(id, crslink + id + ".wkt"))
                .toList();

        return new Codes(items, new PageInfo(offset, limit, items.size(), total));
    }

    @GetMapping("/authorities")
    public AuthoritiesResponse listAuthorities() {
        List<String> authorities = getAuthorities(null);
        String urlPrefix = Objects.requireNonNull(RequestInfo.get()).servletURI("crs?authority=");
        List<Authority> items = authorities.stream()
                .map(authority -> new Authority(authority, urlPrefix + authority))
                .toList();
        return new AuthoritiesResponse(items);
    }

    @GetMapping(path = "/{identifier:.+}.wkt", produces = MediaType.TEXT_PLAIN_VALUE)
    /** Return the WKT of the specified identifier. Will return a 404 if not existing. */
    public ResponseEntity<String> getWkt(@PathVariable String identifier) {
        return ResponseEntity.ok(decodeWkt(identifier));
    }

    /** Return the definition (id, format and wkt) of the specified identifier. Will return a 404 if not existing. */
    @GetMapping(path = "/{identifier:.+}.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public DefinitionResponse getJson(@PathVariable String identifier) {
        return new DefinitionResponse(identifier, "wkt", decodeWkt(identifier));
    }

    private String decodeWkt(String identifier) {
        validateIdentifier(identifier);

        try {
            CoordinateReferenceSystem crs = CRS.decode(identifier, true);
            return crs.toWKT();
        } catch (FactoryException e) {
            throw new ResourceNotFoundException("CRS not found: " + identifier);
        }
    }

    private void validatePaging(int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (limit > PAGE_LIMIT) {
            throw new IllegalArgumentException("limit must be <= " + PAGE_LIMIT);
        }
    }

    private void validateIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("CRS identifier is required");
        }
        if (!identifier.contains(":")) {
            throw new IllegalArgumentException("Invalid CRS identifier, expected AUTHORITY:CODE");
        }
    }

    private Set<String> getCodes(String requestedAuthority) {
        List<String> authorities = getAuthorities(requestedAuthority);

        return authorities.stream()
                .flatMap(authority -> CRS.getSupportedCodes(authority).stream()
                        .filter(CRSController::filterCode)
                        .sorted(this::compareCodes)
                        .map(code -> mapCode(authority, code)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> getAuthorities(String requestedAuthority) {
        return CRS.getSupportedAuthorities(true).stream()
                .filter(a -> !EXCLUDED_AUTHORITIES.contains(a))
                .filter(a -> requestedAuthority == null
                        || requestedAuthority.isBlank()
                        || a.equalsIgnoreCase(requestedAuthority))
                .sorted()
                .toList();
    }

    private static String mapCode(String authority, String code) {
        return authority + ":" + code;
    }

    private static boolean filterCode(String code) {
        // this one shows up in every authority
        return !"WGS84(DD)".equals(code);
    }

    private int compareCodes(String a, String b) {
        try {
            return Integer.valueOf(a).compareTo(Integer.valueOf(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}

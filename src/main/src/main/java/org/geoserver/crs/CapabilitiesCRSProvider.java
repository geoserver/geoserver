/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.crs;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.geotools.referencing.CRS;

/**
 * Capabilities documents provider for CRS objects. Has some smarts to exclude specific authorities
 * that are not meant to be used in such context, like the alias ones for URN, HTTP and the like. It
 * also allows to customize how each code is formatted into the capabilities document, through a
 * mapper {@link BiFunction} receiving authority and code, and returning the code to be published.
 * Codes can also be filtered by setting a {@link BiFunction} that receives authority and code, and
 * returns whether that particular code should be included in the list, or not.
 */
public class CapabilitiesCRSProvider {

    private static Set<String> DEFAULT_AUTHORITY_EXCLUSIONS =
            Set.of(
                    "http://www.opengis.net/gml",
                    "http://www.opengis.net/def",
                    "AUTO", // only suitable in WMS service
                    "AUTO2", // only suitable in WMS service
                    "urn:ogc:def",
                    "urn:x-ogc:def");

    private BiFunction<String, String, String> codeMapper = CapabilitiesCRSProvider::mapCode;

    private Set<String> authorityExclusions = new HashSet<>(DEFAULT_AUTHORITY_EXCLUSIONS);

    private BiFunction<String, String, Boolean> codeFilter = CapabilitiesCRSProvider::filterCode;

    /**
     * Returns the set of authorities exclusions for this CRS provider. Can be manipulated to
     * add/remote authorities from the exclusion set.
     */
    public Set<String> getAuthorityExclusions() {
        return authorityExclusions;
    }

    public Set<String> getCodes() {
        List<String> authorities =
                CRS.getSupportedAuthorities(true).stream()
                        .filter(a -> !authorityExclusions.contains(a))
                        .sorted()
                        .collect(Collectors.toList());

        return authorities.stream()
                .flatMap(
                        authority ->
                                CRS.getSupportedCodes(authority).stream()
                                        .filter(code -> codeFilter.apply(authority, code))
                                        .sorted((a, b) -> compareCodes(a, b))
                                        .map(code -> codeMapper.apply(authority, code)))
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
    }

    private int compareCodes(String a, String b) {
        try {
            return Integer.valueOf(a).compareTo(Integer.valueOf(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    /**
     * Sets a bi-function that the provider uses to go from authority and code to the CRS identifier
     * published in the capabilities document.
     *
     * @param codeMapper
     */
    public void setCodeMapper(BiFunction<String, String, String> codeMapper) {
        this.codeMapper = codeMapper;
    }

    /**
     * Sets a bi-function that the provider uses to filter codes from the capabilities document. The
     * function receives authority and code, and will return true if the code should be included in
     * the output. By default, the provider will filter out the WGS84(DD) code.
     *
     * @param codeFilter
     */
    public void setCodeFilter(BiFunction<String, String, Boolean> codeFilter) {
        this.codeFilter = codeFilter;
    }

    private static String mapCode(String authority, String code) {
        return authority + ":" + code;
    }

    private static boolean filterCode(String authority, String code) {
        // this pest shows up in every authority
        return !"WGS84(DD)".equals(code);
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.roles;

import com.nimbusds.jose.JWSObject;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractor;

/**
 * Extracts roles from the header value (JWT and JSON). JSON -> use the rolesJsonPath to extract
 * either a string or list of strings from the json. JWT -> convert to a JSON claim-set, then use
 * the rolesJsonPath to extract either a string or list of strings from the json. Will also do
 * RoleConversion (cf RoleConverter).
 */
public class JwtHeadersRolesExtractor {

    GeoServerJwtHeadersFilterConfig jwtHeadersConfig;
    RoleConverter roleConverter;

    public JwtHeadersRolesExtractor(GeoServerJwtHeadersFilterConfig config) {
        jwtHeadersConfig = config;
        roleConverter = new RoleConverter(config);
    }

    /**
     * do actual conversion (JWT or JSON) given the value in the header.
     *
     * @param headerValue
     * @return
     */
    public Collection<GeoServerRole> getRoles(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        headerValue = headerValue.replaceFirst("^Bearer", "");
        headerValue = headerValue.replaceFirst("^bearer", "");
        headerValue = headerValue.trim();

        // JWT - convert JWT to JSON, then extract
        if (jwtHeadersConfig.getRoleSource()
                == GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT) {
            JWSObject jwsObject = null;
            try {
                jwsObject = JWSObject.parse(headerValue);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Map<String, Object> claims = jwsObject.getPayload().toJSONObject();
            List<String> roleNames =
                    asStringList(
                            JwtHeaderUserNameExtractor.getClaim(
                                    claims, jwtHeadersConfig.getRolesJsonPath()));
            List<GeoServerRole> roles = this.roleConverter.convert(roleNames);
            return roles;
        }

        // Simple JSON (extract by path)
        if (jwtHeadersConfig.getRoleSource()
                == GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JSON) {
            List<String> roleNames =
                    asStringList(
                            JwtHeaderUserNameExtractor.extractFromJSON(
                                    headerValue, jwtHeadersConfig.getRolesJsonPath()));
            List<GeoServerRole> roles = this.roleConverter.convert(roleNames);
            return roles;
        }
        return null;
    }

    /**
     * handles conversion of either a JSON string or list-of-string (JSONArray) for consistency.
     *
     * @param obj - json string or json list-of-string (JSONArray)
     * @return
     */
    public static List<String> asStringList(Object obj) {
        if (obj instanceof String) {
            return Arrays.asList((String) obj);
        }
        if (obj instanceof JSONArray) {
            return ((JSONArray) obj).stream().map(x -> x.toString()).collect(Collectors.toList());
        }
        if (obj instanceof List) {
            List<Object> list = ((List) obj);
            return list.stream().map(x -> x.toString()).collect(Collectors.toList());
        }
        return null;
    }
}

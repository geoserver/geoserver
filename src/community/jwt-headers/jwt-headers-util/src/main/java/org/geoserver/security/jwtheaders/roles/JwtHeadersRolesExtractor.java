/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.roles;

import com.nimbusds.jose.JWSObject;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractor;

/**
 * Extracts roles from the header value (JWT and JSON). JSON -> use the rolesJsonPath to extract either a string or list
 * of strings from the json. JWT -> convert to a JSON claim-set, then use the rolesJsonPath to extract either a string
 * or list of strings from the json. Will also do RoleConversion (cf RoleConverter).
 */
public class JwtHeadersRolesExtractor {

    JwtConfiguration jwtHeadersConfig;
    RoleConverter roleConverter;

    public JwtHeadersRolesExtractor(JwtConfiguration config) {
        jwtHeadersConfig = config;
        roleConverter = new RoleConverter(config);
    }

    /**
     * do actual conversion (JWT or JSON) given the value in the header.
     *
     * @param headerValue
     * @return
     */
    public Collection<String> getRoles(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        headerValue = headerValue.replaceFirst("^Bearer", "");
        headerValue = headerValue.replaceFirst("^bearer", "");
        headerValue = headerValue.trim();

        // JWT - convert JWT to JSON, then extract
        if (jwtHeadersConfig.getJwtHeaderRoleSource().equals(JwtConfiguration.JWTHeaderRoleSource.JWT.toString())) {
            JWSObject jwsObject = null;
            try {
                jwsObject = JWSObject.parse(headerValue);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Map<String, Object> claims = jwsObject.getPayload().toJSONObject();
            List<String> roleNames =
                    asStringList(JwtHeaderUserNameExtractor.getClaim(claims, jwtHeadersConfig.getRolesJsonPath()));
            List<String> roles = this.roleConverter.convert(roleNames);
            return roles;
        }

        // Simple JSON (extract by path)
        if (jwtHeadersConfig.getJwtHeaderRoleSource().equals(JwtConfiguration.JWTHeaderRoleSource.JSON.toString())) {
            List<String> roleNames = asStringList(
                    JwtHeaderUserNameExtractor.extractFromJSON(headerValue, jwtHeadersConfig.getRolesJsonPath()));
            List<String> roles = this.roleConverter.convert(roleNames);
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
    @SuppressWarnings("unchecked")
    public static List<String> asStringList(Object obj) {
        if (obj instanceof String string) {
            return List.of(string);
        }
        if (obj instanceof JSONArray array) {
            return array.stream().map(Object::toString).collect(Collectors.toList());
        }
        if (obj instanceof List list1) {
            return ((List<Object>) list1).stream().map(Object::toString).collect(Collectors.toList());
        }
        return null;
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.username;

import static org.geoserver.security.jwtheaders.JwtConfiguration.UserNameHeaderFormat.*;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSObject;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.security.jwtheaders.JwtConfiguration;

/** Extracts the username given the value of the request's header. */
public class JwtHeaderUserNameExtractor {

    JwtConfiguration jwtHeadersConfig;

    public JwtHeaderUserNameExtractor(JwtConfiguration config) {
        jwtHeadersConfig = config;
    }

    // get a claim from the JWT Payload - which is a Map.
    public static Object getClaim(Map<String, Object> map, String path) {
        return getClaim(map, new ArrayList<>(Arrays.asList(path.split("\\."))));
    }

    // recursive.
    // if this is trivial (single item in pathList), return the value.
    // otherwise, go into the map one level (pathList[0]) and recurse on the result.
    @SuppressWarnings("unchecked")
    private static Object getClaim(Map<String, Object> map, List<String> pathList) {
        if (map == null) {
            return null;
        }
        if (pathList.size() == 1) {
            return map.get(pathList.get(0));
        }

        String first = pathList.get(0);
        pathList.remove(0);

        return getClaim((Map<String, Object>) map.get(first), pathList);
    }

    // given json string + path into the json, extract the value.
    public static Object extractFromJSON(String json, String path) {
        try {
            return JsonPath.read(json, path);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * given a value of a request header, extract the username value. Handles: STRING (no conversion needed) JSON (use
     * userNameJsonPath to get the username from inside the JSON) JWT (decoded the JWT to a JSON claimset, then use
     * userNameJsonPath to get the username from inside the JSON)
     *
     * @param userNameHeader
     * @return
     */
    public String extractUserName(String userNameHeader) {
        if (userNameHeader == null) {
            return null;
        }

        // STRING - trivial case
        if (jwtHeadersConfig.getUserNameFormatChoice() == STRING) {
            String userName = userNameHeader.trim();
            if (userName.isBlank()) {
                return null;
            }
            return userName;
        }

        userNameHeader = userNameHeader.replaceFirst("^Bearer", "");
        userNameHeader = userNameHeader.replaceFirst("^bearer", "");
        userNameHeader = userNameHeader.trim();

        // JWT - convert JWT to JSON, then extract
        if (jwtHeadersConfig.getUserNameFormatChoice() == JWT) {
            JWSObject jwsObject = null;
            try {
                jwsObject = JWSObject.parse(userNameHeader);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Map<String, Object> claims = jwsObject.getPayload().toJSONObject();
            return (String) getClaim(claims, jwtHeadersConfig.getUserNameJsonPath());
        }

        // Simple JSON (extract by path)
        if (jwtHeadersConfig.getUserNameFormatChoice() == JSON) {
            return (String) extractFromJSON(userNameHeader, jwtHeadersConfig.getUserNameJsonPath());
        }

        return null; // unknown type
    }
}

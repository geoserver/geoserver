/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import static org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractor.extractFromJSON;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.jose.JWSObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;

/**
 * This validates the token against the OIDC userinfo_endpoint ("check token"). This allows an
 * official server to validate the token (not just us).
 *
 * <p>It also CAN verify that the userinfo and access token refer to the same user (SUBject). The
 * OIDC spec recommends this validation.
 *
 * <p>NOTE - we cache results for 1 hour for performance. TokenExpiryValidator will catch a token
 * expiring.
 */
public class TokenEndpointValidator {

    GeoServerJwtHeadersFilterConfig jwtHeadersConfig;

    public static Cache<Object, Object> validEndpoints =
            CacheBuilder.newBuilder()
                    .maximumSize(50000)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .build();

    public TokenEndpointValidator(GeoServerJwtHeadersFilterConfig config) {
        jwtHeadersConfig = config;
    }

    public void validate(String accessToken) throws Exception {
        if (!jwtHeadersConfig.isValidateTokenAgainstURL()) return; // nothing to do

        if (validEndpoints.getIfPresent(accessToken) != null)
            return; // we already know this is a good accessToken

        validateEndpoint(accessToken);

        // its good - put in cache, so we don't do the endpoint validation all the time.
        validEndpoints.put(accessToken, Boolean.TRUE);
    }

    public void validateEndpoint(String accessToken) throws Exception {
        URL url = new URL(jwtHeadersConfig.getValidateTokenAgainstURLEndpoint());
        String result = download(url, accessToken);
        if (result == null) throw new Exception("ValidateTokenAgainstURLEndpoint - failed");

        validateSubject(accessToken, result);
    }

    private void validateSubject(String accessToken, String result) throws Exception {
        if (!jwtHeadersConfig.isValidateSubjectWithEndpoint()) return; // nothing to do

        JWSObject jwsToken = JWSObject.parse(accessToken);
        String subject_userinfo = (String) extractFromJSON(result, "sub");
        String subject_accesstoken = (String) jwsToken.getPayload().toJSONObject().get("sub");
        if (subject_userinfo == null || subject_accesstoken == null)
            throw new Exception("couldn't extract subject from token or userinfo");
        if (!subject_userinfo.equals(subject_accesstoken))
            throw new Exception("subject of token and userinfo dont match");
    }

    public String download(URL url, String accessToken) throws IOException {
        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = httpConn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String data = readStream(httpConn.getInputStream());
            return data;
        }
        return null;
    }

    private String readStream(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)); ) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}

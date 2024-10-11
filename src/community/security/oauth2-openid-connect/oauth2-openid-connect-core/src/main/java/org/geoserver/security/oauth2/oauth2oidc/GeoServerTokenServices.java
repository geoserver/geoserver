/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.oauth2oidc;

import com.nimbusds.jose.JWSObject;
import java.text.ParseException;
import java.util.Map;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.security.oauth2.services.OpenIdConnectTokenServices;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * This is the same as OpenIdConnectTokenServices, except it adds
 * #loadAuthentication(OAuth2AccessToken accessToken). This is used by the
 * GeoserverAuthenticationProcessingFilter.
 *
 * <p>The difference is: OpenIdConnectTokenServices: User information (i.e. username) is taken from
 * the user-info (token validation) endpoint. GeoServerTokenServices: We combine the information in
 * the id_token with the user-info.
 *
 * <p>This is done because some systems have a `usable` username in the ID token and NOT in the
 * user-info.
 */
public class GeoServerTokenServices extends OpenIdConnectTokenServices {

    public GeoServerTokenServices() {}

    public OAuth2Authentication loadAuthentication(OAuth2AccessToken accessToken)
            throws AuthenticationException, InvalidTokenException {
        Map<String, Object> checkTokenResponse = checkToken(accessToken.getValue());

        verifyTokenResponse(accessToken.getValue(), checkTokenResponse);

        transformNonStandardValuesToStandardValues(checkTokenResponse);

        injectIDTokenClaims(checkTokenResponse, accessToken);

        return tokenConverter.extractAuthentication(checkTokenResponse);
    }

    private void injectIDTokenClaims(
            Map<String, Object> checkTokenResponse, OAuth2AccessToken accessToken) {
        if (!accessToken.getAdditionalInformation().containsKey("id_token")
                || (accessToken.getAdditionalInformation().get("id_token") == null)) {
            return;
        }
        var idTokenEncoded = (String) accessToken.getAdditionalInformation().get("id_token");
        try {
            var payload = JWSObject.parse(idTokenEncoded).getPayload().toJSONObject();
            for (var item : payload.entrySet()) {
                if (checkTokenResponse.containsKey(item.getKey())) continue;
                checkTokenResponse.put(item.getKey(), item.getValue());
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        return;
    }

    public void setConfiguration(OpenIdConnectFilterConfig config) {
        super.setConfiguration(config);
    }
}

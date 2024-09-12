/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.pkce;

import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.ClientSecretRequestEnhancer;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.util.MultiValueMap;

/** Used to enhance Token Requests with previously generated code_verifier. */
public class PKCERequestEnhancer implements RequestEnhancer {

    private final StringKeyGenerator secureKeyGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    private static final Logger LOGGER = Logging.getLogger(PKCERequestEnhancer.class);
    private final OpenIdConnectFilterConfig config;

    public PKCERequestEnhancer(OpenIdConnectFilterConfig oidcConfig) {
        this.config = oidcConfig;
    }

    @Override
    public void enhance(
            AccessTokenRequest request,
            OAuth2ProtectedResourceDetails resource,
            MultiValueMap<String, String> form,
            HttpHeaders headers) {

        if (config.isSendClientSecret()) {
            form.put(
                    ClientSecretRequestEnhancer.CLIENT_SECRET,
                    Arrays.asList(resource.getClientSecret()));
            if (config.isAllowUnSecureLogging()) {
                LOGGER.fine("CLIENT_SECRET: " + resource.getClientSecret());
            }
        }
        if (config.isUsePKCE()) {
            var codeVerifier = request.get(PkceParameterNames.CODE_VERIFIER);
            if (codeVerifier != null) {
                form.put(PkceParameterNames.CODE_VERIFIER, codeVerifier);
                if (config.isAllowUnSecureLogging()) {
                    LOGGER.fine("CODE_VERIFIER: " + codeVerifier.get(0));
                }
            }
        }
    }
}

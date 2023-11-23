/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.pkce;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Proof Key of Code Exchange (PKCE) adding {@code code_challenge} hash to authentication redirect.
 *
 * <p>The original {@code code_verifier} is stored in the session for later confirmation.
 */
public class PKCEAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOGGER = Logging.getLogger(PKCEAuthenticationEntryPoint.class);

    /** Session key to store {@code code_verifier} */
    public static String OIDC_CODE_VERIFIER = "OIDC_CODE_VERIFIER";

    private final OpenIdConnectFilterConfig config;

    public PKCEAuthenticationEntryPoint(OpenIdConnectFilterConfig config) {
        this.config = config;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        final StringBuilder loginUri = config.buildAuthorizationUrl();

        if (config.getEnableRedirectAuthenticationEntryPoint()
                || request.getRequestURI().endsWith(config.getLoginEndpoint())) {

            StringKeyGenerator secureKeyGenerator =
                    new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);
            String codeVerifier = secureKeyGenerator.generateKey();
            HttpSession session = request.getSession();
            if (config.isAllowUnSecureLogging())
                LOGGER.fine("Generate code_verifier: " + codeVerifier);
            if (session != null) {
                session.setAttribute(OIDC_CODE_VERIFIER, codeVerifier);
            }
            try {
                String codeChallenge = createHash(codeVerifier);
                if (config.isAllowUnSecureLogging()) {
                    LOGGER.fine("CODE_CHALLENGE: " + codeChallenge);
                    LOGGER.fine("CODE_CHALLENGE_METHOD: S256");
                }
                loginUri.append("&")
                        .append(PkceParameterNames.CODE_CHALLENGE)
                        .append("=")
                        .append(codeChallenge);
                loginUri.append("&")
                        .append(PkceParameterNames.CODE_CHALLENGE_METHOD)
                        .append("=")
                        .append("S256");
            } catch (NoSuchAlgorithmException e) {
                if (config.isAllowUnSecureLogging()) LOGGER.fine("CODE_CHALLENGE: " + codeVerifier);
                loginUri.append("&")
                        .append(PkceParameterNames.CODE_CHALLENGE)
                        .append("=")
                        .append(codeVerifier);
            }

            response.sendRedirect(loginUri.toString());
        }
    }

    /**
     * Create SHA-256 hash of value, used to encode codeVerifier.
     *
     * @param value
     * @return SHA-256 has of value
     * @throws NoSuchAlgorithmException
     */
    private static String createHash(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}

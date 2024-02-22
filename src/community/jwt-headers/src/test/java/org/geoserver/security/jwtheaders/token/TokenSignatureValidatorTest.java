/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import static org.geoserver.security.jwtheaders.token.TokenExpiryValidatorTest.getNotExpiredToken;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;

import com.nimbusds.jose.jwk.JWKSet;
import java.text.ParseException;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** tests TokenSignatureValidator calss */
public class TokenSignatureValidatorTest {

    /**
     * tests a good signature
     *
     * @throws Exception
     */
    @Test
    public void testGoodSignature() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenSignature(true);
        config.setValidateTokenSignatureURL("myurl");

        TokenValidator validator = new TokenValidator(config);

        validator.tokenSignatureValidator = spy(validator.tokenSignatureValidator);

        MockedStatic<TokenSignatureValidator> mocked =
                Mockito.mockStatic(TokenSignatureValidator.class, Mockito.CALLS_REAL_METHODS);

        mocked.when(() -> TokenSignatureValidator.loadJWKSet(anyString())).thenReturn(getJWKSet());

        String token = accessToken;
        try {
            validator.validate(token);
        } finally {
            mocked.close();
        }
    }

    /**
     * tests a bad signature.
     *
     * <p>We modify an existing access token (cf TokenExpiryValidatorTest) and don't update the
     * signature. The signature check should fail (throw).
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testBadSignature() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setValidateToken(true);
        config.setValidateTokenSignature(true);
        config.setValidateTokenSignatureURL("myurl");

        TokenValidator validator = new TokenValidator(config);

        validator.tokenSignatureValidator = spy(validator.tokenSignatureValidator);

        MockedStatic<TokenSignatureValidator> mocked =
                Mockito.mockStatic(TokenSignatureValidator.class, Mockito.CALLS_REAL_METHODS);

        mocked.when(() -> TokenSignatureValidator.loadJWKSet(anyString())).thenReturn(getJWKSet());

        String token = getNotExpiredToken(accessToken);
        try {
            validator.validate(token);
        } finally {
            mocked.close();
        }
    }

    String accessToken =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItb0QyZXphcjF3ZHBUUmZCS0NqMFY4cm5ZVkJGQmxJLW5ldzFEREJCNTJrIn0.eyJleHAiOjE3MDg1NDgwOTYsImlhdCI6MTcwODU0Nzc5NiwiYXV0aF90aW1lIjoxNzA4NTQ3Nzk2LCJqdGkiOiI4YmU0ZjEzYy1lYTc5LTQ5YTYtOTU3Yy00MzEwMTViYTA3ZmQiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0Ojc3NzcvcmVhbG1zL2RhdmUtdGVzdDIiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiOThjZmUwNjAtZjk4MC00YTA1LTg2MTItNmM2MDkyMTlmZmU5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibGl2ZS1rZXkyIiwibm9uY2UiOiJNSm83NzRQRHU0Q2hSaVFHTmJWcHFZcTcwN21pMnZudDZ6dHJBX1RZZ3NvIiwic2Vzc2lvbl9zdGF0ZSI6IjQwNTExNzdhLTg4MjEtNGJmMi05MWEyLWI5NThlNTA4ZTJkMiIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1kYXZlLXRlc3QyIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImxpdmUta2V5MiI6eyJyb2xlcyI6WyJHZW9uZXR3b3JrQWRtaW5pc3RyYXRvciIsIkdlb3NlcnZlckFkbWluaXN0cmF0b3IiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHBob25lIG9mZmxpbmVfYWNjZXNzIG1pY3JvcHJvZmlsZS1qd3QgcHJvZmlsZSBhZGRyZXNzIGVtYWlsIiwic2lkIjoiNDA1MTE3N2EtODgyMS00YmYyLTkxYTItYjk1OGU1MDhlMmQyIiwidXBuIjoiZGF2aWQuYmxhc2J5QGdlb2NhdC5uZXQiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImFkZHJlc3MiOnt9LCJuYW1lIjoiZGF2aWQgYmxhc2J5IiwiZ3JvdXBzIjpbImRlZmF1bHQtcm9sZXMtZGF2ZS10ZXN0MiIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXSwicHJlZmVycmVkX3VzZXJuYW1lIjoiZGF2aWQuYmxhc2J5QGdlb2NhdC5uZXQiLCJnaXZlbl9uYW1lIjoiZGF2aWQiLCJmYW1pbHlfbmFtZSI6ImJsYXNieSIsImVtYWlsIjoiZGF2aWQuYmxhc2J5QGdlb2NhdC5uZXQifQ.qRqmeV-iu4wvY2YK_gB-EnfBQiyXPg3WkfNJBurMtM9GG0LtvQGztIKTxhRGH91VLG6DGR1t1aHo1NIkb5RkJj9wmv7TX5Eyemu8KtkQ-a5ZWnpxcfGWPZQ5iKa_9aob_wY2Z5PXSLaxCGcaZPcC-nWxENeXokQWw63Y-B3PY8_ODV10OMyHQoLKfzoxZwZdh2WJsGhI2d_Ia5Sp4V0Hd03-19g5aksyO4a_M99ScHsMw0qPMJwMS5yPmFMxsvlAk36RdX-4Ru4nS_Vl76gPvbDKilqcmC7N3ODWWjrDwtqIvsfCZ-Msll4m73Qy-XM4X3kOYrpRP9PDTF6xjqZmMQ";

    String jwkset =
            "{\"keys\":[{\"kid\":\"-oD2ezar1wdpTRfBKCj0V8rnYVBFBlI-new1DDBB52k\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"vGDQI9y9ivho67l73doYHvMTVdori6DqlGUTfDQbmCKGN5qtsDr-17M-g8PIgnpSmFJzLd7VkF9_kqUImzh9O_QBndwAwNt9VNL5NIhrYzqJfncvywPD6aImg85RzfujrRczq5HhAorEUjEqZCPZ7XNgTcQ8s0_-gPPvVArAHyOUD2F6rj_M6MLS6jyi3HWdlcNKQOb-3a_VBtDaCOJcwGrxu-RVgIUREY_Ymbi_S-F5Zl4qet6PIERGiWhPpVdzCi491o0vOaoYTKliwR3B0K39IuPUNwhyD_LbdbUwjpL5cUFzyTpdaFBwWio_ds1Q_syGWf4NjlD8cptk15HZZQ\",\"e\":\"AQAB\",\"x5c\":[\"MIICozCCAYsCBgGNngEiKzANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApkYXZlLXRlc3QyMB4XDTI0MDIxMjE1NDYzMFoXDTM0MDIxMjE1NDgxMFowFTETMBEGA1UEAwwKZGF2ZS10ZXN0MjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALxg0CPcvYr4aOu5e93aGB7zE1XaK4ug6pRlE3w0G5gihjearbA6/tezPoPDyIJ6UphScy3e1ZBff5KlCJs4fTv0AZ3cAMDbfVTS+TSIa2M6iX53L8sDw+miJoPOUc37o60XM6uR4QKKxFIxKmQj2e1zYE3EPLNP/oDz71QKwB8jlA9heq4/zOjC0uo8otx1nZXDSkDm/t2v1QbQ2gjiXMBq8bvkVYCFERGP2Jm4v0vheWZeKnrejyBERoloT6VXcwouPdaNLzmqGEypYsEdwdCt/SLj1DcIcg/y23W1MI6S+XFBc8k6XWhQcFoqP3bNUP7Mhln+DY5Q/HKbZNeR2WUCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAi/9ygiQzEOKMaeBqubsQvdY2HX2FDda903KII0lmI+xx6FRbLx6XAR2Y1nkvoXIN3rdQ82QgOl12FxCDP6f0Rx8JyovqYs2Okocdh8O4txq+eBIDnCRYg188ApcINflo5MRtdgQeOCpTl3T5siGuzCkwZwguz3PMmdCxtnaPKpUkev+/MQHzfTPOIq+H5YN4Qzx+239J32JtB1l4EpDepZ0B1MFyR4NtPdQTZSwv4kCdzHcf7wWqjEzKolPQKObSV44r/6gKYC0p2i7oNTOBAcdknlj/RCnLVPfd9Ezdlzrv4DPlokMDQL8CyTLkqyoLqJRXJucNglFfhENqUDxT1A==\"],\"x5t\":\"ey0zf4HBdrCE5E7n5NE0cYAiMqE\",\"x5t#S256\":\"ELydiVBoUfckES_Q-QVzVRc2S0iDXLsHXrHGW5iJTBc\"},{\"kid\":\"FmPsEbsilhjFclDsgyEtzjHzICdcQV83OAf4n3Pdgvc\",\"kty\":\"RSA\",\"alg\":\"RSA-OAEP\",\"use\":\"enc\",\"n\":\"0-cEytoDW41T7UqPBdxIYpBGMwBUch0UXqvFI8wveDYp2il-XjuJIImWuqAJB9BxZkUPuUzH93oY486H6ZR90JcRJx9C4oMemuku-pVzuB1rO7FgrVfKOe1KoJARcpvXLI92XORynp9-Ildw7yIa9XaI-Fy-mPIfwq5M0Q2AGNVnaXu0TVNLPI9Tb1jwQo5SWx37kC-zYnq51dey1KAya8-5GF0Q6CJDvzCZ9UPfj37rU9rj2QwvRRlDYGMTH-lZaPryE5UQDiSUpWsrY9w-BOv3QksvCf_DOJ7Lx3j5g3v0ITplXu3lAOtuWs0TgBnjZKBi1Bkni6MJqGLZGhJDZQ\",\"e\":\"AQAB\",\"x5c\":[\"MIICozCCAYsCBgGNngEi+DANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApkYXZlLXRlc3QyMB4XDTI0MDIxMjE1NDYzMVoXDTM0MDIxMjE1NDgxMVowFTETMBEGA1UEAwwKZGF2ZS10ZXN0MjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANPnBMraA1uNU+1KjwXcSGKQRjMAVHIdFF6rxSPML3g2Kdopfl47iSCJlrqgCQfQcWZFD7lMx/d6GOPOh+mUfdCXEScfQuKDHprpLvqVc7gdazuxYK1XyjntSqCQEXKb1yyPdlzkcp6ffiJXcO8iGvV2iPhcvpjyH8KuTNENgBjVZ2l7tE1TSzyPU29Y8EKOUlsd+5Avs2J6udXXstSgMmvPuRhdEOgiQ78wmfVD349+61Pa49kML0UZQ2BjEx/pWWj68hOVEA4klKVrK2PcPgTr90JLLwn/wziey8d4+YN79CE6ZV7t5QDrblrNE4AZ42SgYtQZJ4ujCahi2RoSQ2UCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAkFI6dLASOnuD8zTx1JvXDvQvlCWkvEhQCMXbaBSoQlBHfbT+qgNmF8Yge5CWBED7pDN1w4SMjoKGWh++sXIvk53a1cFPzwRG0c+/XFkWv6scj7BYdp/tW7gnipCFWvrrC2VOQ3L7Va6qYlIHSEuN01IaVvDM7KUBbwCokjEbqPgP6aRfltYoDUS/cMhixYA5BN1721eNLRJCJsognyoO7tpn5TUbMgo5I0wxeZ3J0ZyDY+MdK9zoylC44FEkwWXgZODkfb+rzNdTuqXuos36xDsRHgmG5hk+go2diejYbrsAdf4Gk2cqKqDAjpKucQfEiq2tULDMOegkqCN5CDkUaA==\"],\"x5t\":\"06stWmvLczzaaBWnLYWiarf9PCc\",\"x5t#S256\":\"d0wADfnb_ZLNFeoV8nEmn_WFrA-hqWLVcHsV7WRpZSQ\"}]}";

    public JWKSet getJWKSet() throws ParseException {
        return JWKSet.parse(jwkset);
    }
}

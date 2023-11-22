package org.geoserver.security.oauth2.pkce;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.geoserver.security.oauth2.services.TokenSignatureValidator;
import org.geotools.util.Base64;
import org.junit.Test;
import org.locationtech.jts.util.Assert;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class TokenSignatureValidatorTest {


    @Test
    public void testLoadKeys() throws  Exception {
        var tokenValidator = new TokenSignatureValidator();

        //id or access token
        //it has 3 parts - header, body, signature
        // header - has the keyid for the public key in the JWKSet
        // body - Map of the claims
        // footer - Signature
        String tokenBase64 ="eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJVX1U3eXUxTjh1Sk9XRVg0UWhUM3FhekNjTm5HTldYUzNrZlJKLVRkeThNIn0.eyJleHAiOjE3MDA2NzYyODcsImlhdCI6MTcwMDYxNjI4NywiYXV0aF90aW1lIjoxNzAwNjE1MzM3LCJqdGkiOiI1ZGI2OWViNy0yMGFkLTQ5M2ItYjE2YS1mNjEyYzRiNjA5N2MiLCJpc3MiOiJodHRwczovL2xvZ2luLWxpdmUtZGV2Lmdlb2NhdC5saXZlL3JlYWxtcy9kYXZlLXJlYWxtIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjU3NDJhOGY2LTM5NzItNGQ2YS05OGYzLTFjZmU2MDc1ZjE0ZSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImxpdmUta2V5Iiwic2Vzc2lvbl9zdGF0ZSI6IjJkOWNlMDBjLWQ4ZTAtNDk0Ny1iODc4LTk2ZDlhZDYwYWU0OCIsImFjciI6IjAiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWRhdmUtcmVhbG0iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImxpdmUta2V5Ijp7InJvbGVzIjpbIkdlb25ldHdvcmtBZG1pbmlzdHJhdG9yIiwiR2Vvc2VydmVyQWRtaW5pc3RyYXRvciJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBwaG9uZSBvZmZsaW5lX2FjY2VzcyBtaWNyb3Byb2ZpbGUtand0IGFkZHJlc3MiLCJzaWQiOiIyZDljZTAwYy1kOGUwLTQ5NDctYjg3OC05NmQ5YWQ2MGFlNDgiLCJ1cG4iOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiYWRkcmVzcyI6e30sIm5hbWUiOiJkYXZpZCBibGFzYnkiLCJncm91cHMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWRhdmUtcmVhbG0iLCJ1bWFfYXV0aG9yaXphdGlvbiJdLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImdpdmVuX25hbWUiOiJkYXZpZCIsImZhbWlseV9uYW1lIjoiYmxhc2J5IiwiZW1haWwiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCJ9.G7Ku40V9B1YcesTFbf9BeTkHlfpnSOmADByQ7AHMamV49K2yFzbRECSsTFH1Ijv_6lofWMnRWM8LUCQQH3HA2V6Y9WiYdfAiV52K9WQr38MDNIZXmpopCVZ9ML21EQnhojsbrDW5JkSQPtwnvwMW7OxFECQo8L4_eU8w6ShWVNwEP0JGPHPI3XCMOn-5Cicj6CHacMvhh1iaufGMOd7Dm8IMNZCtlanqoGLy3N3272n8SuydwN6uL0oD8pauYryY2VfCmSbciLNKy-B7NCkdxtF99vzV7J4y3yAac87V3tZmbO47x-X4DClhxlJ-Pr3p3R3VFW6xhCTd7UgOITVacQ";

        var token = tokenValidator.ParseToken(tokenBase64);
        var keyId = token.getHeader().getKeyID();

        JWKSet publicKeys = JWKSet.load(new URL("https://login-live-dev.geocat.live/realms/dave-realm/protocol/openid-connect/certs"));
        var jwk = publicKeys.getKeyByKeyId(keyId);

        // could be a different type of key, but this should be true in most cases
        Assert.isTrue(jwk instanceof  RSAKey);
        var rsa = (RSAKey) jwk;
        tokenValidator.ValidateSignature(rsa,token);
    }

    @Test
    public void testGoodSignature() throws  Exception {

        var tokenValidator = new TokenSignatureValidator();

        String tokenBase64 ="eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJVX1U3eXUxTjh1Sk9XRVg0UWhUM3FhekNjTm5HTldYUzNrZlJKLVRkeThNIn0.eyJleHAiOjE3MDA2NzYyODcsImlhdCI6MTcwMDYxNjI4NywiYXV0aF90aW1lIjoxNzAwNjE1MzM3LCJqdGkiOiI1ZGI2OWViNy0yMGFkLTQ5M2ItYjE2YS1mNjEyYzRiNjA5N2MiLCJpc3MiOiJodHRwczovL2xvZ2luLWxpdmUtZGV2Lmdlb2NhdC5saXZlL3JlYWxtcy9kYXZlLXJlYWxtIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjU3NDJhOGY2LTM5NzItNGQ2YS05OGYzLTFjZmU2MDc1ZjE0ZSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImxpdmUta2V5Iiwic2Vzc2lvbl9zdGF0ZSI6IjJkOWNlMDBjLWQ4ZTAtNDk0Ny1iODc4LTk2ZDlhZDYwYWU0OCIsImFjciI6IjAiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWRhdmUtcmVhbG0iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImxpdmUta2V5Ijp7InJvbGVzIjpbIkdlb25ldHdvcmtBZG1pbmlzdHJhdG9yIiwiR2Vvc2VydmVyQWRtaW5pc3RyYXRvciJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBwaG9uZSBvZmZsaW5lX2FjY2VzcyBtaWNyb3Byb2ZpbGUtand0IGFkZHJlc3MiLCJzaWQiOiIyZDljZTAwYy1kOGUwLTQ5NDctYjg3OC05NmQ5YWQ2MGFlNDgiLCJ1cG4iOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiYWRkcmVzcyI6e30sIm5hbWUiOiJkYXZpZCBibGFzYnkiLCJncm91cHMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWRhdmUtcmVhbG0iLCJ1bWFfYXV0aG9yaXphdGlvbiJdLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImdpdmVuX25hbWUiOiJkYXZpZCIsImZhbWlseV9uYW1lIjoiYmxhc2J5IiwiZW1haWwiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCJ9.G7Ku40V9B1YcesTFbf9BeTkHlfpnSOmADByQ7AHMamV49K2yFzbRECSsTFH1Ijv_6lofWMnRWM8LUCQQH3HA2V6Y9WiYdfAiV52K9WQr38MDNIZXmpopCVZ9ML21EQnhojsbrDW5JkSQPtwnvwMW7OxFECQo8L4_eU8w6ShWVNwEP0JGPHPI3XCMOn-5Cicj6CHacMvhh1iaufGMOd7Dm8IMNZCtlanqoGLy3N3272n8SuydwN6uL0oD8pauYryY2VfCmSbciLNKy-B7NCkdxtF99vzV7J4y3yAac87V3tZmbO47x-X4DClhxlJ-Pr3p3R3VFW6xhCTd7UgOITVacQ";

        var token = tokenValidator.ParseToken(tokenBase64);
        var keyId = token.getHeader().getKeyID();

        Assert.equals("U_U7yu1N8uJOWEX4QhT3qazCcNnGNWXS3kfRJ-Tdy8M", keyId);

        var publicKey = tokenValidator.CreatePublicKey(
                "pdZfJzYixFM2378U-Tdwm_sLljO1tcSmxRKTZ7utmwBf7zYNMHCA41qsXhyjDdYQzXkkFMvW7gt66Wu-FCyjcThNmUXnoMYaaaC6vQM5xcgZriL6mkDAO1n5LD1chE6uVMOYKuP29LiYIWFy3xOIwPUzqewDCH-9W0IM_tLd-aX6rTidPqVMzKZxLsOVV0kcTQudv0DUiQ0R_6xnovvBdaAgoNm-2QjCBfMBXMEaESQPyRy65cXyQ7DCFSLSbpzJuBSJCVJI7gbuHgwq1pkiFo-dlmwssw9V3_8JdOhqZATK1yjjfyWgm56YtzbPrt5Mz4W1xTygfkMMpOr_SjFXxQ",
                "AQAB"
        );

        tokenValidator.ValidateSignature(publicKey,token);
    }

}

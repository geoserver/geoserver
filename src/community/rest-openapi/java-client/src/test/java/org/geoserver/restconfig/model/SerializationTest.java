package org.geoserver.restconfig.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.codec.Charsets;
import org.geoserver.openapi.model.catalog.EnvelopeInfo;
import org.geoserver.openapi.v1.model.EnvelopeResponse;
import org.geoserver.restconfig.client.GeoServerClient;
import org.hamcrest.core.StringStartsWith;
import org.junit.Before;

public abstract class SerializationTest {

    protected ObjectMapper objectMapper;
    protected FormEncoder encoder;
    protected JacksonDecoder decoder;

    public final @Before void setUpDecoder() {
        // mimic ApiClient's initialization of feign encoder/decoder
        objectMapper = GeoServerClient.newApiClient().getObjectMapper();
        encoder = new FormEncoder(new JacksonEncoder(objectMapper));
        decoder = new JacksonDecoder(objectMapper);
    }

    protected <T> T decodeResource(String resource, Type type) throws IOException {
        return decode(resource, getClass(), type);
    }

    protected <T> T decode(String resource, Class<?> baseClass, Type type) throws IOException {
        String raw;
        try (InputStream in = baseClass.getResourceAsStream(resource)) {
            assertNotNull("resource not found: " + resource, in);
            raw =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining(System.lineSeparator()));
        }
        return decode(raw, type);
    }

    protected <T> T decode(String raw, Type type) throws IOException {
        @SuppressWarnings("deprecation")
        Request request =
                Request.create("GET", "http://doesntmatter", Collections.emptyMap(), null, null);
        Response response = Response.builder().request(request).body(raw, Charsets.UTF_8).build();
        assertNotNull(response);
        @SuppressWarnings("unchecked")
        T decoded = (T) decoder.decode(response, type);
        return decoded;
    }

    @SuppressWarnings("rawtypes")
    protected void assertResponseCRS(Object responseCRS, String atClass, String srs) {
        assertTrue(String.valueOf(responseCRS), responseCRS instanceof java.util.Map);
        assertEquals("projected", ((Map) responseCRS).get("@class"));
        assertThat((String) ((Map) responseCRS).get("$"), StringStartsWith.startsWith(srs));
    }

    protected void assertResponseBounds(
            EnvelopeResponse env, double minx, double maxx, double miny, double maxy) {
        assertEquals(Double.valueOf(minx), env.getMinx());
        assertEquals(Double.valueOf(maxx), env.getMaxx());
        assertEquals(Double.valueOf(miny), env.getMiny());
        assertEquals(Double.valueOf(maxy), env.getMaxy());
    }

    protected void assertResponseBounds(
            EnvelopeInfo env, double minx, double maxx, double miny, double maxy, String srs) {
        assertEquals(Double.valueOf(minx), env.getMinx());
        assertEquals(Double.valueOf(maxx), env.getMaxx());
        assertEquals(Double.valueOf(miny), env.getMiny());
        assertEquals(Double.valueOf(maxy), env.getMaxy());
        assertThat(env.getCrs(), StringStartsWith.startsWith(srs));
    }
}

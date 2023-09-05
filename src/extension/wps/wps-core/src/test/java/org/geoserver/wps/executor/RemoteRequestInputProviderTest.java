/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.forbidden;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.base.MoreObjects.firstNonNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.opengis.wps10.MethodType.GET_LITERAL;
import static net.opengis.wps10.MethodType.POST_LITERAL;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ssl.SSLContextBuilder;
import com.github.tomakehurst.wiremock.http.ssl.TrustEverythingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.opengis.wps10.BodyReferenceType;
import net.opengis.wps10.HeaderType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import net.opengis.wps10.Wps10Factory;
import org.apache.commons.io.IOUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.geoserver.ows.Ows11Util;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geotools.api.util.ProgressListener;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class RemoteRequestInputProviderTest {

    @ClassRule @Rule
    public static WireMockRule service =
            new WireMockRule(WireMockConfiguration.options().dynamicPort().dynamicHttpsPort());

    private ProgressListener listener;

    @BeforeClass
    public static void setSocketFactory() throws Exception {
        // create a socket factory that trusts all certificates
        RemoteRequestInputProvider.setSocketFactory(
                new SSLConnectionSocketFactory(
                        SSLContextBuilder.create()
                                .loadTrustMaterial(null, new TrustEverythingStrategy())
                                .build(),
                        NoopHostnameVerifier.INSTANCE));
    }

    @Before
    public void setListener() {
        listener = mock(ProgressListener.class);
    }

    @AfterClass
    public static void clearSocketFactory() {
        RemoteRequestInputProvider.setSocketFactory(null);
    }

    @Test
    public void testNullResult() throws Exception {
        InputType input = input(file("test.txt"), null, null, null);
        ComplexPPIO ppio =
                new ComplexPPIO(null, null, null) {
                    @Override
                    public Object decode(InputStream input) throws Exception {
                        return null;
                    }

                    @Override
                    public void encode(Object value, OutputStream os) {}
                };
        assertNull(getValue(input, ppio, 0, 0));
    }

    @Test
    public void testIncorrectResultType() throws Exception {
        InputType input = input(file("test.txt"), null, null, null);
        String message = "Failed to retrieve value for input testInput";
        assertException(
                input, ppio(Number.class, null), 0, 0, message, IllegalArgumentException.class);
    }

    @Test
    public void testFileUrlValidWithoutMaxSize() throws Exception {
        InputType input = input(file("test.txt"), null, null, null);
        assertValue(input, ppio(), 0, 0, "TEST");
    }

    @Test
    public void testFileUrlValidWithMaxSize() throws Exception {
        InputType input = input(file("test.txt"), null, null, null);
        assertValue(input, ppio(), 0, 100, "TEST");
    }

    @Test
    public void testFileUrlInvalidTooBig() throws Exception {
        InputType input = input(file("test.txt"), null, null, null);
        String message = "Input testInput size 4 exceeds maximum allowed size of 1";
        assertException(input, ppio(), 0, 1, message, null);
    }

    @Test
    public void testFileUrlInvalidMissingFile() throws Exception {
        URL url = new URL(file("test.txt").toString().replace("test.txt", "missing.txt"));
        InputType input = input(url, null, null, null);
        String message = "Failed to retrieve value for input testInput";
        assertException(input, ppio(), 0, 0, message, IOException.class);
    }

    @Test
    public void testFileUrlInvalidCanceled() throws Exception {
        when(listener.isCanceled()).thenReturn(false, true);
        InputType input = input(file("test.txt"), null, null, null);
        String message = "Failed to retrieve value for input testInput";
        assertException(input, ppio(), 0, 0, message, ProcessDismissedException.class);
    }

    @Test
    public void testOtherUrlValidWithoutMaxSize() throws Exception {
        InputType input = input(jar("test"), null, null, null);
        assertValue(input, ppio(), 0, 0, "TEST");
    }

    @Test
    public void testOtherUrlValidWithMaxSize() throws Exception {
        InputType input = input(jar("test"), null, null, null);
        assertValue(input, ppio(), 0, 100, "TEST");
    }

    @Test
    public void testOtherUrlInvalidTooBig() throws Exception {
        InputType input = input(jar("test"), null, null, null);
        String message = "Exceeded maximum input size of 1 bytes while reading input testInput";
        assertException(input, ppio(), 0, 1, message, null);
    }

    @Test
    public void testOtherUrlInvalidMissingFile() throws Exception {
        URL url = new URL(jar("test").toString().replace("test.zip", "missing.zip"));
        InputType input = input(url, null, null, null);
        String message = "Failed to retrieve value for input testInput";
        assertException(input, ppio(), 0, 0, message, IOException.class);
    }

    @Test
    public void testOtherUrlInvalidCanceled() throws Exception {
        when(listener.isCanceled()).thenReturn(false, true);
        InputType input = input(jar("test"), null, null, null);
        String message = "Failed to retrieve value for input testInput";
        assertException(input, ppio(), 0, 0, message, ProcessDismissedException.class);
    }

    @Test
    public void testHttpNoMethodInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(http("test"), null, null);
    }

    @Test
    public void testHttpGetInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(http("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpPostBodyInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(http("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpPostRefFileInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(http("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpPostRefOtherInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(http("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpPostRefHttpInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(http("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpPostRefHttpsInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(http("test"), POST_LITERAL, https("foo"));
    }

    @Test
    public void testHttpsNoMethodInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(https("test"), null, null);
    }

    @Test
    public void testHttpsGetInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(https("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpsPostBodyInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(https("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpsPostRefFileInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(https("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpsPostRefOtherInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(https("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpsPostRefHttpInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(https("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpsPostRefHttpsInvalidBadStatus() throws Exception {
        doTestHttpInvalidBadStatus(https("test"), POST_LITERAL, https("foo"));
    }

    private void doTestHttpInvalidBadStatus(URL url, MethodType method, URL bodyUrl)
            throws Exception {
        String auth = "Basic YWxhZGRpbjpvcGVuc2VzYW1l";
        stub(forbidden(), bodyUrl, ok("FOO"));
        InputType input = input(url, method, "FOO", bodyUrl, AUTHORIZATION, auth);
        assertException(
                input,
                ppio(),
                0,
                0,
                "Failed to retrieve value for input testInput",
                IllegalStateException.class,
                request(input).withHeader(AUTHORIZATION, equalTo(auth)),
                bodyRequest(input));
    }

    @Test
    public void testHttpNoMethodValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(http("test"), null, null);
    }

    @Test
    public void testHttpGetValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(http("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpPostBodyValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(http("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpPostRefFileValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(http("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpPostRefOtherValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(http("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpPostRefHttpValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(http("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpPostRefHttpsValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(http("test"), POST_LITERAL, https("foo"));
    }

    @Test
    public void testHttpsNoMethodValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(https("test"), null, null);
    }

    @Test
    public void testHttpsGetValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(https("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpsPostBodyValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(https("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpsPostRefFileValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(https("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpsPostRefOtherValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(https("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpsPostRefHttpValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(https("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpsPostRefHttpsValidWithoutMaxSize() throws Exception {
        doTestHttpValidWithoutMaxSize(https("test"), POST_LITERAL, https("foo"));
    }

    private void doTestHttpValidWithoutMaxSize(URL url, MethodType method, URL bodyUrl)
            throws Exception {
        stub(ok("TEST"), bodyUrl, ok("FOO"));
        InputType input = input(url, method, "FOO", bodyUrl);
        assertValue(input, ppio(), 0, 0, "TEST", request(input), bodyRequest(input));
    }

    @Test
    public void testHttpNoMethodValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(http("test"), null, null);
    }

    @Test
    public void testHttpGetValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(http("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpPostBodyValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(http("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpPostRefFileValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(http("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpPostRefOtherValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(http("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpPostRefHttpValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(http("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpPostRefHttpsValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(http("test"), POST_LITERAL, https("foo"));
    }

    @Test
    public void testHttpsNoMethodValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(https("test"), null, null);
    }

    @Test
    public void testHttpsGetValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(https("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpsPostBodyValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(https("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpsPostRefFileValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(https("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpsPostRefOtherValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(https("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpsPostRefHttpValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(https("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpsPostRefHttpsValidWithoutContentLength() throws Exception {
        doTestHttpValidWithoutContentLength(https("test"), POST_LITERAL, https("foo"));
    }

    private void doTestHttpValidWithoutContentLength(URL url, MethodType method, URL bodyUrl)
            throws Exception {
        stub(ok("TEST"), bodyUrl, ok("FOO"));
        InputType input = input(url, method, "FOO", bodyUrl);
        assertValue(input, ppio(), 0, 100, "TEST", request(input), bodyRequest(input));
    }

    @Test
    public void testHttpNoMethodValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(http("test"), null, null);
    }

    @Test
    public void testHttpGetValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(http("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpPostBodyValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(http("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpPostRefFileValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(http("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpPostRefOtherValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(http("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpPostRefHttpValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(http("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpPostRefHttpsValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(http("test"), POST_LITERAL, https("foo"));
    }

    @Test
    public void testHttpsNoMethodValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(https("test"), null, null);
    }

    @Test
    public void testHttpsGetValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(https("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpsPostBodyValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(https("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpsPostRefFileValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(https("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpsPostRefOtherValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(https("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpsPostRefHttpValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(https("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpsPostRefHttpsValidWithMaxSizeAndContentLength() throws Exception {
        doTestHttpValidWithMaxSizeAndContentLength(https("test"), POST_LITERAL, https("foo"));
    }

    private void doTestHttpValidWithMaxSizeAndContentLength(URL url, MethodType method, URL bodyUrl)
            throws Exception {
        stub(ok("TEST").withHeader(CONTENT_LENGTH, "4"), bodyUrl, ok("FOO"));
        InputType input = input(url, method, "FOO", bodyUrl);
        assertValue(input, ppio(), 0, 100, "TEST", request(input), bodyRequest(input));
    }

    @Test
    public void testHttpNoMethodInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(http("test"), null, null);
    }

    @Test
    public void testHttpGetValidInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(http("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpPostBodyInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(http("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpPostRefFileInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(http("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpPostRefOtherInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(http("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpPostRefHttpInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(http("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpPostRefHttpsInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(http("test"), POST_LITERAL, https("foo"));
    }

    @Test
    public void testHttpsNoMethodInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(https("test"), null, null);
    }

    @Test
    public void testHttpsGetValidInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(https("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpsPostBodyInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(https("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpsPostRefFileInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(https("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpsPostRefOtherInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(https("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpsPostRefHttpInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(https("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpsPostRefHttpsInvalidTooBigWithoutContentLength() throws Exception {
        doTestHttpInvalidTooBigWithoutContentLength(https("test"), POST_LITERAL, https("foo"));
    }

    private void doTestHttpInvalidTooBigWithoutContentLength(
            URL url, MethodType method, URL bodyUrl) throws Exception {
        stub(ok("TEST"), bodyUrl, ok("FOO"));
        InputType input = input(url, method, "FOO", bodyUrl);
        String message = "Exceeded maximum input size of 3 bytes while reading input testInput";
        assertException(input, ppio(), 0, 3, message, null, request(input), bodyRequest(input));
    }

    @Test
    public void testHttpNoMethodInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(http("test"), null, null);
    }

    @Test
    public void testHttpGetValidInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(http("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpPostBodyInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(http("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpPostRefFileInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(http("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpPostRefOtherInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(http("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpPostRefHttpInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(http("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpPostRefHttpsInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(http("test"), POST_LITERAL, https("foo"));
    }

    @Test
    public void testHttpsNoMethodInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(https("test"), null, null);
    }

    @Test
    public void testHttpsGetValidInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(https("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpsPostBodyInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(https("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpsPostRefFileInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(https("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpsPostRefOtherInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(https("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpsPostRefHttpInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(https("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpsPostRefHttpsInvalidTooBigWithContentLength() throws Exception {
        doTestHttpInvalidTooBigWithContentLength(https("test"), POST_LITERAL, https("foo"));
    }

    private void doTestHttpInvalidTooBigWithContentLength(URL url, MethodType method, URL bodyUrl)
            throws Exception {
        stub(ok("TEST").withHeader(CONTENT_LENGTH, "4"), bodyUrl, ok("FOO"));
        InputType input = input(url, method, "FOO", bodyUrl);
        String message =
                "Input testInput size 4 exceeds maximum allowed size of 3 "
                        + "according to HTTP Content-Length response header";
        assertException(input, ppio(), 0, 3, message, null, request(input), bodyRequest(input));
    }

    @Test
    public void testHttpNoMethodInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(http("test"), null, null);
    }

    @Test
    public void testHttpGetValidInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(http("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpPostBodyInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(http("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpPostRefFileInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(http("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpPostRefOtherInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(http("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpPostRefHttpInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(http("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpPostRefHttpsInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(http("test"), POST_LITERAL, https("foo"));
    }

    @Test
    public void testHttpsNoMethodInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(https("test"), null, null);
    }

    @Test
    public void testHttpsGetValidInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(https("test"), GET_LITERAL, null);
    }

    @Test
    public void testHttpsPostBodyInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(https("test"), POST_LITERAL, null);
    }

    @Test
    public void testHttpsPostRefFileInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(https("test"), POST_LITERAL, file("foo.txt"));
    }

    @Test
    public void testHttpsPostRefOtherInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(https("test"), POST_LITERAL, jar("foo"));
    }

    @Test
    public void testHttpsPostRefHttpInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(https("test"), POST_LITERAL, http("foo"));
    }

    @Test
    public void testHttpsPostRefHttpsInvalidCanceled() throws Exception {
        doTestHttpInvalidCanceled(https("test"), POST_LITERAL, https("foo"));
    }

    private void doTestHttpInvalidCanceled(URL url, MethodType method, URL bodyUrl)
            throws Exception {
        when(listener.isCanceled()).thenReturn(false, true);
        stub(ok("TEST"), bodyUrl, ok("FOO"));
        InputType input = input(url, method, "FOO", bodyUrl);
        // these tests throw the exception from the body reference requests
        assertException(
                input,
                ppio(),
                0,
                3,
                "Failed to retrieve value for input testInput",
                ProcessDismissedException.class,
                bodyUrl == null ? request(input) : null,
                bodyRequest(input));
    }

    @Test
    public void testHttpPostInvalidNullBodyAndBodyRef() throws Exception {
        doTestHttpPostInvalidNullBodyAndBodyRef(http("test"));
    }

    @Test
    public void testHttpsPostInvalidNullBodyAndBodyRef() throws Exception {
        doTestHttpPostInvalidNullBodyAndBodyRef(https("test"));
    }

    private void doTestHttpPostInvalidNullBodyAndBodyRef(URL url) throws Exception {
        InputType input = input(url, POST_LITERAL, null, null);
        input.getReference().setBodyReference(null);
        String message = "A POST request should contain a non empty body";
        assertException(input, ppio(), 0, 0, message, null);
    }

    @Test
    public void testHttpPostInvalidNullBodyAndBodyRefHref() throws Exception {
        doTestHttpPostInvalidNullBodyAndBodyRefHref(http("test"));
    }

    @Test
    public void testHttpsPostInvalidNullBodyAndBodyRefHref() throws Exception {
        doTestHttpPostInvalidNullBodyAndBodyRefHref(https("test"));
    }

    private void doTestHttpPostInvalidNullBodyAndBodyRefHref(URL url) throws Exception {
        InputType input = input(url, POST_LITERAL, null, null);
        String message = "A POST request should contain a non empty body";
        assertException(input, ppio(), 0, 0, message, null);
    }

    @Test
    public void testHttpPostInvalidBodyType() throws Exception {
        doTestHttpPostInvalidBodyType(http("test"));
    }

    @Test
    public void testHttpsPostInvalidBodyType() throws Exception {
        doTestHttpPostInvalidBodyType(https("test"));
    }

    private void doTestHttpPostInvalidBodyType(URL url) throws Exception {
        InputType input = input(url, POST_LITERAL, 1, null);
        String message =
                "The request body should be contained in a CDATA section, otherwise "
                        + "it will get parsed as XML instead of being preserved as is";
        assertException(input, ppio(), 0, 0, message, null);
    }

    private void assertException(
            InputType input,
            ComplexPPIO ppio,
            int timeout,
            long maxSize,
            String message,
            Class<? extends Exception> hidden,
            RequestPatternBuilder... requests)
            throws Exception {
        WPSException e =
                assertThrows(WPSException.class, () -> getValue(input, ppio, timeout, maxSize));
        assertEquals(message, e.getMessage());
        assertEquals("NoApplicableCode", e.getCode());
        assertEquals("testInput", e.getLocator());
        assertNull(e.getCause());
        // verify that the hidden exception was of the proper type if one was expected
        // or that no hidden exception was thrown if none was expected
        verify(listener).exceptionOccurred(any(firstNonNull(hidden, Exception.class)));
        verify(listener).exceptionOccurred(any(WPSException.class));
        Arrays.stream(requests).filter(Objects::nonNull).forEach(WireMock::verify);
    }

    private void assertValue(
            InputType input,
            ComplexPPIO ppio,
            int timeout,
            long maxSize,
            Object expected,
            RequestPatternBuilder... requests)
            throws Exception {
        assertEquals(expected, getValue(input, ppio, timeout, maxSize));
        Arrays.stream(requests).filter(Objects::nonNull).forEach(WireMock::verify);
    }

    private Object getValue(InputType input, ComplexPPIO ppio, int timeout, long maxSize)
            throws Exception {
        Map<String, InputProvider> map = new HashMap<>();
        map.put("testInput", new RemoteRequestInputProvider(input, ppio, timeout, maxSize));
        LazyInputMap inputs = new LazyInputMap(map);
        inputs.setListener(listener);
        return inputs.get("testInput");
    }

    private URL file(String file) {
        return getClass().getResource(file);
    }

    private URL jar(String name) throws Exception {
        // the unit tests use jar:file: URLs to test other URL protocols
        return new URL("jar:" + file(name + ".zip") + "!/" + name + ".txt");
    }

    private static URL http(String name) throws Exception {
        return new URL("http://localhost:" + service.port() + "/" + name);
    }

    private static URL https(String name) throws Exception {
        return new URL("https://localhost:" + service.httpsPort() + "/" + name);
    }

    private static void stub(
            ResponseDefinitionBuilder response,
            URL bodyUrl,
            ResponseDefinitionBuilder bodyResponse) {
        // stub for the main reference HTTP(S) URL
        stubFor(any(urlEqualTo("/test")).willReturn(response));
        if (bodyUrl != null && bodyUrl.getProtocol().startsWith("http")) {
            // stub for the body reference HTTP(S) URL
            stubFor(get(urlEqualTo("/foo")).willReturn(bodyResponse));
        }
    }

    private static RequestPatternBuilder request(InputType input) {
        if (input.getReference().getMethod() != POST_LITERAL) {
            return getRequestedFor(urlEqualTo("/test"));
        } else {
            String mimeType = "text/plain";
            if (input.getReference().getBodyReference().getHref() == null) {
                // the charset is only set when using Body instead of BodyReference
                mimeType += "; charset=UTF-8";
            }
            return postRequestedFor(urlEqualTo("/test"))
                    .withRequestBody(equalTo("FOO"))
                    .withHeader(CONTENT_TYPE, equalTo(mimeType));
        }
    }

    private static RequestPatternBuilder bodyRequest(InputType input) {
        String bodyHref = input.getReference().getBodyReference().getHref();
        if (bodyHref != null && bodyHref.startsWith("http")) {
            return getRequestedFor(urlEqualTo("/foo"));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static InputType input(
            URL href, MethodType method, Object body, URL bodyHref, String... headers) {
        Wps10Factory factory = Wps10Factory.eINSTANCE;
        InputType input = factory.createInputType();
        input.setIdentifier(Ows11Util.code("testInput"));
        InputReferenceType ref = factory.createInputReferenceType();
        ref.setHref(href.toString());
        ref.setMethod(method);
        BodyReferenceType bodyRef = factory.createBodyReferenceType();
        if (bodyHref != null) {
            bodyRef.setHref(bodyHref.toString());
        } else {
            ref.setBody(body);
        }
        ref.setBodyReference(bodyRef);
        for (int i = 1; i < headers.length; i += 2) {
            HeaderType header = factory.createHeaderType();
            header.setKey(headers[i - 1]);
            header.setValue(headers[i]);
            ref.getHeader().add(header);
        }
        input.setReference(ref);
        return input;
    }

    private static ComplexPPIO ppio() {
        return ppio(String.class, "text/plain");
    }

    private static ComplexPPIO ppio(Class<?> type, String mimeType) {
        return new ComplexPPIO(type, type, mimeType) {
            @Override
            public Object decode(InputStream input) throws Exception {
                return IOUtils.toString(input, UTF_8);
            }

            @Override
            public void encode(Object value, OutputStream os) {}
        };
    }
}

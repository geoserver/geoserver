/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import net.opengis.wps10.HeaderType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.ows.URLCheckers;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;

/**
 * Handles an internal reference to a remote location
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RemoteRequestInputProvider extends AbstractInputProvider {

    static final Logger LOGGER = Logging.getLogger(RemoteRequestInputProvider.class);

    // only used for unit tests

    private int timeout;

    private ComplexPPIO complexPPIO;

    private long maxSize;

    private final HostnameVerifier hostnameVerifier;

    // Existing ctor now delegates and keeps STRICT hostname verification by default
    public RemoteRequestInputProvider(InputType input, ComplexPPIO ppio, int timeout, long maxSize) {
        this(input, ppio, timeout, maxSize, HttpsSupport.getDefaultHostnameVerifier());
    }

    // Convenience toggle: pass true to disable hostname verification (NoopHostnameVerifier)
    public RemoteRequestInputProvider(
            InputType input, ComplexPPIO ppio, int timeout, long maxSize, boolean disableHostnameVerification) {
        this(
                input,
                ppio,
                timeout,
                maxSize,
                disableHostnameVerification
                        ? NoopHostnameVerifier.INSTANCE
                        : HttpsSupport.getDefaultHostnameVerifier());
    }

    // Full-control ctor: inject any TlsHostnameVerifier you want
    public RemoteRequestInputProvider(
            InputType input, ComplexPPIO ppio, int timeout, long maxSize, HostnameVerifier hostnameVerifier) {
        super(input, ppio);
        this.timeout = timeout;
        this.complexPPIO = ppio;
        this.maxSize = maxSize;
        this.hostnameVerifier = hostnameVerifier;

        // check we are allowed to access a remote resource
        String location = input.getReference().getHref();
        URLCheckers.confirm(location);
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        InputReferenceType ref = input.getReference();

        // execute the request
        listener.started();
        String location = ref.getHref();
        try (CloseableHttpClient client = buildHttpClient(location);
                ClassicHttpResponse response = mainHttpRequest(client, ref, listener);
                InputStream is = getInputStream(response, location, listener)) {

            // actually parse the data
            Object result = complexPPIO.decode(is);
            if (result != null && !complexPPIO.getType().isInstance(result)) {
                // Some text parsers return a Map when it cannot be converted to the proper type.
                // Detect those errors here rather than later.
                throw new IllegalArgumentException(
                        "Decoded result is not a " + complexPPIO.getType().getName() + ", got a: "
                                + result.getClass().getName());
            }
            return result;
        } catch (WPSException e) {
            throw e;
        } catch (Exception e) {
            // Log the exception and replace with a generic exception to prevent
            // potentially disclosing sensitive information from a remote resource.
            listener.exceptionOccurred(e);
            String message = "Failed to retrieve value for input " + getInputId();
            LOGGER.log(Level.WARNING, message, e);
            throw new WPSException(message, "NoApplicableCode", getInputId());
        } finally {
            listener.progress(100);
            listener.complete();
        }
    }

    private CloseableHttpClient buildHttpClient(String href) throws IOException {
        if (!isHttpURL(href)) {
            // only build a client if the href is an HTTP(S) URL
            return null;
        }

        try {
            // Move connect/socket timeouts to ConnectionConfig
            ConnectionConfig connConfig = ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(timeout))
                    .setSocketTimeout(Timeout.ofMilliseconds(timeout))
                    .build();

            // Keep per-request response timeout on RequestConfig
            RequestConfig reqConfig = RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(timeout))
                    .build();

            LOGGER.log(
                    Level.SEVERE,
                    "Building HTTP client that accepts all SSL certificates for WPS RemoteInput, verify this is ok");
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();

            DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext, this.hostnameVerifier);

            PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(connConfig)
                    .setTlsSocketStrategy(tlsStrategy)
                    .build();

            return HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(reqConfig)
                    .disableAutomaticRetries()
                    .disableRedirectHandling()
                    .setUserAgent("GeoServer WPS RemoteInput")
                    .useSystemProperties()
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException(e);
        }
    }

    private static boolean isHttpURL(String href) throws IOException {
        if (href == null) {
            return false;
        }
        String protocol = new URL(href).getProtocol();
        return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
    }

    private ClassicHttpResponse mainHttpRequest(
            CloseableHttpClient client, InputReferenceType ref, ProgressListener listener) throws IOException {
        if (client == null) {
            // return null if not an HTTP(S) request
            return null;
        }
        String bodyHref = getBodyReferenceHref(ref);
        try (CloseableHttpClient bodyClient = buildHttpClient(bodyHref);
                ClassicHttpResponse bodyResponse = bodyHttpRequest(bodyClient, bodyHref);
                InputStream bodyInput = getInputStream(bodyResponse, bodyHref, listener)) {

            ClassicRequestBuilder request;
            // prepare either a GET or a POST request
            if (ref.getMethod() != MethodType.POST_LITERAL) {
                request = ClassicRequestBuilder.get(ref.getHref());
            } else {
                request = ClassicRequestBuilder.post(ref.getHref());
                ContentType contentType = ContentType.create(complexPPIO.getMimeType());
                if (bodyInput != null) {
                    request.setEntity(new InputStreamEntity(bodyInput, contentType));
                } else {
                    String charset = firstNonNull(ref.getEncoding(), "UTF-8");
                    contentType = contentType.withCharset(charset);
                    request.setEntity(new StringEntity((String) ref.getBody(), contentType));
                }
            }
            // add eventual extra headers
            for (Object o : ref.getHeader()) {
                HeaderType header = (HeaderType) o;
                request.setHeader(header.getKey(), header.getValue());
            }
            // it is safe to close bodyClient, bodyResponse and bodyInput after this finishes
            return client.executeOpen(null, request.build(), HttpClientContext.create());
        }
    }

    private String getBodyReferenceHref(InputReferenceType ref) {
        if (ref.getMethod() != MethodType.POST_LITERAL || ref.getBody() instanceof String) {
            // only use the BodyReference for POST requests without a Body
            return null;
        } else if (ref.getBody() != null) {
            throw new WPSException(
                    "The request body should be contained in a CDATA section, otherwise "
                            + "it will get parsed as XML instead of being preserved as is",
                    "NoApplicableCode",
                    getInputId());
        } else if (ref.getBodyReference() == null || ref.getBodyReference().getHref() == null) {
            throw new WPSException("A POST request should contain a non empty body", "NoApplicableCode", getInputId());
        }
        String bodyReferenceHref = ref.getBodyReference().getHref();
        URLCheckers.confirm(bodyReferenceHref);
        return bodyReferenceHref;
    }

    private static ClassicHttpResponse bodyHttpRequest(CloseableHttpClient client, String href) throws IOException {
        // return null if not an HTTP(S) request; otherwise execute a GET request
        return isHttpURL(href)
                ? client.executeOpen(null, ClassicRequestBuilder.get(href).build(), HttpClientContext.create())
                : null;
    }

    private InputStream getInputStream(ClassicHttpResponse response, String href, ProgressListener listener)
            throws IOException {
        if (href == null) {
            // no URL to open a stream from
            return null;
        }
        URL url = new URL(href);
        InputStream is;
        if (response != null) {
            // get the stream from the HTTP response
            is = getResponseStream(response, href);
        } else if ("file".equalsIgnoreCase(url.getProtocol())) {
            is = openFileURL(url);
        } else {
            is = openOtherURL(url);
        }
        if (maxSize > 0) {
            is = new MaxSizeInputStream(is, getInputId(), maxSize);
        }
        return new CancellingInputStream(is, listener);
    }

    private InputStream getResponseStream(ClassicHttpResponse response, String href) throws IOException {
        int code = response.getCode();
        if (code != 200) {
            // do not expose the status code to users
            throw new IllegalStateException("Error getting remote resources from "
                    + href
                    + ", http error "
                    + code
                    + ": "
                    + response.getReasonPhrase());
        }
        try {
            Header length = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
            if (maxSize > 0 && length != null && maxSize < Long.parseLong(length.getValue())) {
                throw new WPSException(
                        "Input "
                                + getInputId()
                                + " size "
                                + length.getValue()
                                + " exceeds maximum allowed size of "
                                + maxSize
                                + " according to HTTP Content-Length response header",
                        "NoApplicableCode",
                        getInputId());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(
                    Level.FINE,
                    "Failed to parse content length to check input limits respect, "
                            + "moving on and checking data as it comes in",
                    e);
        }
        return response.getEntity().getContent();
    }

    private InputStream openFileURL(URL url) throws IOException {
        File file = URLs.urlToFile(url);
        if (maxSize > 0 && maxSize < file.length()) {
            throw new WPSException(
                    "Input " + getInputId() + " size " + file.length() + " exceeds maximum allowed size of " + maxSize,
                    "NoApplicableCode",
                    getInputId());
        }
        return FileUtils.openInputStream(file);
    }

    private InputStream openOtherURL(URL url) throws IOException {
        // open with the built-in url management
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        return conn.getInputStream();
    }

    @Override
    public int longStepCount() {
        return 1;
    }
}

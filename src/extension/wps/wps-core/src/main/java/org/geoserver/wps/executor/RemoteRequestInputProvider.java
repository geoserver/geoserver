/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wps10.HeaderType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geotools.data.ows.URLCheckers;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Handles an internal reference to a remote location
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RemoteRequestInputProvider extends AbstractInputProvider {

    static final Logger LOGGER = Logging.getLogger(RemoteRequestInputProvider.class);

    // only used for unit tests
    private static LayeredConnectionSocketFactory socketFactory;

    private int timeout;

    private ComplexPPIO complexPPIO;

    private long maxSize;

    public RemoteRequestInputProvider(
            InputType input, ComplexPPIO ppio, int timeout, long maxSize) {
        super(input, ppio);
        this.timeout = timeout;
        this.complexPPIO = ppio;
        this.maxSize = maxSize;

        // check we are allowed to access a remote resource
        String location = input.getReference().getHref();
        URLCheckers.confirm(location);
    }

    @VisibleForTesting
    protected static void setSocketFactory(LayeredConnectionSocketFactory newSocketFactory) {
        socketFactory = newSocketFactory;
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        InputReferenceType ref = input.getReference();

        // execute the request
        listener.started();
        String location = ref.getHref();
        try (CloseableHttpClient client = buildHttpClient(location);
                CloseableHttpResponse response = mainHttpRequest(client, ref, listener);
                InputStream is = getInputStream(response, location, listener)) {
            // actually parse the data
            Object result = complexPPIO.decode(is);
            if (result != null && !complexPPIO.getType().isInstance(result)) {
                // Some text parsers return a Map when it can not be converted to
                // the proper type.  Detect those errors here rather than later.
                throw new IllegalArgumentException(
                        "Decoded result is not a "
                                + complexPPIO.getType().getName()
                                + ", got a: "
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
        // build a new client with the configured timeout
        RequestConfig config =
                RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();
        return HttpClients.custom()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .setDefaultRequestConfig(config)
                .setSSLSocketFactory(socketFactory)
                .setUserAgent("GeoServer WPS RemoteInput")
                .useSystemProperties()
                .build();
    }

    private static boolean isHttpURL(String href) throws IOException {
        if (href == null) {
            return false;
        }
        String protocol = new URL(href).getProtocol();
        return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
    }

    private CloseableHttpResponse mainHttpRequest(
            CloseableHttpClient client, InputReferenceType ref, ProgressListener listener)
            throws IOException {
        if (client == null) {
            // return null if not an HTTP(S) request
            return null;
        }
        String bodyHref = getBodyReferenceHref(ref);
        try (CloseableHttpClient bodyClient = buildHttpClient(bodyHref);
                CloseableHttpResponse bodyResponse = bodyHttpRequest(bodyClient, bodyHref);
                InputStream bodyInput = getInputStream(bodyResponse, bodyHref, listener)) {
            RequestBuilder request;
            // prepare either a GET or a POST request
            if (ref.getMethod() != MethodType.POST_LITERAL) {
                request = RequestBuilder.get(ref.getHref());
            } else {
                request = RequestBuilder.post(ref.getHref());
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
            return client.execute(request.build());
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
            throw new WPSException(
                    "A POST request should contain a non empty body",
                    "NoApplicableCode",
                    getInputId());
        }
        String bodyReferenceHref = ref.getBodyReference().getHref();
        URLCheckers.confirm(bodyReferenceHref);
        return bodyReferenceHref;
    }

    private static CloseableHttpResponse bodyHttpRequest(CloseableHttpClient client, String href)
            throws IOException {
        // return null if not an HTTP(S) request; otherwise execute a GET request
        return isHttpURL(href) ? client.execute(RequestBuilder.get(href).build()) : null;
    }

    private InputStream getInputStream(
            CloseableHttpResponse response, String href, ProgressListener listener)
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

    private InputStream getResponseStream(CloseableHttpResponse response, String href)
            throws IOException {
        int code = response.getStatusLine().getStatusCode();
        if (code != 200) {
            // do not expose the status code to users
            throw new IllegalStateException(
                    "Error getting remote resources from "
                            + href
                            + ", http error "
                            + code
                            + ": "
                            + response.getStatusLine().getReasonPhrase());
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
                    "Input "
                            + getInputId()
                            + " size "
                            + file.length()
                            + " exceeds maximum allowed size of "
                            + maxSize,
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

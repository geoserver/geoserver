/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wps10.HeaderType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.MethodType;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;
import org.springframework.http.HttpHeaders;

/**
 * Handles an internal reference to a remote location
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RemoteRequestInputProvider extends AbstractInputProvider {

    static final Logger LOGGER = Logging.getLogger(RemoteRequestInputProvider.class);

    private int timeout;

    private ComplexPPIO complexPPIO;

    private long maxSize;

    public RemoteRequestInputProvider(
            InputType input, ComplexPPIO ppio, int timeout, long maxSize) {
        super(input, ppio);
        this.timeout = timeout;
        this.complexPPIO = ppio;
        this.maxSize = maxSize;
    }

    @Override
    protected Object getValueInternal(ProgressListener listener) throws Exception {
        InputReferenceType ref = input.getReference();
        URL destination = new URL(ref.getHref());

        HttpMethod method = null;
        GetMethod refMethod = null;
        InputStream input = null;
        InputStream refInput = null;

        // execute the request
        listener.started();
        try {
            if ("file".equalsIgnoreCase(destination.getProtocol())) {
                File file = URLs.urlToFile(destination);
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

                input = new FileInputStream(file);
            } else if ("http".equalsIgnoreCase(destination.getProtocol())) {
                // setup the client
                HttpClient client = new HttpClient();
                HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                params.setSoTimeout(timeout);
                params.setConnectionTimeout(timeout);
                // TODO: make the http client a well behaved http client, no more than x connections
                // per server (x admin configurable maybe), persistent connections and so on
                HttpConnectionManager manager = new SimpleHttpConnectionManager();
                manager.setParams(params);
                client.setHttpConnectionManager(manager);

                // prepare either a GET or a POST request
                if (ref.getMethod() == null || ref.getMethod() == MethodType.GET_LITERAL) {
                    GetMethod get = new GetMethod(ref.getHref());
                    get.setFollowRedirects(true);
                    method = get;
                } else {
                    String encoding = ref.getEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }

                    PostMethod post = new PostMethod(ref.getHref());
                    Object body = ref.getBody();
                    if (body == null) {
                        if (ref.getBodyReference() != null) {
                            URL refDestination = new URL(ref.getBodyReference().getHref());
                            if ("http".equalsIgnoreCase(refDestination.getProtocol())) {
                                // open with commons http client
                                refMethod = new GetMethod(ref.getBodyReference().getHref());
                                refMethod.setFollowRedirects(true);
                                client.executeMethod(refMethod);
                                refInput = refMethod.getResponseBodyAsStream();
                            } else {
                                // open with the built-in url management
                                URLConnection conn = refDestination.openConnection();
                                conn.setConnectTimeout(timeout);
                                conn.setReadTimeout(timeout);
                                refInput = conn.getInputStream();
                            }
                            post.setRequestEntity(
                                    new InputStreamRequestEntity(
                                            refInput, complexPPIO.getMimeType()));
                        } else {
                            throw new WPSException(
                                    "A POST request should contain a non empty body");
                        }
                    } else if (body instanceof String) {
                        post.setRequestEntity(
                                new StringRequestEntity(
                                        (String) body, complexPPIO.getMimeType(), encoding));
                    } else {
                        throw new WPSException(
                                "The request body should be contained in a CDATA section, "
                                        + "otherwise it will get parsed as XML instead of being preserved as is");
                    }
                    method = post;
                }
                // add eventual extra headers
                if (ref.getHeader() != null) {
                    for (Iterator it = ref.getHeader().iterator(); it.hasNext(); ) {
                        HeaderType header = (HeaderType) it.next();
                        method.setRequestHeader(header.getKey(), header.getValue());
                    }
                }
                int code = client.executeMethod(method);

                if (code == 200) {
                    try {
                        Header length = method.getResponseHeader(HttpHeaders.CONTENT_LENGTH);
                        if (maxSize > 0
                                && length != null
                                && Long.parseLong(length.getValue()) > maxSize) {
                            throw new WPSException(
                                    "Input "
                                            + getInputId()
                                            + " size "
                                            + length.getValue()
                                            + " exceeds maximum allowed size of "
                                            + maxSize
                                            + " according to HTTP Content-Lenght response header",
                                    "NoApplicableCode",
                                    getInputId());
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.log(
                                Level.FINE,
                                "Failed to parse content lenght to check input limits respect, "
                                        + "moving on and checking data as it comes in",
                                e);
                    }
                    input = method.getResponseBodyAsStream();
                    if (maxSize > 0) {
                        input = new MaxSizeInputStream(input, getInputId(), maxSize);
                    }
                } else {
                    throw new WPSException(
                            "Error getting remote resources from "
                                    + ref.getHref()
                                    + ", http error "
                                    + code
                                    + ": "
                                    + method.getStatusText());
                }
            } else {
                // use the normal url connection methods then...
                URLConnection conn = destination.openConnection();
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                input = conn.getInputStream();

                if (maxSize > 0) {
                    input = new MaxSizeInputStream(input, getInputId(), maxSize);
                }
            }

            // actually parse the data
            if (input != null) {
                CancellingInputStream is = new CancellingInputStream(input, listener);
                Object result = complexPPIO.decode(is);
                if (result == null || complexPPIO.getType().isInstance(result)) {
                    return result;
                }
                // Some text parsers return a Map when it can not be converted to
                // the proper type.  Detect those errors here rather than later.
                throw new IllegalArgumentException(
                        "Decoded result is not a "
                                + complexPPIO.getType().getName()
                                + ", got a: "
                                + result.getClass().getName());
            } else {
                throw new WPSException("Could not find a mean to read input " + inputId);
            }
        } catch (WPSException e) {
            throw e;
        } catch (Exception e) {
            // Log the exception and replace with a generic exception to prevent
            // potentially disclosing sensitive information from a remote resource.
            listener.exceptionOccurred(e);
            String message = "Failed to retrieve value for input " + getInputId();
            LOGGER.log(Level.WARNING, message, e);
            throw new WPSException(message);
        } finally {
            listener.progress(100);
            listener.complete();
            // make sure to close the connection and streams no matter what
            if (refInput != null) {
                refInput.close();
            }
            if (input != null) {
                input.close();
            }
            if (method != null) {
                method.releaseConnection();
            }
            if (refMethod != null) {
                refMethod.releaseConnection();
            }
        }
    }

    @Override
    public int longStepCount() {
        return 1;
    }
}

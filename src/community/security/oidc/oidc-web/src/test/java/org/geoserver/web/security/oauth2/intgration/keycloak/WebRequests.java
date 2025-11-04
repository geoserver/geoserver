/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/** simple class to make web requests to keycloak easier - GET and POST. */
public class WebRequests {

    private static final Logger LOG = Logger.getLogger(WebRequests.class.getName());

    /**
     * execute a GET request (i.e. to keycloak container)
     *
     * @param uri url for the reqeust
     * @return response from server (see WebResponse class)
     * @throws Exception error occurred
     */
    public static WebResponse webRequestGET(String uri) throws Exception {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        WebResponse response = new WebResponse(connection);
        connection.disconnect();
        return response;
    }

    /**
     * execute a POST request (i.e. to keycloak container). Body should be in `application/x-www-form-urlencoded`
     * format.
     *
     * @param uri URL to post to
     * @param body body of request
     * @param cookieManager from a previous request-response -- attach cookies to request
     * @return see WebResponse
     * @throws Exception error occurred
     */
    public static WebResponse webRequestPOSTForm(String uri, String body, CookieManager cookieManager)
            throws Exception {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true); // Enable output for sending data
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        // don't follow redirects - we need to do them ourselves.
        connection.setInstanceFollowRedirects(false);

        String cookieVal = String.join(
                "; ",
                cookieManager.getCookieStore().getCookies().stream()
                        .map(x -> x.getName() + "=" + x.getValue())
                        .toList());
        if (!cookieManager.getCookieStore().getCookies().isEmpty()) {
            connection.setRequestProperty("Cookie", cookieVal);
        }

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("curl -H \"Content-Type:application/x-www-form-urlencoded\" -d \"" + body + "\"  \\");
            LOG.fine("     -H \"Cookie: " + cookieVal + "\"  \\");
            LOG.fine("     \"" + uri + "\" -v");
        }

        WebResponse response = new WebResponse(connection);
        connection.disconnect();
        return response;
    }

    /** captures basic information about the response. */
    public static class WebResponse {

        public Map<String, List<String>> headers;
        public int statusCode;
        public String body = "";
        public HttpURLConnection connection;
        public CookieManager cookieManager;

        public WebResponse(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            this.statusCode = connection.getResponseCode();
            this.body = IOUtils.toString(getInputStream(connection), StandardCharsets.UTF_8);
            this.headers = connection.getHeaderFields();

            this.cookieManager = new CookieManager();
            List<String> cookies = headers.get("Set-Cookie");
            if (cookies != null) {
                for (String cookie : cookies) {
                    this.cookieManager
                            .getCookieStore()
                            .add(null, HttpCookie.parse(cookie).get(0));
                }
            }
        }

        /**
         * HttpURLConnection will have the body of the response either in #getInputStream or #getErrorStream. This
         * handles both cases.
         *
         * @param connection response connection
         * @return body of the response
         * @throws IOException error occurred
         */
        public InputStream getInputStream(HttpURLConnection connection) throws IOException {
            try {
                return connection.getInputStream();
            } catch (Exception e) {
                return connection.getErrorStream();
            }
        }
    }
}

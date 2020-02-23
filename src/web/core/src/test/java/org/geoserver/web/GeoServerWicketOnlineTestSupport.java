package org.geoserver.web;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.IOUtils;

/**
 * Utilities for online integration testing against GeoServer's Wicket UI
 *
 * @see GeoServerWicketOnlineTest
 */
public class GeoServerWicketOnlineTestSupport {
    protected static final String GEOSERVER_BASE_URL = "http://localhost:8080/geoserver";

    protected boolean isOnline() {
        try {
            URL u = new URL(GEOSERVER_BASE_URL);
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("HEAD");
            huc.connect();
            return huc.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    /** Logs out the session associated with the provided JSESSIONID */
    public void logout(String jsessionid) throws IOException {
        post("j_spring_security_logout", "", "application/x-www-form-urlencoded", jsessionid)
                .disconnect();
    }

    /**
     * Log in to geoserver with a username and password
     *
     * @return The JSESSIONID associated with the authenticated session.
     */
    public String login(String username, String password) throws IOException {
        // GET the homepage, and aquire a fresh (unauthenticated) JSESSIONID
        HttpURLConnection huc = get("web/", null);
        String cookie = huc.getHeaderField("Set-Cookie");
        String jsessionid = parseJsessionid(cookie);
        huc.disconnect();

        // Log in via /j_spring_security_check
        String body = "username=" + username + "&password=" + password;
        huc =
                preparePost(
                        "j_spring_security_check",
                        body.length(),
                        "application/x-www-form-urlencoded",
                        jsessionid);

        // Follow redirects to get the new JSESSIONID for the authenticated session
        huc.setInstanceFollowRedirects(false);
        huc = doPost(huc, body);
        while (huc.getResponseCode() == 302) {
            if (huc.getHeaderField("Set-Cookie") != null) {
                cookie = huc.getHeaderField("Set-Cookie");
                jsessionid = parseJsessionid(cookie);
            }
            String location = huc.getHeaderField("Location");
            if (location.startsWith(GEOSERVER_BASE_URL)) {
                location = location.substring(GEOSERVER_BASE_URL.length() + 1);
            }
            huc.disconnect();

            huc = prepareGet(location, jsessionid, null);
            huc.setInstanceFollowRedirects(false);
            huc = doGet(huc);
        }

        // Verify that we have logged in successfuly
        String homePage = IOUtils.toString(huc.getInputStream(), "UTF-8");
        assertTrue(homePage.contains("Logged in as <span>" + username + "</span>"));

        // Return the JSESSIONID for the authenticated session
        huc.disconnect();
        return jsessionid;
    }

    private String parseJsessionid(String cookie) {
        String jsessionid = null;

        String[] parts = cookie.split(";");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("JSESSIONID=")) {
                jsessionid = parts[i];
            }
        }
        return jsessionid;
    }

    /**
     * Performs an HTTP GET against the provided url
     *
     * @param url The URL to GET
     * @param jsessionid The jsessionid cookie of the session, in the form "JSESSIONID=foo". May be
     *     null, in which case the GET is performed without authentication
     * @return The open HttpUrlConnection resulting from the GET. Callers must read the response and
     *     {@link HttpURLConnection#disconnect()} from the connection.
     */
    protected HttpURLConnection get(String url, String jsessionid) throws IOException {
        return get(url, jsessionid, null);
    }

    /**
     * Performs an HTTP GET against the provided url
     *
     * @param url The URL to GET
     * @param jsessionid The jsessionid cookie of the session, in the form "JSESSIONID=foo". May be
     *     null, in which case the GET is performed without authentication
     * @param accept the content type(s) to accept. The accept header is ommited if this is null
     * @return The open HttpUrlConnection resulting from the GET. Callers must read the response and
     *     {@link HttpURLConnection#disconnect()} from the connection.
     */
    protected HttpURLConnection get(String url, String jsessionid, String accept)
            throws IOException {
        return doGet(prepareGet(url, jsessionid, accept));
    }

    /**
     * Prepares an HTTP GET request against the provided url
     *
     * @param @param url The URL to GET
     * @param jsessionid The jsessionid cookie of the session, in the form "JSESSIONID=foo". May be
     *     null, in which case the GET is performed without authentication
     * @param accept the content type(s) to accept. The accept header is ommited if this is null
     * @return The prepared HttpUrlConnection. Callers may add headers or other properties before
     *     calling {@link HttpURLConnection#connect()} and reading the response.
     */
    protected HttpURLConnection prepareGet(String url, String jsessionid, String accept)
            throws IOException {
        URL u = new URL(GEOSERVER_BASE_URL + "/" + url);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        if (accept != null) {
            huc.setRequestProperty("Accept", accept);
        }
        huc.setRequestMethod("GET");
        if (jsessionid != null) {
            huc.setRequestProperty("Cookie", jsessionid);
            huc.setRequestProperty("Host", "localhost:8080");
            huc.setRequestProperty("Upgrade-Insecure-Requests", "1");
        }
        return huc;
    }

    /**
     * Calls {@link HttpURLConnection#connect()} on the provided connection, and returns it.
     *
     * @param huc An {@link HttpURLConnection} prepared for a get request
     * @return the opened connection
     */
    protected HttpURLConnection doGet(HttpURLConnection huc) throws IOException {
        huc.connect();
        return huc;
    }

    /**
     * Performs an HTTP POST against the provided url
     *
     * @param url The URL to POST
     * @param body The content to post
     * @param contentType The Content-Type of the content
     * @param jsessionid The jsessionid cookie of the session, in the form "JSESSIONID=foo". May be
     *     null, in which case the POST is performed without authentication
     * @return The open HttpUrlConnection resulting from the POST. Callers must read the response
     *     and {@link HttpURLConnection#disconnect()} from the connection.
     */
    protected HttpURLConnection post(String url, String body, String contentType, String jsessionid)
            throws IOException {
        return doPost(preparePost(url, body.length(), contentType, jsessionid), body);
    }

    /**
     * Prepares an HTTP POST request against the provided url
     *
     * @param url The URL to POST
     * @param contentLength The length of the content that will be sent
     * @param contentType The Content-Type of the content
     * @param jsessionid The jsessionid cookie of the session, in the form "JSESSIONID=foo". May be
     *     null, in which case the POST is performed without authentication
     * @return The prepared HttpUrlConnection. Callers may add headers or other properties before
     *     calling {@link HttpURLConnection#connect()}, sending the body, and reading the response.
     */
    protected HttpURLConnection preparePost(
            String url, int contentLength, String contentType, String jsessionid)
            throws IOException {
        URL u = new URL(GEOSERVER_BASE_URL + "/" + url);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        huc.setRequestMethod("POST");
        huc.setRequestProperty("Content-Type", contentType);
        huc.setRequestProperty("Content-Length", String.valueOf(contentLength));

        if (jsessionid != null) {
            huc.setRequestProperty("Cookie", jsessionid);
            huc.setRequestProperty("Host", "localhost:8080");
            huc.setRequestProperty("Origin", "http://localhost:8080");
            huc.setRequestProperty("Connection", "keep-alive");
            huc.setRequestProperty("Upgrade-Insecure-Requests", "1");
        }
        huc.setDoOutput(true);
        return huc;
    }

    /**
     * Calls {@link HttpURLConnection#connect()} on the provided connection, writes the provided
     * content and returns the connection.
     *
     * @param huc An {@link HttpURLConnection} prepared for a post request
     * @param body The body to write to the connection
     * @return the opened connection
     */
    protected HttpURLConnection doPost(HttpURLConnection huc, String body) throws IOException {
        huc.connect();

        PrintWriter out = new java.io.PrintWriter(huc.getOutputStream());
        out.print(body);
        out.close();

        return huc;
    }

    /**
     * Performs an HTTP DELETE against the provided url
     *
     * @param url The URL to DELETE
     * @param jsessionid The jsessionid cookie of the session, in the form "JSESSIONID=foo". May be
     *     null, in which case the DELETE is performed without authentication
     * @return The open HttpUrlConnection resulting from the DELETE. Callers must read the response
     *     and {@link HttpURLConnection#disconnect()} from the connection.
     */
    protected HttpURLConnection delete(String url, String jsessionid) throws IOException {
        // Can just use doGet here, as it merely opens the connection
        return doGet(prepareDelete(url, jsessionid));
    }

    /**
     * Prepares an HTTP DELETE request against the provided url
     *
     * @param @param url The URL to DELETE
     * @param jsessionid The jsessionid cookie of the session, in the form "JSESSIONID=foo". May be
     *     null, in which case the DELETE is performed without authentication
     * @return The prepared HttpUrlConnection. Callers may add headers or other properties before
     *     calling {@link HttpURLConnection#connect()} and reading the response.
     */
    protected HttpURLConnection prepareDelete(String url, String jsessionid) throws IOException {
        URL u = new URL(GEOSERVER_BASE_URL + "/" + url);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();

        huc.setRequestMethod("DELETE");
        if (jsessionid != null) {
            huc.setRequestProperty("Cookie", jsessionid);
            huc.setRequestProperty("Host", "localhost:8080");
            huc.setRequestProperty("Upgrade-Insecure-Requests", "1");
        }
        return huc;
    }
}

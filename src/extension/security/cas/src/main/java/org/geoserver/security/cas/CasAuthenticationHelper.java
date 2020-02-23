/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An abstract helper class for authentication against a Cas server
 *
 * @author christian
 */
public abstract class CasAuthenticationHelper {

    protected URL casUrlPrefix;
    /** true for an SSL (TLS) connection */
    protected boolean secure;

    protected HttpCookie ticketGrantingCookie, warningCookie;

    /** casUrlPrefix is the CAS Server URL including context root */
    public CasAuthenticationHelper(URL casUrlPrefix) {
        secure = "HTTPS".equalsIgnoreCase(casUrlPrefix.getProtocol());
        this.casUrlPrefix = casUrlPrefix;
    }

    /** create URL from a CAS protocol URI */
    protected URL createURLFromCasURI(String casUri) {
        URL retValue = null;
        try {
            retValue =
                    new URL(
                            casUrlPrefix.getProtocol(),
                            casUrlPrefix.getHost(),
                            casUrlPrefix.getPort(),
                            casUrlPrefix.getPath() + casUri);
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "Cannot build url from " + casUrlPrefix.toExternalForm() + " and " + casUri);
        }
        return retValue;
    }

    protected String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        StringBuffer buff = new StringBuffer();
        while ((line = in.readLine()) != null) {
            buff.append(line);
        }
        in.close();
        return buff.toString();
    }

    protected List<String> getResponseHeaderValues(HttpURLConnection conn, String hName) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; ; i++) {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);

            if (headerName == null && headerValue == null) {
                // No more headers
                break;
            }
            if (hName.equalsIgnoreCase(headerName)) {
                result.add(headerValue);
            }
        }
        return result;
    }

    protected List<HttpCookie> getCookies(HttpURLConnection conn) {
        List<HttpCookie> result = new ArrayList<HttpCookie>();
        List<String> cookieStrings = getResponseHeaderValues(conn, "Set-Cookie");
        for (String cookieString : cookieStrings) {
            result.addAll(HttpCookie.parse("Set-Cookie: " + cookieString));
        }
        cookieStrings = getResponseHeaderValues(conn, "Set-Cookie2");
        for (String cookieString : cookieStrings) {
            result.addAll(HttpCookie.parse("Set-Cookie2: " + cookieString));
        }
        return result;
    }

    protected HttpCookie getCookieNamed(List<HttpCookie> cookies, String cookieName) {
        for (HttpCookie c : cookies) {
            if (c.getName().equalsIgnoreCase(cookieName)) return c;
        }
        return null;
    }

    protected void writeParamsForPostAndSend(HttpURLConnection conn, Map<String, String> paramMap)
            throws IOException {
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());

        StringBuffer buff = new StringBuffer();
        for (Entry<String, String> entry : paramMap.entrySet()) {
            if (buff.length() > 0) buff.append("&");
            buff.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "utf-8"));
        }

        out.writeBytes(buff.toString());
        out.flush();
        out.close();
    }

    public HttpCookie getTicketGrantingCookie() {
        return ticketGrantingCookie;
    }

    public HttpCookie getWarningCookie() {
        return warningCookie;
    }

    /** Single logout from Cas server */
    public boolean ssoLogout() throws IOException {
        if (!secure) return true;
        if (ticketGrantingCookie == null) return true;

        URL logoutUrl = createURLFromCasURI(GeoServerCasConstants.LOGOUT_URI);
        HttpURLConnection conn = (HttpURLConnection) logoutUrl.openConnection();
        addCasCookies(conn);
        conn.getInputStream().close();
        extractCASCookies(getCookies(conn), conn);
        return getTicketGrantingCookie() != null
                && "\"\"".equals(getTicketGrantingCookie().getValue());
    }

    /** add Cas cookies to request */
    protected void addCasCookies(HttpURLConnection conn) {
        String cookieString = "";
        if (checkCookieForSend(warningCookie)) cookieString = warningCookie.toString();
        if (checkCookieForSend(ticketGrantingCookie)) {
            if (cookieString.length() > 0) cookieString += ",";
            cookieString += ticketGrantingCookie.toString();
        }
        if (cookieString.length() > 0) conn.setRequestProperty("Cookie", cookieString);
    }

    public boolean isSecure() {
        return secure;
    }

    protected boolean checkCookieForSend(HttpCookie cookie) {
        if (cookie == null) return false;
        if (cookie.hasExpired()) return false;
        if (isSecure() == false && cookie.getSecure()) {
            return false;
        }
        return true;
    }

    /**
     * The concrete login, after sucessful login, the cookies should be set using {@link
     * #extractCASCookies(List, HttpURLConnection)}
     */
    public abstract boolean ssoLogin() throws IOException;

    /**
     * Get a service ticket for the service
     *
     * <p>Precondition: successful log in wiht {@link #ssoLogin()} {@link #isSecure()} == true
     */
    public String getServiceTicket(URL service) throws IOException {

        if (getTicketGrantingCookie() == null || getTicketGrantingCookie().getValue().isEmpty()) {
            throw new IOException("na valid TGC ");
        }

        URL loginUrl =
                createURLFromCasURI(
                        GeoServerCasConstants.LOGIN_URI + "?service=" + service.toExternalForm());
        HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
        conn.setInstanceFollowRedirects(false);
        addCasCookies(conn);
        conn.getInputStream().close();
        List<String> values = getResponseHeaderValues(conn, "Location");
        if (values.isEmpty()) {
            throw new IOException("No redirect received for " + loginUrl);
        }
        String redirectURL = values.get(0);
        String ticket = null;
        URL rURL = new URL(redirectURL);
        for (String kvp : rURL.getQuery().split("&")) {
            String[] tmp = kvp.split("=");
            if ("ticket".equalsIgnoreCase((tmp[0]).trim())) {
                ticket = tmp[1].trim();
                break;
            }
        }
        return ticket;
    }

    /** extract Cas cookies from all received cookies */
    public void extractCASCookies(List<HttpCookie> cookies, HttpURLConnection conn) {
        warningCookie = getCookieNamed(cookies, "CASPRIVACY");
        ticketGrantingCookie = getCookieNamed(cookies, "CASTGC");
    }
}

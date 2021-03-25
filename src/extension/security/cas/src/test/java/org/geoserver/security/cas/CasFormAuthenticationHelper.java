/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class for authentication against a Cas server
 *
 * <p>supported authentication mechanisms
 *
 * <p>- Cas Form login
 *
 * @author christian
 */
public class CasFormAuthenticationHelper extends CasAuthenticationHelper {

    public static final String CAS_4_0_USER = "casuser";
    public static final String CAS_4_0_PW = "Mellon";

    String username, password;

    public CasFormAuthenticationHelper(URL casUrlPrefix, String username, String password) {
        super(casUrlPrefix);
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean ssoLogin() throws IOException {
        URL loginUrl = createURLFromCasURI("/login");
        HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
        String responseString = readResponse(conn);
        String execution = extractFormParameter(responseString, "\"execution\"");
        if (execution == null)
            throw new IOException(" No hidden execution field for: " + loginUrl.toString());

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("username", username);
        paramMap.put("password", password);
        paramMap.put("_eventId", "submit");
        paramMap.put("execution", execution);
        paramMap.put("geolocation", "");

        conn = (HttpURLConnection) loginUrl.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // conn.setRequestProperty("Cookie", sessionCookieSend);

        writeParamsForPostAndSend(conn, paramMap);
        if (conn.getResponseCode() == 401) return false;

        List<HttpCookie> cookies = getCookies(conn);
        readResponse(conn);

        extractCASCookies(cookies, conn);

        return ticketGrantingCookie != null;
    }

    protected String extractFormParameter(String formLoginHtml, String searchString) {
        int index = formLoginHtml.indexOf(searchString);
        if (index == -1) return null;
        index += searchString.length();
        index = formLoginHtml.indexOf("\"", index);
        int index2 = formLoginHtml.indexOf("\"", index + 1);
        return formLoginHtml.substring(index + 1, index2);
    }
}

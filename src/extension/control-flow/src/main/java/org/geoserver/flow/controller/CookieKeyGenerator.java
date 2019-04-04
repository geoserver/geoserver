/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.rmi.server.UID;
import javax.servlet.http.Cookie;
import org.geoserver.ows.Request;

/**
 * Helper class that allows to identify a specific user and returns a unique key for it. The
 * mechanism works by setting a cookie on HTTP request to identify the users.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CookieKeyGenerator implements KeyGenerator {

    static String COOKIE_NAME = "GS_FLOW_CONTROL";

    static String COOKIE_PREFIX = "GS_CFLOW_";

    /** Returns an id that can be associated uniquely to this user */
    public String getUserKey(Request request) {
        // check if this client already made other connections
        Cookie idCookie = null;
        Cookie[] cookies = request.getHttpRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(COOKIE_NAME)) {
                    idCookie = cookie;
                    break;
                }
            }
        }

        // see if we have that queue already
        if (idCookie == null) {
            idCookie = new Cookie(COOKIE_NAME, COOKIE_PREFIX + new UID().toString());
        }
        request.getHttpResponse().addCookie(idCookie);

        return idCookie.getValue();
    }
}

/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.spring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;

@SuppressWarnings("serial")
public class GeoServerSession extends WebSession{
    public GeoServerSession(Request request) {
        super(request);
    }

    public static GeoServerSession get() {
        return (GeoServerSession)Session.get();
    }

    public Authentication getAuthentication(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null &&
                auth.getAuthorities().size() == 1 &&
                "ROLE_ANONYMOUS".equals(auth.getAuthorities().iterator().next().getAuthority())
           ) return null;

        return auth;
    }
}

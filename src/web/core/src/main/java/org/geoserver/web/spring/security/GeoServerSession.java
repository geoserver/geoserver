package org.geoserver.web.spring.security;

import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
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
                auth.getAuthorities().length == 1 &&
                "ROLE_ANONYMOUS".equals(auth.getAuthorities()[0].getAuthority())
           ) return null;

        return auth;
    }
}

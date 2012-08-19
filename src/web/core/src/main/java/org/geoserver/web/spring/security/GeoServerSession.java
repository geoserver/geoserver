package org.geoserver.web.spring.security;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

package org.geoserver.web.spring.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;

@SuppressWarnings("serial")
public class GeoServerSession extends WebSession {

    private Map<String, String> cache;

    public GeoServerSession(Request request) {
        super(request);
        
        cache = new HashMap<String, String>();
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
    
    public String getCachedValue(String key) {
        return cache.get(key);
    }
    
    public String cacheValue(String key, String value) {
        return cache.put(key, value);
    }
}

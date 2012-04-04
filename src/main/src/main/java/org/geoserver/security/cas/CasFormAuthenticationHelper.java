/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
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
 * supported authentication mechanisms
 * 
 *  - Cas Form login
 * 
 * @author christian
 *
 */
public class CasFormAuthenticationHelper extends CasAuthenticationHelper{

    
    String username,password;

    public CasFormAuthenticationHelper (URL casUrlPrefix,String username, String password) {
        this(casUrlPrefix,null,username,password);
    }
    
    public CasFormAuthenticationHelper (URL casUrlPrefix,URL proxyReceptor,String username, String password) {
        super(casUrlPrefix,proxyReceptor);
        this.username=username;
        this.password=password;        
    }
    
    
    public boolean ssoLogin(URL serviceURL) throws IOException{
        URL loginUrl = createURLFromCasURI("/login");
        HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
        String responseString = readResponse(conn);
        String loginTicket = extractFormParameter(responseString,"\"lt\"");
        if (loginTicket==null)
            throw new IOException (" No login ticket for: "+loginUrl.toString());
        String execution = extractFormParameter(responseString,"\"execution\"");
        if (execution==null)
            throw new IOException (" No hidden execution field for: "+loginUrl.toString());

        List<HttpCookie> cookies = getCookies(conn);
        HttpCookie sessionCookie = getCookieNamed(cookies, "JSESSIONID");        
        String sessionCookieSend=sessionCookie.toString();
        
        Map<String,String> paramMap = new HashMap<String,String>();
        paramMap.put("username",username);
        paramMap.put("password",password);
        paramMap.put("lt",loginTicket);
        paramMap.put("_eventId","submit");
        paramMap.put("submit","LOGIN");
        paramMap.put("execution",execution);
        if (serviceURL!=null)
            paramMap.put("service",serviceURL.toString());
                
        conn = (HttpURLConnection) loginUrl.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Cookie", sessionCookieSend);
        
        writeParamsForPostAndSend(conn, paramMap);

        cookies = getCookies(conn);
        readResponse(conn);
        if (serviceURL!=null) {
            String ticket = getResponseHeaderValues(conn,"Location").get(0);
        }
        
        warningCookie=getCookieNamed(cookies, "CASPRIVACY");
        ticketGrantingCookie=getCookieNamed(cookies, "CASTGC");
        
        return warningCookie!=null; 
    }

}

/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
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

import javax.servlet.http.HttpServletResponse;

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
public abstract class CasAuthenticationHelper {

    protected URL casUrlPrefix,proxyReceptor;
    protected boolean secure;
    protected String serviceTicket;


    protected HttpCookie ticketGrantingCookie,warningCookie; 
    

    public CasAuthenticationHelper (URL casUrlPrefix) {
        this(casUrlPrefix,null);
    }
    
    public CasAuthenticationHelper (URL casUrlPrefix,URL proxyReceptor) {
        secure="HTTPS".equalsIgnoreCase(casUrlPrefix.getProtocol());
        this.casUrlPrefix=casUrlPrefix;
        if (proxyReceptor != null && "HTTPS".equalsIgnoreCase(proxyReceptor.getProtocol())==false)
            throw new RuntimeException("proxy receptor url must be HTTPS");        
        this.proxyReceptor=proxyReceptor;
    }
    
    protected URL createURLFromCasURI(String casUri) {
        URL retValue=null;
        try {
            retValue = new URL(casUrlPrefix.getProtocol(),casUrlPrefix.getHost(),
                    casUrlPrefix.getPort(),casUrlPrefix.getPath()+casUri);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot build url from "+casUrlPrefix.toExternalForm()+
                    " and "+casUri);
        }
        return retValue;
    }
    
    protected String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        StringBuffer buff=new StringBuffer();
        while((line=in.readLine())!=null) {
                buff.append(line);
        }
        in.close();
        return buff.toString();        
    }
    
    protected List<String> getResponseHeaderValues(HttpURLConnection conn,String hName) {
        List<String> result = new ArrayList<String>();
        for (int i=0; ; i++) {
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
            result.addAll(HttpCookie.parse("Set-Cookie: "+cookieString));
        }
        cookieStrings = getResponseHeaderValues(conn, "Set-Cookie2");
        for (String cookieString : cookieStrings) {
            result.addAll(HttpCookie.parse("Set-Cookie2: "+cookieString));
        }
        return result;
    }
    
    protected HttpCookie getCookieNamed(List<HttpCookie> cookies, String cookieName) {
        for (HttpCookie c : cookies) {
            if (c.getName().equalsIgnoreCase(cookieName))
                return c;
        }
        return null;
    }


    protected void writeParamsForPostAndSend(HttpURLConnection conn, Map<String,String> paramMap) throws IOException{
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        
        StringBuffer buff = new StringBuffer();
        for (Entry<String,String> entry : paramMap.entrySet()) {
            if (buff.length()>0)
                buff.append("&");
            buff.append(entry.getKey()).append("=").
                append(URLEncoder.encode(entry.getValue(),"utf-8"));
        }
        
        out.writeBytes(buff.toString());
        out.flush();
        out.close();
    }

    protected String extractFormParameter(String formLoginHtml, String searchString) {        
        int index = formLoginHtml.indexOf(searchString);
        index+=searchString.length();
        index = formLoginHtml.indexOf("\"", index);
        int index2 = formLoginHtml.indexOf("\"", index+1);
        return  formLoginHtml.substring(index+1,index2);        
    }
    

    public HttpCookie getTicketGrantingCookie() {
        return ticketGrantingCookie;
    }

    public HttpCookie getWarningCookie() {
        return warningCookie;
    }
    
    public boolean ssoLogout() throws IOException {
        if (!secure) return true;
        if (ticketGrantingCookie==null) return true;
        
        URL logoutUrl = createURLFromCasURI("/logout");
        HttpURLConnection conn = (HttpURLConnection) logoutUrl.openConnection();
        addCasCookies(conn);
        boolean result = HttpServletResponse.SC_OK==conn.getResponseCode();
        if (result)
            warningCookie=ticketGrantingCookie=null;
        return result;
    }

    protected void addCasCookies(HttpURLConnection conn) {
        String cookieString="";
        if (warningCookie!=null)
            cookieString=warningCookie.toString();
        if (ticketGrantingCookie!=null && isSecure()) {
            if  (cookieString.length()> 0)
                 cookieString+=",";
            cookieString+=ticketGrantingCookie.toString();     
        }
        if (cookieString.length() > 0)
            conn.setRequestProperty("Cookie", cookieString);
    }
    
    public boolean isSecure() {
        return secure;
    }

    public  boolean ssoLogin() throws IOException {
        return ssoLogin(null);
    }
    
    public abstract boolean ssoLogin(URL serviceURL) throws IOException;

    public String getServiceTicket() {
        return serviceTicket;
    }

}

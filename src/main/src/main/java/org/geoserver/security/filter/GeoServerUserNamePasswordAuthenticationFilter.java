/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * User name / password authentication filter
 * 
 * 
 * @author christian
 * 
 */
public class GeoServerUserNamePasswordAuthenticationFilter extends GeoServerCompositeFilter
    implements GeoServerAuthenticationFilter {

    public static final String URL_FOR_LOGIN = "/j_spring_security_check";
    public static final String URL_LOGIN_SUCCCESS = "/";
    public static final String URL_LOGIN_FAILURE = "/web/?wicket:bookmarkablePage=:org.geoserver.web.GeoServerLoginPage&amp;error=true";
    public static final String URL_LOGIN_FORM="/admin/login.do";
    
    
    private LoginUrlAuthenticationEntryPoint aep;
    protected MessageDigest digest;


    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        
        try {            
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        } 

        
        UsernamePasswordAuthenticationFilterConfig upConfig = (UsernamePasswordAuthenticationFilterConfig) config;
        
        aep=new LoginUrlAuthenticationEntryPoint(URL_LOGIN_FORM);
        aep.setForceHttps(false);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e2) {
            throw new IOException(e2);
        }

        RememberMeServices rms = securityManager.getRememberMeService(); 

        // add login filter
        UsernamePasswordAuthenticationFilter filter = new UsernamePasswordAuthenticationFilter();


        filter.setPasswordParameter(upConfig.getPasswordParameterName());
        filter.setUsernameParameter(upConfig.getUsernameParameterName());
        filter.setAuthenticationManager(getSecurityManager());

        filter.setRememberMeServices(rms);
        GeoServerWebAuthenticationDetailsSource s = new GeoServerWebAuthenticationDetailsSource();
        filter.setAuthenticationDetailsSource(s);

        filter.setAllowSessionCreation(false);
        filter.setFilterProcessesUrl(URL_FOR_LOGIN);

        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl(URL_LOGIN_SUCCCESS);
        filter.setAuthenticationSuccessHandler(successHandler);

        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        // TODO, check this when using encrypting of URL parameters
        failureHandler
                .setDefaultFailureUrl(URL_LOGIN_FAILURE);
        filter.setAuthenticationFailureHandler(failureHandler);

        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
//        SecurityContextHolderAwareRequestFilter contextAwareFilter = new SecurityContextHolderAwareRequestFilter();
//        try {
//            contextAwareFilter.afterPropertiesSet();
//        } catch (ServletException e) {
//            throw new IOException(e);
//        }
//        getNestedFilters().add(contextAwareFilter);
    }
    
    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        super.doFilter(req, res, chain);
    }            

    /** 
     * returns username:md5(password:filtername)
     */
//    @Override
//    public String getCacheKey(HttpServletRequest request) {
//        String uname = request.getParameter(usernameParameter);
//        String passwd = request.getParameter(passwordParameter);
//        if (!StringUtils.hasLength(uname)) return null;
//        if (!StringUtils.hasLength(passwd)) return null;
//        
//        if (GeoServerUser.ROOT_USERNAME.equals(uname)) 
//            return null;
//
//        StringBuffer buff = new StringBuffer(passwd);
//        buff.append(":");
//        buff.append(getName());
//        String digestString = null;
//        try {
//            digestString = new String(Hex.encode(digest.digest(buff.toString().getBytes("utf-8"))));
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }        
//        buff = new StringBuffer(uname);
//        buff.append(":");
//        buff.append(digestString);
//        return buff.toString();        
//    }

}

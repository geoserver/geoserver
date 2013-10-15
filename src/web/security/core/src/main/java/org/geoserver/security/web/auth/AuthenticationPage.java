/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.HTTPMethod;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.SSLFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.HelpLink;

/**
 * Main menu page for authentication.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticationPage extends AbstractSecurityPage {

    Form<SecurityManagerConfig> form;
    LogoutFilterConfig logoutFilterConfig;
    SSLFilterConfig sslFilterConfig;
    SecurityManagerConfig config;
    AuthFilterChainPanel authFilterChainPanel;
    
    public AuthenticationPage() {
        initComponents();
    }

    void initComponents() {
        
        // The request filter chain objects have to be cloned
        config = getSecurityManager().getSecurityConfig();
        List<RequestFilterChain> clones = new ArrayList<RequestFilterChain>();
        
        for (RequestFilterChain chain : config.getFilterChain().getRequestChains()) {            
            try {
                clones.add((RequestFilterChain)chain.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }            
        }
        config.setFilterChain(new GeoServerSecurityFilterChain(clones));
        
        form = new Form("form", 
            new CompoundPropertyModel<SecurityManagerConfig>(config));
        add(form);

        try {
            logoutFilterConfig= (LogoutFilterConfig) getSecurityManager().loadFilterConfig(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        form.add(new TextField<String>("redirectURL",new PropertyModel<String>(this, "logoutFilterConfig.redirectURL")));
        
        try {
            sslFilterConfig= (SSLFilterConfig) getSecurityManager().loadFilterConfig(GeoServerSecurityFilterChain.SSL_FILTER);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        form.add(new TextField<Integer>("sslPort",new PropertyModel<Integer>(this, "sslFilterConfig.sslPort")));


        form.add(new AuthenticationFiltersPanel("authFilters"));
        form.add(new HelpLink("authFiltersHelp").setDialog(dialog));

        form.add(new AuthenticationProvidersPanel("authProviders"));
        form.add(new HelpLink("authProvidersHelp").setDialog(dialog));
        
        form.add(new SecurityFilterChainsPanel("authChains",config)); 
        form.add(new HelpLink("authChainsHelp").setDialog(dialog));

        form.add(authFilterChainPanel = new AuthFilterChainPanel("filterChain", 
            new PropertyModel<GeoServerSecurityFilterChain>(form.getModel(), "filterChain")));
        form.add(new HelpLink("filterChainHelp").setDialog(dialog));

        form.add(new AuthenticationChainPanel("providerChain", form));
        form.add(new HelpLink("providerChainHelp").setDialog(dialog));

        form.add(new SubmitLink("save", form) {
            @Override
            public void onSubmit() {
                try {
                    getSecurityManager()
                        .saveSecurityConfig((SecurityManagerConfig) getForm().getModelObject());
                    getSecurityManager().saveFilter(logoutFilterConfig);
                    getSecurityManager().saveFilter(sslFilterConfig);
                    doReturn();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error saving authentication config", e);
                    error(e);
                }
            }
        });
        form.add(new Link("cancel") {
            @Override
            public void onClick() {
                doReturn();
            }
        });
    }
    
    public void updateChainComponents() {                
        form.replace(new SecurityFilterChainsPanel("authChains", config));
    }

    class AuthenticationChainPanel extends FormComponentPanel {

        public AuthenticationChainPanel(String id, Form form) {
            super(id, new Model());

            add(new AuthenticationChainPalette("authProviderNames"));
        }
    }


    class AuthFilterChainPanel extends FormComponentPanel {
        
        DropDownChoice<HTTPMethod> httpMethodChoice;
        TextField<String> urlPathField,chainTestResultField;
        String urlPath,chainTestResult;
        HTTPMethod httpMethod=HTTPMethod.GET;

        public AuthFilterChainPanel(String id, IModel<GeoServerSecurityFilterChain> model) {
            super(id, new Model());

            this.setOutputMarkupId(true);
            
            add(urlPathField=new TextField<String>("urlPath",new PropertyModel<String>(this,"urlPath")));            
            urlPathField.setOutputMarkupId(true);
            urlPathField.add(
                    new OnChangeAjaxBehavior() {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                        }
                    });
                                                 
            add(chainTestResultField=new TextField<String>("chainTestResult",new PropertyModel<String>(this,"chainTestResult")));
            chainTestResultField.setEnabled(false);
            chainTestResultField.setOutputMarkupId(true);
           
            
            add(httpMethodChoice=new DropDownChoice<HTTPMethod>("httpMethod",
                    new PropertyModel<HTTPMethod>(this,"httpMethod"),
                    Arrays.asList(HTTPMethod.values())));
            httpMethodChoice.setOutputMarkupId(true);
            httpMethodChoice.setNullValid(false);
            httpMethodChoice.add(
                    new OnChangeAjaxBehavior() {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                        }
                    });
        
            add(new AjaxSubmitLink("chainTest") {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    try {
                        String result="NONE";
                        HttpServletRequest request = getHttpRequest();
                        for (RequestFilterChain chain : config.getFilterChain().getRequestChains()) {
                            if (httpMethod!=null && urlPath!=null) {
                                if (getProxy().matcherForChain(chain).matches(request)) {
                                    result=chain.getName();
                                    break;
                                }
                            }
                        }        
                        chainTestResultField.getModel().setObject(result);
                        target.addComponent(chainTestResultField);
                    }
                    catch(Exception e) {
                        error(e);
                        LOGGER.log(Level.WARNING, "Connection error", e);
                        target.addComponent(feedbackPanel);
                    }
                }
                protected GeoServerSecurityFilterChainProxy getProxy() {
                    return GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
                }
                
                HttpServletRequest getHttpRequest() {
                    return new HttpServletRequest() {
                        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
                        }
                        public void setAttribute(String name, Object o) {
                        }
                        public void removeAttribute(String name) {
                        }
                        public boolean isSecure() {
                            return false;
                        }
                        public int getServerPort() {
                            return 0;
                        }
                        public String getServerName() {
                            return null;
                        }
                        public String getScheme() {
                            return null;
                        }
                        public RequestDispatcher getRequestDispatcher(String path) {
                            return null;
                        }
                        public int getRemotePort() {
                            return 0;
                        }
                        public String getRemoteHost() {
                            return null;
                        }
                        public String getRemoteAddr() {
                            return null;
                        }
                        public String getRealPath(String path) {
                            return null;
                        }
                        public BufferedReader getReader() throws IOException {
                            return null;
                        }
                        public String getProtocol() {
                            return null;
                        }
                        public String[] getParameterValues(String name) {
                            return null;
                        }
                        public Enumeration getParameterNames() {
                            return null;
                        }
                        public Map getParameterMap() {
                            return null;
                        }
                        public String getParameter(String name) {
                            return null;
                        }
                        public Enumeration getLocales() {
                            return null;
                        }
                        public Locale getLocale() {
                            return null;
                        }
                        public int getLocalPort() {
                            return 0;
                        }
                        public String getLocalName() {
                            return null;
                        }
                        public String getLocalAddr() {
                            return null;
                        }
                        public ServletInputStream getInputStream() throws IOException {
                            return null;
                        }
                        public String getContentType() {
                            return null;
                        }
                        public int getContentLength() {
                            return 0;
                        }
                        public String getCharacterEncoding() {
                            return null;
                        }
                        public Enumeration getAttributeNames() {
                            return null;
                        }
                        public Object getAttribute(String name) {
                            return null;
                        }
                        public boolean isUserInRole(String role) {
                            return false;
                        }
                        public boolean isRequestedSessionIdValid() {
                            return false;
                        }
                        public boolean isRequestedSessionIdFromUrl() {
                            return false;
                        }
                        public boolean isRequestedSessionIdFromURL() {
                            return false;
                        }
                        public boolean isRequestedSessionIdFromCookie() {
                            return false;
                        }
                        public Principal getUserPrincipal() {
                            return null;
                        }
                        public HttpSession getSession(boolean create) {
                            return null;
                        }
                        public HttpSession getSession() {
                            return null;
                        }
                        public String getServletPath() {
                            return "";
                        }
                        public String getRequestedSessionId() {
                            return null;
                        }
                        public StringBuffer getRequestURL() {
                            return null;
                        }
                        public String getRequestURI() {
                            return null;
                        }
                        public String getRemoteUser() {
                            return null;
                        }
                        public String getQueryString() {
                            return null;
                        }
                        public String getPathTranslated() {
                            return null;
                        }
                        public String getPathInfo() {
                            return urlPath;
                        }
                        public String getMethod() {
                            return httpMethod.toString();
                        }
                        public int getIntHeader(String name) {
                            return 0;
                        }
                        public Enumeration getHeaders(String name) {
                            return null;
                        }
                        public Enumeration getHeaderNames() {
                            return null;
                        }
                        public String getHeader(String name) {
                            return null;
                        }
                        public long getDateHeader(String name) {
                            return 0;
                        }
                        public Cookie[] getCookies() {
                            return null;
                        }
                        public String getContextPath() {
                            return null;
                        }
                        public String getAuthType() {
                            return null;
                        }
                    }; 
                }

            }.setDefaultFormProcessing(false));

            
        }    
                
    }
}

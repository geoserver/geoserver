/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.RangeValidator;
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
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

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

    @SuppressWarnings("serial")
    void initComponents() {

        // The request filter chain objects have to be cloned
        config = getSecurityManager().getSecurityConfig();
        List<RequestFilterChain> clones = new ArrayList<>();

        for (RequestFilterChain chain : config.getFilterChain().getRequestChains()) {
            try {
                clones.add((RequestFilterChain) chain.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
        config.setFilterChain(new GeoServerSecurityFilterChain(clones));

        form = new Form<>("form", new CompoundPropertyModel<>(config));
        add(form);

        try {
            logoutFilterConfig =
                    (LogoutFilterConfig)
                            getSecurityManager()
                                    .loadFilterConfig(
                                            GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        form.add(
                new TextField<>(
                        "redirectURL",
                        new PropertyModel<>(this, "logoutFilterConfig.redirectURL")));

        try {
            sslFilterConfig =
                    (SSLFilterConfig)
                            getSecurityManager()
                                    .loadFilterConfig(GeoServerSecurityFilterChain.SSL_FILTER);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        form.add(new TextField<>("sslPort", new PropertyModel<>(this, "sslFilterConfig.sslPort")));

        // brute force attack
        form.add(
                new CheckBox(
                        "bfEnabled",
                        new PropertyModel<>(this, "config.bruteForcePrevention.enabled")));
        final TextField<Integer> bfMinDelay =
                new TextField<>(
                        "bfMinDelaySeconds",
                        new PropertyModel<>(this, "config.bruteForcePrevention.minDelaySeconds"));
        bfMinDelay.add(RangeValidator.minimum(0));
        form.add(bfMinDelay);
        final TextField<Integer> bfMaxDelay =
                new TextField<>(
                        "bfMaxDelaySeconds",
                        new PropertyModel<>(this, "config.bruteForcePrevention.maxDelaySeconds"));
        bfMaxDelay.add(RangeValidator.minimum(0));
        form.add(bfMaxDelay);

        final TextField<List<String>> netmasks =
                new TextField<List<String>>(
                        "bfWhitelistedNetmasks",
                        new PropertyModel<>(this, "config.bruteForcePrevention.whitelistedMasks")) {

                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return (IConverter<C>) new CommaSeparatedListConverter();
                    }
                };
        netmasks.add(
                (IValidator<List<String>>)
                        validatable -> {
                            List<String> masks = validatable.getValue();
                            for (String mask : masks) {
                                try {
                                    new IpAddressMatcher(mask);
                                } catch (Exception e) {
                                    form.error(
                                            new ParamResourceModel("invalidMask", getPage(), mask)
                                                    .getString());
                                }
                            }
                        });
        form.add(netmasks);
        form.add(
                new AbstractFormValidator() {

                    @Override
                    public void validate(Form<?> form) {
                        Integer min = bfMinDelay.getConvertedInput();
                        Integer max = bfMaxDelay.getConvertedInput();
                        if (max < min) {
                            form.error(
                                    new ParamResourceModel("bfInvalidMinMax", getPage())
                                            .getString());
                        }
                    }

                    @Override
                    public FormComponent<?>[] getDependentFormComponents() {
                        return new FormComponent[] {bfMinDelay, bfMaxDelay};
                    }
                });
        final TextField<Integer> bfMaxBlockedThreads =
                new TextField<>(
                        "bfMaxBlockedThreads",
                        new PropertyModel<>(this, "config.bruteForcePrevention.maxBlockedThreads"));
        bfMaxBlockedThreads.add(RangeValidator.minimum(0));
        form.add(bfMaxBlockedThreads);

        form.add(new AuthenticationFiltersPanel("authFilters"));
        form.add(new HelpLink("authFiltersHelp").setDialog(dialog));

        form.add(new AuthenticationProvidersPanel("authProviders"));
        form.add(new HelpLink("authProvidersHelp").setDialog(dialog));

        form.add(new SecurityFilterChainsPanel("authChains", config));
        form.add(new HelpLink("authChainsHelp").setDialog(dialog));

        form.add(
                authFilterChainPanel =
                        new AuthFilterChainPanel(
                                "filterChain",
                                new PropertyModel<>(form.getModel(), "filterChain")));
        form.add(new HelpLink("filterChainHelp").setDialog(dialog));

        form.add(new AuthenticationChainPanel("providerChain"));
        form.add(new HelpLink("providerChainHelp").setDialog(dialog));

        form.add(
                new SubmitLink("save", form) {
                    @Override
                    public void onSubmit() {
                        try {
                            getSecurityManager()
                                    .saveSecurityConfig(
                                            (SecurityManagerConfig) getForm().getModelObject());
                            getSecurityManager().saveFilter(logoutFilterConfig);
                            getSecurityManager().saveFilter(sslFilterConfig);
                            doReturn();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error saving authentication config", e);
                            error(e);
                        }
                    }
                });
        form.add(
                new Link("cancel") {
                    @Override
                    public void onClick() {
                        doReturn();
                    }
                });
    }

    public void updateChainComponents() {
        form.replace(new SecurityFilterChainsPanel("authChains", config));
    }

    class AuthenticationChainPanel extends FormComponentPanel<SecurityManagerConfig> {

        public AuthenticationChainPanel(String id) {
            super(id, new Model<>());

            add(new AuthenticationChainPalette("authProviderNames"));
        }
    }

    class AuthFilterChainPanel extends FormComponentPanel<GeoServerSecurityFilterChain> {

        DropDownChoice<HTTPMethod> httpMethodChoice;
        TextField<String> urlPathField, chainTestResultField;
        String urlPath, chainTestResult;
        HTTPMethod httpMethod = HTTPMethod.GET;

        public AuthFilterChainPanel(String id, IModel<GeoServerSecurityFilterChain> model) {
            super(id, new Model<>());

            this.setOutputMarkupId(true);

            add(urlPathField = new TextField<>("urlPath", new PropertyModel<>(this, "urlPath")));
            urlPathField.setOutputMarkupId(true);
            urlPathField.add(
                    new OnChangeAjaxBehavior() {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {}
                    });

            add(
                    chainTestResultField =
                            new TextField<>(
                                    "chainTestResult",
                                    new PropertyModel<>(this, "chainTestResult")));
            chainTestResultField.setEnabled(false);
            chainTestResultField.setOutputMarkupId(true);

            add(
                    httpMethodChoice =
                            new DropDownChoice<>(
                                    "httpMethod",
                                    new PropertyModel<>(this, "httpMethod"),
                                    Arrays.asList(HTTPMethod.values())));
            httpMethodChoice.setOutputMarkupId(true);
            httpMethodChoice.setNullValid(false);
            httpMethodChoice.add(
                    new OnChangeAjaxBehavior() {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {}
                    });

            add(
                    new AjaxSubmitLink("chainTest") {
                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            try {
                                String result = "NONE";
                                HttpServletRequest request = getHttpRequest();
                                for (RequestFilterChain chain :
                                        config.getFilterChain().getRequestChains()) {
                                    if (httpMethod != null && urlPath != null) {
                                        if (getProxy().matcherForChain(chain).matches(request)) {
                                            result = chain.getName();
                                            break;
                                        }
                                    }
                                }
                                chainTestResultField.getModel().setObject(result);
                                target.add(chainTestResultField);
                            } catch (Exception e) {
                                error(e);
                                LOGGER.log(Level.WARNING, "Connection error", e);
                                addFeedbackPanels(target);
                            }
                        }

                        protected GeoServerSecurityFilterChainProxy getProxy() {
                            return GeoServerExtensions.bean(
                                    GeoServerSecurityFilterChainProxy.class);
                        }

                        @SuppressWarnings("deprecation")
                        HttpServletRequest getHttpRequest() {
                            return new HttpServletRequest() {
                                @Override
                                public void setCharacterEncoding(String env)
                                        throws UnsupportedEncodingException {}

                                @Override
                                public void setAttribute(String name, Object o) {}

                                @Override
                                public void removeAttribute(String name) {}

                                @Override
                                public boolean isSecure() {
                                    return false;
                                }

                                @Override
                                public int getServerPort() {
                                    return 0;
                                }

                                @Override
                                public String getServerName() {
                                    return null;
                                }

                                @Override
                                public String getScheme() {
                                    return null;
                                }

                                @Override
                                public RequestDispatcher getRequestDispatcher(String path) {
                                    return null;
                                }

                                @Override
                                public int getRemotePort() {
                                    return 0;
                                }

                                @Override
                                public String getRemoteHost() {
                                    return null;
                                }

                                @Override
                                public String getRemoteAddr() {
                                    return null;
                                }

                                @Override
                                public String getRealPath(String path) {
                                    return null;
                                }

                                @Override
                                public BufferedReader getReader() throws IOException {
                                    return null;
                                }

                                @Override
                                public String getProtocol() {
                                    return null;
                                }

                                @Override
                                public String[] getParameterValues(String name) {
                                    return null;
                                }

                                @Override
                                public Enumeration<String> getParameterNames() {
                                    return null;
                                }

                                @Override
                                public Map<String, String[]> getParameterMap() {
                                    return null;
                                }

                                @Override
                                public String getParameter(String name) {
                                    return null;
                                }

                                @Override
                                public Enumeration<Locale> getLocales() {
                                    return null;
                                }

                                @Override
                                public Locale getLocale() {
                                    return null;
                                }

                                @Override
                                public int getLocalPort() {
                                    return 0;
                                }

                                @Override
                                public ServletContext getServletContext() {
                                    return null;
                                }

                                @Override
                                public AsyncContext startAsync() throws IllegalStateException {
                                    return null;
                                }

                                @Override
                                public AsyncContext startAsync(
                                        ServletRequest servletRequest,
                                        ServletResponse servletResponse)
                                        throws IllegalStateException {
                                    return null;
                                }

                                @Override
                                public boolean isAsyncStarted() {
                                    return false;
                                }

                                @Override
                                public boolean isAsyncSupported() {
                                    return false;
                                }

                                @Override
                                public AsyncContext getAsyncContext() {
                                    return null;
                                }

                                @Override
                                public DispatcherType getDispatcherType() {
                                    return null;
                                }

                                @Override
                                public String getLocalName() {
                                    return null;
                                }

                                @Override
                                public String getLocalAddr() {
                                    return null;
                                }

                                @Override
                                public ServletInputStream getInputStream() throws IOException {
                                    return null;
                                }

                                @Override
                                public String getContentType() {
                                    return null;
                                }

                                @Override
                                public int getContentLength() {
                                    return 0;
                                }

                                @Override
                                public long getContentLengthLong() {
                                    return 0;
                                }

                                @Override
                                public String getCharacterEncoding() {
                                    return null;
                                }

                                @Override
                                public Enumeration<String> getAttributeNames() {
                                    return null;
                                }

                                @Override
                                public Object getAttribute(String name) {
                                    return null;
                                }

                                @Override
                                public boolean isUserInRole(String role) {
                                    return false;
                                }

                                @Override
                                public boolean isRequestedSessionIdValid() {
                                    return false;
                                }

                                @Override
                                public boolean isRequestedSessionIdFromUrl() {
                                    return false;
                                }

                                @Override
                                public boolean authenticate(HttpServletResponse response)
                                        throws IOException, ServletException {
                                    return false;
                                }

                                @Override
                                public void login(String username, String password)
                                        throws ServletException {}

                                @Override
                                public void logout() throws ServletException {}

                                @Override
                                public Collection<Part> getParts()
                                        throws IOException, ServletException {
                                    return null;
                                }

                                @Override
                                public Part getPart(String name)
                                        throws IOException, ServletException {
                                    return null;
                                }

                                @Override
                                public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass)
                                        throws IOException, ServletException {
                                    return null;
                                }

                                @Override
                                public boolean isRequestedSessionIdFromURL() {
                                    return false;
                                }

                                @Override
                                public boolean isRequestedSessionIdFromCookie() {
                                    return false;
                                }

                                @Override
                                public Principal getUserPrincipal() {
                                    return null;
                                }

                                @Override
                                public HttpSession getSession(boolean create) {
                                    return null;
                                }

                                @Override
                                public HttpSession getSession() {
                                    return null;
                                }

                                @Override
                                public String changeSessionId() {
                                    return null;
                                }

                                @Override
                                public String getServletPath() {
                                    return "";
                                }

                                @Override
                                public String getRequestedSessionId() {
                                    return null;
                                }

                                @Override
                                public StringBuffer getRequestURL() {
                                    return null;
                                }

                                @Override
                                public String getRequestURI() {
                                    return null;
                                }

                                @Override
                                public String getRemoteUser() {
                                    return null;
                                }

                                @Override
                                public String getQueryString() {
                                    if (urlPath == null || urlPath.indexOf("?") == -1) {
                                        return null;
                                    } else {
                                        return urlPath.substring(urlPath.indexOf("?") + 1);
                                    }
                                }

                                @Override
                                public String getPathTranslated() {
                                    return null;
                                }

                                @Override
                                public String getPathInfo() {
                                    if (urlPath == null || urlPath.indexOf("?") == -1) {
                                        return urlPath;
                                    } else {
                                        return urlPath.substring(0, urlPath.indexOf("?"));
                                    }
                                }

                                @Override
                                public String getMethod() {
                                    return httpMethod.toString();
                                }

                                @Override
                                public int getIntHeader(String name) {
                                    return 0;
                                }

                                @Override
                                public Enumeration<String> getHeaders(String name) {
                                    return null;
                                }

                                @Override
                                public Enumeration<String> getHeaderNames() {
                                    return null;
                                }

                                @Override
                                public String getHeader(String name) {
                                    return null;
                                }

                                @Override
                                public long getDateHeader(String name) {
                                    return 0;
                                }

                                @Override
                                public Cookie[] getCookies() {
                                    return null;
                                }

                                @Override
                                public String getContextPath() {
                                    return null;
                                }

                                @Override
                                public String getAuthType() {
                                    return null;
                                }
                            };
                        }
                    }.setDefaultFormProcessing(false));
        }
    }
}

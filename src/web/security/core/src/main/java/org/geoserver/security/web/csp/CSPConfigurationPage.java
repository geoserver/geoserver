/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.csp;

import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serial;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.security.csp.CSPConfiguration;
import org.geoserver.security.csp.CSPHeaderDAO;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;

/** Page for configuring the Content-Security-Policy HTTP response header. */
public class CSPConfigurationPage extends GeoServerSecuredPage {

    @Serial
    private static final long serialVersionUID = -5935887226717780789L;

    private TextArea<String> testResultField;

    private CSPConfiguration config;

    private String testUrl = "";

    public CSPConfigurationPage() throws IOException {
        this.config = new CSPConfiguration(getCSPHeaderDAO().getConfig());
        IModel<CSPConfiguration> model = new Model<>(this.config);
        Form<CSPConfiguration> form = new Form<>("form", new CompoundPropertyModel<>(model));
        form.add(new CheckBox("enabled", new PropertyModel<>(model, "enabled")));
        form.add(new CheckBox("reportOnly", new PropertyModel<>(model, "reportOnly")));
        form.add(new CheckBox("allowOverride", new PropertyModel<>(model, "allowOverride")));
        form.add(new CheckBox("injectProxyBase", new PropertyModel<>(model, "injectProxyBase")));
        form.add(new TextArea<>("remoteResources", new PropertyModel<>(model, "remoteResources")));
        form.add(new TextArea<>("formAction", new PropertyModel<>(model, "formAction")));
        form.add(new TextArea<>("frameAncestors", new PropertyModel<>(model, "frameAncestors")));
        form.add(new CSPPolicyPanel("policies", this.config));
        form.add(new TextArea<>("testUrl", new PropertyModel<>(this, "testUrl")));
        form.add(new AjaxSubmitLink("testLink") {
            @Serial
            private static final long serialVersionUID = 1700932575669734348L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                testContentSecurityPolicy(target);
            }
        });
        this.testResultField = new TextArea<>("testResult", new Model<>(""));
        form.add(this.testResultField.setOutputMarkupId(true).setEnabled(false));
        form.add(new SubmitLink("save", form) {
            @Serial
            private static final long serialVersionUID = -8900006356449150190L;

            @Override
            public void onSubmit() {
                saveConfiguration(true);
            }
        });
        form.add(new Button("apply") {
            @Serial
            private static final long serialVersionUID = -3327108081898697618L;

            @Override
            public void onSubmit() {
                saveConfiguration(false);
            }
        });
        form.add(new Button("cancel") {
            @Serial
            private static final long serialVersionUID = 7567566240358171893L;

            @Override
            public void onSubmit() {
                doReturn();
            }
        });
        add(form);
    }

    private CSPHeaderDAO getCSPHeaderDAO() {
        return getGeoServerApplication().getBeanOfType(CSPHeaderDAO.class);
    }

    /**
     * Saves the current configuration to the data directory.
     *
     * @param doReturn true to return to the home page
     */
    private void saveConfiguration(boolean doReturn) {
        try {
            Preconditions.checkArgument(
                    !this.config.getFormAction().contains("'none'"),
                    "form-action containing 'none' is not allowed here");
            getCSPHeaderDAO().setConfig(new CSPConfiguration(this.config));
            if (doReturn) {
                doReturn();
            }
        } catch (Exception e) {
            error(e);
        }
    }

    /**
     * Runs the input test URL against the current CSP configuration to determine the header value.
     *
     * @param target the request target
     * @throws IOException if the test URL is invalid
     */
    private void testContentSecurityPolicy(AjaxRequestTarget target) {
        String result = "Enter URL";
        try {
            if (StringUtils.isNotBlank(this.testUrl)) {
                HttpServletRequest request = getHttpRequest(new URL(this.testUrl.trim()));
                this.config.parseFilters();
                result = CSPHeaderDAO.getContentSecurityPolicy(this.config, request, true);
            }
        } catch (Exception e) {
            result = "ERROR";
            error(e);
            addFeedbackPanels(target);
        }
        target.add(this.testResultField.setDefaultModelObject(result));
    }

    /**
     * Builds a mock HTTP request to test the Content Security Policy configuration.
     *
     * @param url the test URL
     * @return the mock HTTP request
     * @throws IOException if the test URL is invalid
     */
    @SuppressWarnings("deprecation")
    private static HttpServletRequest getHttpRequest(URL url) throws IOException {
        String host = url.getHost() + (url.getPort() == -1 ? "" : (':' + url.getPort()));
        String path = URLDecoder.decode(url.getPath(), "UTF-8");
        String context = GeoServerApplication.get().servletRequest().getContextPath();
        String pathInfo = path.startsWith(context) ? path.substring(context.length()) : path;
        Map<String, List<String>> listMap = new LinkedHashMap<>();
        URLEncodedUtils.parse(url.getQuery(), StandardCharsets.UTF_8)
                .forEach(p -> listMap.computeIfAbsent(p.getName(), x -> new ArrayList<>())
                        .add(p.getValue()));
        Map<String, String[]> parameterMap = new LinkedHashMap<>();
        listMap.forEach((k, v) -> parameterMap.put(k, v.toArray(new String[v.size()])));
        return new HttpServletRequest() {

            @Override
            public String getHeader(String name) {
                return HttpHeaders.HOST.equals(name) ? host : null;
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return Collections.unmodifiableMap(parameterMap);
            }

            @Override
            public String getPathInfo() {
                return pathInfo;
            }

            @Override
            public String getQueryString() {
                return url.getQuery();
            }

            @Override
            public String getRequestURI() {
                return url.getPath();
            }

            @Override
            public String getScheme() {
                return url.getProtocol();
            }

            @Override
            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AsyncContext startAsync() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setCharacterEncoding(String env) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setAttribute(String name, Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeAttribute(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isSecure() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isAsyncSupported() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isAsyncStarted() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ServletContext getServletContext() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getServerPort() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getServerName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getRemotePort() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getRemoteHost() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getRemoteAddr() {
                throw new UnsupportedOperationException();
            }

            @Override
            public BufferedReader getReader() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getProtocol() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String[] getParameterValues(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Enumeration<String> getParameterNames() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getParameter(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Enumeration<Locale> getLocales() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Locale getLocale() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getLocalPort() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getLocalName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getLocalAddr() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ServletInputStream getInputStream() {
                throw new UnsupportedOperationException();
            }

            @Override
            public DispatcherType getDispatcherType() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getRequestId() {
                return "";
            }

            @Override
            public String getProtocolRequestId() {
                return "";
            }

            @Override
            public ServletConnection getServletConnection() {
                return null;
            }

            @Override
            public String getContentType() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getContentLength() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getContentLengthLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getCharacterEncoding() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object getAttribute(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AsyncContext getAsyncContext() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void logout() throws ServletException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void login(String username, String password) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isUserInRole(String role) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Principal getUserPrincipal() {
                throw new UnsupportedOperationException();
            }

            @Override
            public HttpSession getSession(boolean create) {
                throw new UnsupportedOperationException();
            }

            @Override
            public HttpSession getSession() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String changeSessionId() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getServletPath() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getRequestedSessionId() {
                throw new UnsupportedOperationException();
            }

            @Override
            public StringBuffer getRequestURL() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getRemoteUser() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getPathTranslated() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Part> getParts() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Part getPart(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getIntHeader(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getDateHeader(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Cookie[] getCookies() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getContextPath() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAuthType() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean authenticate(HttpServletResponse response) {
                throw new UnsupportedOperationException();
            }
        };
    }
}

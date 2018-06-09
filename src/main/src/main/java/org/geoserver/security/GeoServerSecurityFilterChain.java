/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The security filter filter chain.
 *
 * <p>The content of {code antPatterns} must be equal to the keys of {code filterMap}. #
 *
 * <p>The order of {code antPatterns} determines the order of ant pattern matching used by
 * GeoServerSecurityFilterChainProxy.
 *
 * @author christian
 */
public class GeoServerSecurityFilterChain implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    List<RequestFilterChain> requestChains = new ArrayList<RequestFilterChain>();

    /*
     * chain patterns
     */
    public static final String WEB_CHAIN = "/web/**";
    public static final String FORM_LOGIN_CHAIN =
            "/j_spring_security_check,/j_spring_security_check/,/login";
    public static final String FORM_LOGOUT_CHAIN =
            "/j_spring_security_logout,/j_spring_security_logout/,/logout";
    public static final String REST_CHAIN = "/rest/**";
    public static final String GWC_WEB_CHAIN = "/gwc/rest/web/**";
    public static final String GWC_REST_CHAIN = "/gwc/rest/**";
    public static final String DEFAULT_CHAIN = "/**";

    /*
     * filter names
     */
    public static final String SECURITY_CONTEXT_ASC_FILTER = "contextAsc";
    public static final String SECURITY_CONTEXT_NO_ASC_FILTER = "contextNoAsc";

    public static final String ROLE_FILTER = "roleFilter";
    public static final String SSL_FILTER = "sslFilter";

    public static final String FORM_LOGIN_FILTER = "form";
    public static final String FORM_LOGOUT_FILTER = "formLogout";

    public static final String REMEMBER_ME_FILTER = "rememberme";

    public static final String ANONYMOUS_FILTER = "anonymous";

    public static final String BASIC_AUTH_FILTER = "basic";
    // public static final String BASIC_AUTH_NO_REMEMBER_ME_FILTER = "basicAuthNrm";

    public static final String DYNAMIC_EXCEPTION_TRANSLATION_FILTER = "exception";
    public static final String GUI_EXCEPTION_TRANSLATION_FILTER = "guiException";

    public static final String FILTER_SECURITY_INTERCEPTOR = "interceptor";
    public static final String FILTER_SECURITY_REST_INTERCEPTOR = "restInterceptor";

    // standard chain names as constant
    public static final String WEB_CHAIN_NAME = "web";
    public static final String WEB_LOGIN_CHAIN_NAME = "webLogin";
    public static final String WEB_LOGOUT_CHAIN_NAME = "webLogout";
    public static final String REST_CHAIN_NAME = "rest";
    public static final String GWC_CHAIN_NAME = "gwc";
    public static final String DEFAULT_CHAIN_NAME = "default";

    static HtmlLoginFilterChain WEB = new HtmlLoginFilterChain(WEB_CHAIN, GWC_WEB_CHAIN);

    static {
        WEB.setName(WEB_CHAIN_NAME);
        WEB.setFilterNames(REMEMBER_ME_FILTER, FORM_LOGIN_FILTER, ANONYMOUS_FILTER);
        WEB.setAllowSessionCreation(true);
    }

    private static ConstantFilterChain WEB_LOGIN = new ConstantFilterChain(FORM_LOGIN_CHAIN);

    static {
        WEB_LOGIN.setName(WEB_LOGIN_CHAIN_NAME);
        WEB_LOGIN.setFilterNames(FORM_LOGIN_FILTER);
        WEB_LOGIN.setAllowSessionCreation(true);
    }

    private static LogoutFilterChain WEB_LOGOUT = new LogoutFilterChain(FORM_LOGOUT_CHAIN);

    static {
        WEB_LOGOUT.setName(WEB_LOGOUT_CHAIN_NAME);
        WEB_LOGOUT.setFilterNames(FORM_LOGOUT_FILTER);
    }

    private static ServiceLoginFilterChain REST = new ServiceLoginFilterChain(REST_CHAIN);

    static {
        REST.setName(REST_CHAIN_NAME);
        REST.setFilterNames(BASIC_AUTH_FILTER, ANONYMOUS_FILTER);
        REST.setInterceptorName(FILTER_SECURITY_REST_INTERCEPTOR);
    }

    private static ServiceLoginFilterChain GWC = new ServiceLoginFilterChain(GWC_REST_CHAIN);

    static {
        GWC.setName(GWC_CHAIN_NAME);
        GWC.setFilterNames(BASIC_AUTH_FILTER);
        GWC.setInterceptorName(FILTER_SECURITY_REST_INTERCEPTOR);
    }

    private static ServiceLoginFilterChain DEFAULT = new ServiceLoginFilterChain(DEFAULT_CHAIN);

    static {
        DEFAULT.setName(DEFAULT_CHAIN_NAME);
        DEFAULT.setFilterNames(BASIC_AUTH_FILTER, ANONYMOUS_FILTER);
    }

    private static List<RequestFilterChain> INITIAL = new ArrayList<RequestFilterChain>();

    static {
        INITIAL.add(WEB);
        INITIAL.add(WEB_LOGIN);
        INITIAL.add(WEB_LOGOUT);
        INITIAL.add(REST);
        INITIAL.add(GWC);
        INITIAL.add(DEFAULT);
    }

    public GeoServerSecurityFilterChain() {
        requestChains = new ArrayList<RequestFilterChain>();
    }

    /** Constructor cloning all collections */
    public GeoServerSecurityFilterChain(List<RequestFilterChain> requestChains) {
        this.requestChains = requestChains;
    }

    /** Constructor cloning all collections */
    public GeoServerSecurityFilterChain(GeoServerSecurityFilterChain other) {
        this.requestChains = new ArrayList<RequestFilterChain>(other.getRequestChains());
    }

    /** Create the initial {@link GeoServerSecurityFilterChain} */
    public static GeoServerSecurityFilterChain createInitialChain() {
        return new GeoServerSecurityFilterChain(new ArrayList<RequestFilterChain>(INITIAL));
    }

    public void postConfigure(GeoServerSecurityManager secMgr) {
        // TODO, Justin
        // Not sure if this is correct, if it is, you can add the constant chain
        // for the root user login
        for (GeoServerSecurityProvider p : secMgr.lookupSecurityProviders()) {
            p.configureFilterChain(this);
        }
    }

    public static RequestFilterChain lookupRequestChainByName(
            String name, GeoServerSecurityManager secMgr) {
        // this is kind of a hack but we create an initial filter chain and run it through the
        // security provider extension points to get an actual final chain, and then look through
        // the elements for a matching name
        GeoServerSecurityFilterChain filterChain = createInitialChain();
        filterChain.postConfigure(secMgr);

        for (RequestFilterChain requestChain : filterChain.getRequestChains()) {
            if (requestChain.getName().equals(name)) {
                return requestChain;
            }
        }

        return null;
    }

    public static RequestFilterChain lookupRequestChainByPattern(
            String pattern, GeoServerSecurityManager secMgr) {
        // this is kind of a hack but we create an initial filter chain and run it through the
        // security provider extension points to get an actual final chain, and then look through
        // the elements for a matching name
        GeoServerSecurityFilterChain filterChain = createInitialChain();
        filterChain.postConfigure(secMgr);

        for (RequestFilterChain requestChain : filterChain.getRequestChains()) {
            if (requestChain.getPatterns().contains(pattern)) {
                return requestChain;
            }
        }

        return null;
    }

    public List<RequestFilterChain> getRequestChains() {
        return requestChains;
    }

    public List<RequestFilterChain> getVariableRequestChains() {
        List<RequestFilterChain> result = new ArrayList<RequestFilterChain>();
        for (RequestFilterChain chain : getRequestChains())
            if (chain.isConstant() == false) result.add(chain);
        return result;
    }

    public RequestFilterChain getRequestChainByName(String name) {
        for (RequestFilterChain requestChain : requestChains) {
            if (requestChain.getName().equals(name)) {
                return requestChain;
            }
        }
        return null;
    }

    /**
     * Inserts a filter as the first of the filter list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertFirst(String pattern, String filterName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName);
        if (requestChain == null) {
            return false;
        }
        requestChain.getFilterNames().add(0, filterName);
        return false;
    }

    /**
     * Inserts a filter as the last of the filter list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertLast(String pattern, String filterName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName);
        if (requestChain == null) {
            return false;
        }

        return requestChain.getFilterNames().add(filterName);
    }

    /**
     * Inserts a filter as before another in the list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertBefore(String pattern, String filterName, String positionName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName);
        if (requestChain == null) {
            return false;
        }

        List<String> filterNames = requestChain.getFilterNames();
        int index = filterNames.indexOf(positionName);
        if (index == -1) {
            return false;
        }

        filterNames.add(index, filterName);
        return true;
    }

    /**
     * Inserts a filter as after another in the list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertAfter(String pattern, String filterName, String positionName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName);
        if (requestChain == null) {
            return false;
        }

        List<String> filterNames = requestChain.getFilterNames();
        int index = filterNames.indexOf(positionName);
        if (index == -1) {
            return false;
        }

        filterNames.add(index + 1, filterName);
        return true;
    }

    public RequestFilterChain find(String pattern) {
        return requestChain(pattern);
    }

    /**
     * Get a list of patterns having the filter in their chain. If includeAll is false, only
     * authentication filters are searched
     */
    public List<String> patternsForFilter(String filterName, boolean includeAll) {
        List<String> result = new ArrayList<String>();
        for (RequestFilterChain requestChain : requestChains) {
            List<String> filterNames =
                    includeAll
                            ? requestChain.getCompiledFilterNames()
                            : requestChain.getFilterNames();
            if (filterNames.contains(filterName)) {
                result.addAll(requestChain.getPatterns());
            }
        }
        return result;
    }

    /** Get the filters for the specified pattern. */
    public List<String> filtersFor(String pattern) {
        RequestFilterChain requestChain = requestChain(pattern);
        if (requestChain == null) {
            return Collections.EMPTY_LIST;
        }

        return new ArrayList(requestChain.getFilterNames());
    }

    public boolean removeForPattern(String pattern) {
        RequestFilterChain requestChain = requestChain(pattern);
        if (requestChain != null) {
            return requestChains.remove(requestChain);
        }
        return false;
    }

    /** Removes a filter by name from all filter request chains. */
    public boolean remove(String filterName) {
        boolean removed = false;
        for (RequestFilterChain requestChain : requestChains) {
            removed |= requestChain.getFilterNames().remove(filterName);
        }
        return removed;
    }

    RequestFilterChain findAndCheck(String pattern, String filterName) {
        RequestFilterChain requestChain = requestChain(pattern);
        if (requestChain == null) {
            return null;
        }

        if (requestChain.getFilterNames().contains(filterName)) {
            // JD: perhaps we should move it
            return null;
        }

        return requestChain;
    }

    RequestFilterChain requestChain(String pattern) {
        for (RequestFilterChain requestChain : requestChains) {
            if (requestChain.getPatterns().contains(pattern)) {
                return requestChain;
            }
        }
        return null;
    }
}

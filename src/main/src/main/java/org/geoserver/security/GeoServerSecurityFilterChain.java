/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The security filter filter chain.
 * <p>
 * The content of {@link #antPatterns} must be equal to the keys of {@link #filterMap}.
 * </p>
 * <p>
 * The order of {@link #antPatterns} determines the order of ant pattern matching used by 
 * GeoServerSecurityFilterChainProxy.
 * </p>
 * @author christian
 *
 */
public class GeoServerSecurityFilterChain implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private List<String> antPatterns;
    private HashMap<String,List<String>>  filterMap; 

    public static enum FilterChain {

        WEB(WEB_CHAIN, GWC_WEB_CHAIN) {
            @Override
            List<String> createInitial() {
                return list(SECURITY_CONTEXT_ASC_FILTER, REMEMBER_ME_FILTER, ANONYMOUS_FILTER,
                    GUI_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR);
            }
        },
        WEB_LOGIN(FORM_LOGIN_CHAIN) {
            @Override
            List<String> createInitial() {
                return list(SECURITY_CONTEXT_ASC_FILTER, FORM_LOGIN_FILTER);
            }
        },
        WEB_LOGOUT(FORM_LOGOUT_CHAIN) {
            @Override
            List<String> createInitial() {
                return list(SECURITY_CONTEXT_ASC_FILTER, FORM_LOGOUT_FILTER);
            }
        },
        REST(REST_CHAIN) {
            @Override
            List<String> createInitial() {
                return list(SECURITY_CONTEXT_NO_ASC_FILTER, BASIC_AUTH_FILTER, ANONYMOUS_FILTER, 
                    DYNAMIC_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_REST_INTERCEPTOR);
            }
        }, 
        GWC(GWC_REST_CHAIN) {
            @Override
            List<String> createInitial() {
                return list(SECURITY_CONTEXT_NO_ASC_FILTER, BASIC_AUTH_FILTER, 
                    DYNAMIC_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_REST_INTERCEPTOR);
            }
        },
        DEFAULT(DEFAULT_CHAIN) {
            @Override
            List<String> createInitial() {
                return list(SECURITY_CONTEXT_NO_ASC_FILTER, BASIC_AUTH_FILTER, ANONYMOUS_FILTER, 
                    DYNAMIC_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR);
            }
        };

        List<String> patterns;

        FilterChain(String... patterns) {
            this.patterns = Arrays.asList(patterns);
        }

        List<String> getPatterns() {
            return patterns;
        }

        abstract List<String> createInitial();
    }

    /*
     * chain patterns 
     */
    public static final String WEB_CHAIN = "/web/**";
    public static final String FORM_LOGIN_CHAIN = "/j_spring_security_check,/j_spring_security_check/"; 
    public static final String FORM_LOGOUT_CHAIN = "/j_spring_security_logout,/j_spring_security_logout/";
    public static final String REST_CHAIN = "/rest/**";
    public static final String GWC_WEB_CHAIN = "/gwc/rest/web/**";
    public static final String GWC_REST_CHAIN = "/gwc/rest/**"; 
    public static final String DEFAULT_CHAIN = "/**"; 
    
    /*
     * filter names
     */
    public static final String SECURITY_CONTEXT_ASC_FILTER = "contextAsc";
    public static final String SECURITY_CONTEXT_NO_ASC_FILTER = "contextNoAsc";
    
    public static final String FORM_LOGIN_FILTER = "form";
    public static final String FORM_LOGOUT_FILTER = "formLogout";

    public static final String REMEMBER_ME_FILTER = "rememberme";

    public static final String ANONYMOUS_FILTER = "anonymous";

    public static final String BASIC_AUTH_FILTER = "basic";
    //public static final String BASIC_AUTH_NO_REMEMBER_ME_FILTER = "basicAuthNrm";

    public static final String DYNAMIC_EXCEPTION_TRANSLATION_FILTER = "exception";
    public static final String GUI_EXCEPTION_TRANSLATION_FILTER = "guiException";

    public static final String FILTER_SECURITY_INTERCEPTOR = "interceptor";
    public static final String FILTER_SECURITY_REST_INTERCEPTOR = "restInterceptor";


    public GeoServerSecurityFilterChain() {
        antPatterns = new ArrayList<String>();
        filterMap = new HashMap<String,List<String>>();   
    }
        
    /**
     * Constructor cloning all collections
     */
    public GeoServerSecurityFilterChain(GeoServerSecurityFilterChain other) {
        this.antPatterns=new ArrayList<String>(other.antPatterns);
        this.filterMap=new HashMap<String,List<String>>();
        for (String pattern: other.filterMap.keySet()) {
            this.filterMap.put(pattern, new  ArrayList<String>(other.getFilterMap().get(pattern)));
        }
    }

    /**
     * Create the initial {@link GeoServerSecurityFilterChain} 
     * 
     * @return
     */
    public static GeoServerSecurityFilterChain createInitialChain() {
        GeoServerSecurityFilterChain chain = new GeoServerSecurityFilterChain();

        //gather up all the different patterns
        List<String> patterns = new ArrayList();
        for (FilterChain fc : FilterChain.values()) {
            patterns.addAll(fc.getPatterns());

            for (String p : fc.getPatterns()) {
                chain.filterMap.put(p, fc.createInitial());
            }
        }
        chain.setAntPatterns(patterns);
        return chain;
    }

    /**
     * Helper method to create a list
     */
    static List<String> list(String... filterName) {
        return new ArrayList<String>(Arrays.asList(filterName));
    }
    
    public List<String> getAntPatterns() {
        return antPatterns;
    }

    public void setAntPatterns(List<String> antPatterns) {
        this.antPatterns = antPatterns;
    }

    public HashMap<String, List<String>> getFilterMap() {
        return filterMap;
    }

    public void setFilterMap(HashMap<String, List<String>> filterMap) {
        this.filterMap = filterMap;
    }

    /**
     * Convenience method, insert filter name at
     * first position for the given pattern
     * 
     * returns true on success
     * 
     * @param pattern
     * @param filterName
     * @return
     */
    public boolean insertFirst(String pattern, String filterName) {
        List<String> filterNames = filterMap.get(pattern);
        if (filterNames==null) return false;
        filterNames.add(0,filterName);
        return true;
    }
    
    /**
     * Convenience method, insert filter name at
     * last position for the given pattern
     * 
     * returns true on success
     * 
     * @param pattern
     * @param filterName
     * @return
     */
    public boolean insertLast(String pattern, String filterName) {
        List<String> filterNames = filterMap.get(pattern);
        if (filterNames==null) return false;
        filterNames.add(filterName);
        return true;
    }

    /**
     * Convenience method, insert filter name before
     * filter named positionName for the given pattern
     * 
     * returns true on success
     * 
     * @param pattern
     * @param filterName
     * @param poslitionName
     * @return
     */
    public boolean insertBefore(String pattern, String filterName, String positionName) {
        List<String> filterNames = filterMap.get(pattern);
        if (filterNames==null) return false;
        int index = filterNames.indexOf(positionName);
        if (index==-1) return false;
        filterNames.add(index,filterName);
        return true;
    }
    
    /**
     * Convenience method, insert filter name after
     * filter named positionName for the given pattern
     * 
     * returns true on success
     * 
     * @param pattern
     * @param filterName
     * @param poslitionName
     * @return
     */
    public boolean insertAfter(String pattern, String filterName, String positionName) {
        List<String> filterNames = filterMap.get(pattern);
        if (filterNames==null) return false;
        int index = filterNames.indexOf(positionName);
        if (index==-1) return false;
        filterNames.add(index+1,filterName);
        return true;
    }

    /**
     * Get a list of patterns having the filter in their chain
     * 
     * @param filterName
     * @return
     */
    public List<String> patternsContainingFilter(String filterName) {
        List<String> result = new ArrayList<String>();
        for (String pattern: antPatterns) {
            if (filterMap.get(pattern).contains(filterName)) {
                result.add(pattern);
            }
        }
        return result;
    }

    public List<String> filtersFor(String pattern) {
        if (!filterMap.containsKey(pattern)) {
            return Collections.EMPTY_LIST;
        }

        return new ArrayList(filterMap.get(pattern));
    }

    public List<String> filtersFor(FilterChain cat) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String p : cat.patterns) {
            result.addAll(filtersFor(p));
        }
        return new ArrayList(result);
    }

    public boolean updateAuthFilters(FilterChain cat, List<String> filterNames) {
        boolean update = true;
        HashMap<String,ArrayList<String>> tmp = new HashMap<String,ArrayList<String>>();

        for (String p : cat.patterns) {
            ArrayList<String> list = new ArrayList(filterMap.get(p));
            tmp.put(p, list);

            int i = list.indexOf(SECURITY_CONTEXT_ASC_FILTER);
            i = i != -1 ? i : list.indexOf(SECURITY_CONTEXT_NO_ASC_FILTER);

            int j = list.indexOf(DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
            j = j != -1 ? j : list.indexOf(GUI_EXCEPTION_TRANSLATION_FILTER);

            if (i == -1 || j == -1) {
                update = false;
            }
            else {
                ArrayList<String> sub = new ArrayList(list.subList(i+1, j));
                list.removeAll(sub);
                list.addAll(i+1, filterNames);
            }
        }

        if (update) {
            filterMap.putAll(tmp);
            return true;
        }

        return false;
    }
}

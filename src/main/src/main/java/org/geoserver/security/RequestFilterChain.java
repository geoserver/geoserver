/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

/**
 * List of filters applied to a pattern matching a set of requests.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class RequestFilterChain implements Serializable, Cloneable {

    /** */
    private static final long serialVersionUID = 1L;

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** The unique name of the chain */
    String name;

    /** The ANT patterns for this chain */
    List<String> patterns;

    /** The filter names */
    List<String> filterNames;

    /** Chain disabled ? */
    boolean disabled;

    /** Is this chain allowed to create an HTTP session ? */
    boolean allowSessionCreation;

    /** Does this chain accept SSL requests only */
    boolean requireSSL;

    /** Is this chain matching individual HTTP methods */
    boolean matchHTTPMethod;

    /** The set of HTTP methods to match against if {@link #matchHTTPMethod} is <code>true</code> */
    Set<HTTPMethod> httpMethods;

    String roleFilterName;

    public RequestFilterChain(String... patterns) {
        this.patterns = new ArrayList<String>(Arrays.asList((patterns)));
        filterNames = new ArrayList<String>();
        httpMethods = new TreeSet<HTTPMethod>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public List<String> getFilterNames() {
        return filterNames;
    }

    public abstract boolean isConstant();

    public void setFilterNames(String... filterNames) {
        setFilterNames(new ArrayList<String>(Arrays.asList(filterNames)));
    }

    public void setFilterNames(List<String> filterNames) {
        this.filterNames = filterNames;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(patterns).append(":").append(filterNames);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isConstant() ? 1231 : 1237);
        result = prime * result + (isAllowSessionCreation() ? 17 : 19);
        result = prime * result + (isDisabled() ? 23 : 29);
        result = prime * result + (isRequireSSL() ? 31 : 37);
        result = prime * result + (isMatchHTTPMethod() ? 41 : 49);
        result = prime * ((roleFilterName == null) ? 1 : roleFilterName.hashCode());
        result = prime * result + ((httpMethods == null) ? 0 : httpMethods.hashCode());
        result = prime * result + ((filterNames == null) ? 0 : filterNames.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((patterns == null) ? 0 : patterns.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RequestFilterChain other = (RequestFilterChain) obj;

        if (this.roleFilterName == null && other.roleFilterName != null) return false;
        if (this.roleFilterName != null
                && this.roleFilterName.equals(other.roleFilterName) == false) return false;

        if (this.isAllowSessionCreation() != other.isAllowSessionCreation()) return false;
        if (this.isDisabled() != other.isDisabled()) return false;
        if (this.isRequireSSL() != other.isRequireSSL()) return false;
        if (this.isMatchHTTPMethod() != other.isMatchHTTPMethod()) return false;

        if (filterNames == null) {
            if (other.filterNames != null) return false;
        } else if (!filterNames.equals(other.filterNames)) return false;

        if (httpMethods == null) {
            if (other.httpMethods != null) return false;
        } else if (!httpMethods.equals(other.httpMethods)) return false;

        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (patterns == null) {
            if (other.patterns != null) return false;
        } else if (!patterns.equals(other.patterns)) return false;
        return true;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RequestFilterChain chain = (RequestFilterChain) super.clone();
        chain.setFilterNames(new ArrayList<String>(filterNames));
        chain.patterns = new ArrayList<String>(patterns);
        chain.httpMethods = new TreeSet<HTTPMethod>(httpMethods);
        return chain;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<String> getCompiledFilterNames() {
        if (isDisabled() == true) return Collections.emptyList();

        List<String> result = new ArrayList<String>();

        if (isRequireSSL()) result.add(GeoServerSecurityFilterChain.SSL_FILTER);

        if (isAllowSessionCreation())
            result.add(GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER);
        else result.add(GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER);

        if (StringUtils.hasLength(getRoleFilterName())) result.add(getRoleFilterName());

        createCompiledFilterList(result);
        return result;
    }

    void createCompiledFilterList(List<String> list) {
        list.addAll(getFilterNames());
    }

    public boolean isAllowSessionCreation() {
        return allowSessionCreation;
    }

    public void setAllowSessionCreation(boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }

    public boolean isRequireSSL() {
        return requireSSL;
    }

    public void setRequireSSL(boolean requireSSL) {
        this.requireSSL = requireSSL;
    }

    public boolean isMatchHTTPMethod() {
        return matchHTTPMethod;
    }

    public void setMatchHTTPMethod(boolean matchHTTPMethod) {
        this.matchHTTPMethod = matchHTTPMethod;
    }

    public Set<HTTPMethod> getHttpMethods() {
        return httpMethods;
    }

    public void setHttpMethods(Set<HTTPMethod> httpMethods) {
        this.httpMethods = httpMethods;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public String getRoleFilterName() {
        return roleFilterName;
    }

    public void setRoleFilterName(String roleFilterName) {
        this.roleFilterName = roleFilterName;
    }

    public abstract boolean canBeRemoved();
}

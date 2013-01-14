/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

/**
 * List of filters applied to a pattern matching a set of requests.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class RequestFilterChain implements Serializable,Cloneable {

    /**
     * 
     */        
    private static final long serialVersionUID = 1L;
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    
    String name;
    List<String> patterns, filterNames;
    boolean disabled,allowSessionCreation;

    public RequestFilterChain(String... patterns) {
        this.patterns = new ArrayList<String>(Arrays.asList((patterns)));
        filterNames = new ArrayList<String>();
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
        result = prime * result
                + ((filterNames == null) ? 0 : filterNames.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((patterns == null) ? 0 : patterns.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RequestFilterChain other = (RequestFilterChain) obj;
        if (this.isAllowSessionCreation() != other.isAllowSessionCreation())
            return false;
        if (filterNames == null) {
            if (other.filterNames != null)
                return false;
        } else if (!filterNames.equals(other.filterNames))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (patterns == null) {
            if (other.patterns != null)
                return false;
        } else if (!patterns.equals(other.patterns))
            return false;
        return true;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RequestFilterChain chain =  (RequestFilterChain) super.clone();
        chain.setFilterNames(new ArrayList<String>(filterNames));
        chain.patterns=new ArrayList<String>(patterns);
        return chain;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<String> getCompiledFilterNames() {
        if (isDisabled()==true)
            return Collections.emptyList();
        List<String> result = new ArrayList<String>();
        if (isAllowSessionCreation())
            result.add(GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER);
         else   
            result.add(GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER);
        
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
}

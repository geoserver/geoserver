/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.geoserver.security.GeoServerSecurityFilterChain.*;

/**
 * List of filters applied to a pattern matching a set of requests.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class RequestFilterChain implements Serializable {

    String name;
    List<String> patterns, filterNames;
    boolean constant;

    public RequestFilterChain(String... patterns) {
        this.patterns = new ArrayList(Arrays.asList((patterns)));
        filterNames = new ArrayList();
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

    public void setFilterNames(String... filterNames) {
        setFilterNames(new ArrayList<String>(Arrays.asList(filterNames)));
    }

    public void setFilterNames(List<String> filterNames) {
        this.filterNames = filterNames;
    }

    public boolean isConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }
    
    public boolean updateAuthFilters(List<String> newFilterNames) {
        int i = filterNames.indexOf(SECURITY_CONTEXT_ASC_FILTER);
        i = i != -1 ? i : filterNames.indexOf(SECURITY_CONTEXT_NO_ASC_FILTER);

        int j = filterNames.indexOf(DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
        j = j != -1 ? j : filterNames.indexOf(GUI_EXCEPTION_TRANSLATION_FILTER);

        if (i == -1 || j == -1) {
            return false;
        }

        ArrayList<String> sub = new ArrayList(filterNames.subList(i+1,j));
        filterNames.removeAll(sub);
        filterNames.addAll(i+1, newFilterNames);

        return true;
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
        result = prime * result + (constant ? 1231 : 1237);
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
        if (constant != other.constant)
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

    
}

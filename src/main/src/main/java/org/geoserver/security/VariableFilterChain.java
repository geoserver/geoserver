/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

/**
 * Filter chains of this type can be modified
 *
 * @author christian
 */
public abstract class VariableFilterChain extends RequestFilterChain {

    String interceptorName, exceptionTranslationName;
    /** */
    private static final long serialVersionUID = 1L;

    public VariableFilterChain(String... patterns) {
        super(patterns);
        interceptorName = GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR;
        exceptionTranslationName =
                GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER;
    }

    public boolean isConstant() {
        return false;
    }

    /** list the filter names which can be added to this chain */
    public abstract SortedSet<String> listFilterCandidates(GeoServerSecurityManager m)
            throws IOException;

    @Override
    void createCompiledFilterList(List<String> list) {
        list.addAll(getFilterNames());
        list.add(exceptionTranslationName);
        list.add(interceptorName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VariableFilterChain == false) return false;

        VariableFilterChain other = (VariableFilterChain) obj;
        if (this.interceptorName == null && other.interceptorName != null) return false;
        if (this.interceptorName != null
                && this.interceptorName.equals(other.interceptorName) == false) return false;

        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = hash * ((interceptorName == null) ? 1 : interceptorName.hashCode());
        return hash;
    }

    public String getInterceptorName() {
        return interceptorName;
    }

    public void setInterceptorName(String interceptorName) {
        this.interceptorName = interceptorName;
    }

    @Override
    public boolean canBeRemoved() {
        return true;
    }

    public String getExceptionTranslationName() {
        return exceptionTranslationName;
    }

    public void setExceptionTranslationName(String exceptionTranslationName) {
        this.exceptionTranslationName = exceptionTranslationName;
    }
}

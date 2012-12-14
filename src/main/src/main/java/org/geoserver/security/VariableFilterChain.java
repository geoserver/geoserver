/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import org.springframework.util.StringUtils;



/**
 * Filter chains of this type can be modified  
 * 
 * @author christian
 *
 */
public abstract class VariableFilterChain extends RequestFilterChain {

    String roleFilterName,interceptorName;
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    

    public VariableFilterChain(String... patterns) {
        super(patterns);     
        interceptorName=GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR;
    }

    
    public boolean isConstant() {
        return false;
    }
        
    public String getRoleFilterName() {
        return roleFilterName;
    }

    public void setRoleFilterName(String roleFilterName) {
        this.roleFilterName = roleFilterName;
    }

    
    /**
     * list the filter names which can be added to this chain
     * 
     * @param m
     * @return
     */
    public abstract SortedSet<String> listFilterCandidates(GeoServerSecurityManager m) throws IOException;
        

    @Override
    void createCompiledFilterList(List<String> list) {
        if (StringUtils.hasLength(getRoleFilterName()))
                list.add(1, getRoleFilterName());
        list.addAll(getFilterNames());
        list.add(GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
        list.add(interceptorName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VariableFilterChain == false)
            return false;
        
        VariableFilterChain other = (VariableFilterChain) obj;
        if (this.roleFilterName ==null && other.roleFilterName!=null)
            return false;
        if (this.roleFilterName !=null && this.roleFilterName.equals(other.roleFilterName)==false)
            return false;
        if (this.interceptorName ==null && other.interceptorName!=null)
            return false;
        if (this.interceptorName !=null && this.interceptorName.equals(other.interceptorName)==false)
            return false;                

        
        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = hash * ((roleFilterName == null) ? 1 : roleFilterName.hashCode());
        hash = hash * ((interceptorName == null) ? 1 : interceptorName.hashCode());
        return hash;
    }


    public String getInterceptorName() {
        return interceptorName;
    }


    public void setInterceptorName(String interceptorName) {
        this.interceptorName = interceptorName;
    }


}

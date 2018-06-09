/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.VariableFilterChain;

/**
 * Model for {@link VariableFilterChain}
 *
 * @author christian
 */
public class VariableFilterChainWrapper extends RequestFilterChainWrapper {

    private static final long serialVersionUID = 1L;

    public VariableFilterChainWrapper(VariableFilterChain chain) {
        super(chain);
    }

    public String getInterceptorName() {
        return getVariableFilterChain().getInterceptorName();
    }

    public void setInterceptorName(String interceptorName) {
        getVariableFilterChain().setInterceptorName(interceptorName);
    }

    public String getgetExceptionTranslationName() {
        return getVariableFilterChain().getExceptionTranslationName();
    }

    public void setExceptionTranslationName(String exceptionTranslationName) {
        getVariableFilterChain().setExceptionTranslationName(exceptionTranslationName);
    }

    public VariableFilterChain getVariableFilterChain() {
        return (VariableFilterChain) getChain();
    }
}

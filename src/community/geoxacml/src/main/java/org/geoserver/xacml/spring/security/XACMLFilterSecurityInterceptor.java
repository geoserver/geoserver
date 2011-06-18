/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.xacml.spring.security;

import org.springframework.security.intercept.ObjectDefinitionSource;
import org.springframework.security.intercept.web.FilterSecurityInterceptor;

/**
 * Url based authorization
 * 
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLFilterSecurityInterceptor extends FilterSecurityInterceptor {

    @Override
    public ObjectDefinitionSource obtainObjectDefinitionSource() {
        return XACMLFilterDefinitionSource.Singleton;
    }

}

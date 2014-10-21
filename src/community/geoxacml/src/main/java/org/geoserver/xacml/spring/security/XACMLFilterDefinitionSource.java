/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.spring.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.intercept.ObjectDefinitionSource;

/**
 * Spring Security ObjectDefinitonSource implementation for Services
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLFilterDefinitionSource implements ObjectDefinitionSource {

    private static final ConfigAttributeDefinition ConfigDef = new ConfigAttributeDefinition("xacml");

    public final static XACMLFilterDefinitionSource Singleton = new XACMLFilterDefinitionSource();

    public ConfigAttributeDefinition getAttributes(Object obj) throws IllegalArgumentException {
        return ConfigDef;
    }

    public Collection getConfigAttributeDefinitions() {
        return Collections.EMPTY_SET;
    }

    public boolean supports(Class aClass) {
        return true;
    }

}

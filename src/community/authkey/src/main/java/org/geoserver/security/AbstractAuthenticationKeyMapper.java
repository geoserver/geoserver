/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.util.StringUtils;

/**
 * Base class for {@link AuthenticationKeyMapper} implementations
 * 
 * 
 * @author christian
 *
 */
public abstract class AbstractAuthenticationKeyMapper implements AuthenticationKeyMapper {


    protected static Logger LOGGER = 
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security");

    
    private String beanName;    
    private String userGroupServiceName;
    private GeoServerSecurityManager securityManager;


    @Override
    public void setBeanName(String name) {
        beanName=name;
    }
    
    public String getBeanName() {
        return beanName;
    }

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }


    public GeoServerSecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    protected GeoServerUserGroupService getUserGroupService() throws IOException {
        GeoServerUserGroupService service= getSecurityManager().loadUserGroupService(getUserGroupServiceName());
        if (service==null) { 
            throw new IOException("Unkown user/group service: "+getUserGroupServiceName());
        }
        return service;
    }

    protected void checkProperties() throws IOException {
        if (StringUtils.hasLength(getUserGroupServiceName())==false) {
            throw new IOException ("User/Group Service Name is unset");            
        }
        if (getSecurityManager()==null) {
            throw new IOException ("Security manager is unset");            
        }

    }
    
    protected String createAuthKey() {
        return UUID.randomUUID().toString();
    }
}

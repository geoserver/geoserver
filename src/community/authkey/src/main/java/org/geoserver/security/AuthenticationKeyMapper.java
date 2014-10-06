/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;

import org.geoserver.security.impl.GeoServerUser;
import org.springframework.beans.factory.BeanNameAware;

/**
 * 
 * Maps a unique authentication key to a user name. Since user names are
 * unique within a {@link GeoServerUserGroupService} an individual mapper
 * is needed for each service offering this feature.
 * 
 * @author Andrea Aime - GeoSolution
 */
public interface AuthenticationKeyMapper extends BeanNameAware {

    /**
     * Maps the key provided in the request to the {@link GeoServerUser} object
     * of the corresponding user, or returns null
     * if no corresponding user is found
     * 
     * Returns <code>null</code> if the user is disabled
     * 
     * @param key
     * @return
     */
    GeoServerUser getUser(String key) throws IOException;
    
    /**
     * Assures that each user in the corresponding {@link GeoServerUserGroupService} has
     * an authentication key.
     * 
     * returns the number of added authentication keys
     * 
     * @throws IOException
     */
    int synchronize() throws IOException;
            
    /**
     * Returns <code>true</code> it the mapper can deal with read only u 
     * user/group services
     * 
     * @return 
     */
    boolean supportsReadOnlyUserGroupService();
    
    String getBeanName();
    
    void setUserGroupServiceName(String serviceName);
    String getUserGroupServiceName();
    
    public GeoServerSecurityManager getSecurityManager();
    public void setSecurityManager(GeoServerSecurityManager securityManager);
    

}

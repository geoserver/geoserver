/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.List;

import org.geoserver.catalog.Info;
import org.geoserver.wps.security.SecurityProcessFactory;
import org.opengis.feature.type.Name;

/**
 * Configuration for a specific process to configure enable/disable and roles informations (backed by a {@link SecurityProcessFactory})
 * 
 * @used {@link ProcessGroupInfo#getFilteredProcesses()}
 * 
 */
public interface ProcessInfo extends Info, Cloneable {

    Name getName();
    
    void setName(Name name);
    
    /*
     * Enables/disables the WPS
     */
    void setEnabled(Boolean enabled);
    
    boolean isEnabled();    

    /*
     * Return roles granted to works with this WPS
     */
    List<String> getRoles();   

}

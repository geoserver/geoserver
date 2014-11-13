/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.type.Name;

public class ProcessInfoImpl implements ProcessInfo {

    private static final long serialVersionUID = -8791361642137777632L;
    
    private Boolean enabled;
    private List<String> roles = new ArrayList<String>();
    private Name name;
    private String id;
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setName(Name name) {
       this.name = name;        
    }

    @Override
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;        
    }

}

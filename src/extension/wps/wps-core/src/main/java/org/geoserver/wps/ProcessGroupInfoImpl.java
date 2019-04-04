/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.geoserver.catalog.MetadataMap;
import org.geotools.process.ProcessFactory;

public class ProcessGroupInfoImpl implements ProcessGroupInfo {

    private static final long serialVersionUID = 4850653421657310854L;

    Class<? extends ProcessFactory> factoryClass;

    boolean enabled;

    List<String> roles = new ArrayList<>();

    List<ProcessInfo> filteredProcesses = new ArrayList<ProcessInfo>();

    MetadataMap metadata = new MetadataMap();

    @Override
    public String getId() {
        return "wpsProcessFactory-" + factoryClass.getName();
    }

    public Class<? extends ProcessFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends ProcessFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ProcessInfo> getFilteredProcesses() {
        return filteredProcesses;
    }

    public void setFilteredProcesses(List<ProcessInfo> filteredProcesses) {
        this.filteredProcesses = filteredProcesses;
    }

    @Override
    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadataMap) {
        this.metadata = metadataMap;
    }

    @Override
    public ProcessGroupInfo clone() {
        ProcessGroupInfoImpl clone = new ProcessGroupInfoImpl();
        clone.setEnabled(enabled);
        clone.setFactoryClass(factoryClass);
        clone.setRoles(roles);
        if (filteredProcesses != null) {
            clone.setFilteredProcesses(new ArrayList<ProcessInfo>(filteredProcesses));
        }
        if (metadata != null) {
            clone.metadata = new MetadataMap(new HashMap<String, Serializable>(metadata));
        }

        return clone;
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((factoryClass == null) ? 0 : factoryClass.hashCode());
        result = prime * result + ((filteredProcesses == null) ? 0 : filteredProcesses.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ProcessGroupInfoImpl other = (ProcessGroupInfoImpl) obj;
        if (enabled != other.enabled) return false;
        if (factoryClass == null) {
            if (other.factoryClass != null) return false;
        } else if (!factoryClass.equals(other.factoryClass)) return false;
        if (filteredProcesses == null) {
            if (other.filteredProcesses != null) return false;
        } else if (!filteredProcesses.equals(other.filteredProcesses)) return false;
        if (metadata == null) {
            if (other.metadata != null) return false;
        } else if (!metadata.equals(other.metadata)) return false;
        if (roles == null) {
            if (other.roles != null) return false;
        } else if (!roles.equals(other.roles)) return false;
        return true;
    }
}

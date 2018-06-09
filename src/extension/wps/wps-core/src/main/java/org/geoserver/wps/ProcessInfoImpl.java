/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wps.validator.WPSInputValidator;
import org.opengis.feature.type.Name;

public class ProcessInfoImpl implements ProcessInfo {

    private static final long serialVersionUID = -8791361642137777632L;

    private Boolean enabled;

    private List<String> roles = new ArrayList<String>();

    private Name name;

    private String id;

    private Multimap<String, WPSInputValidator> validators = ArrayListMultimap.create();

    MetadataMap metadata = new MetadataMap();

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

    public Multimap<String, WPSInputValidator> getValidators() {
        return validators;
    }

    public void setValidators(Multimap<String, WPSInputValidator> validators) {
        this.validators = validators;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadataMap) {
        this.metadata = metadataMap;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        result = prime * result + ((validators == null) ? 0 : validators.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ProcessInfoImpl other = (ProcessInfoImpl) obj;
        if (enabled == null) {
            if (other.enabled != null) return false;
        } else if (!enabled.equals(other.enabled)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (metadata == null) {
            if (other.metadata != null) return false;
        } else if (!metadata.equals(other.metadata)) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (roles == null) {
            if (other.roles != null) return false;
        } else if (!roles.equals(other.roles)) return false;
        if (validators == null) {
            if (other.validators != null) return false;
        } else if (!validators.equals(other.validators)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "ProcessInfoImpl [enabled="
                + enabled
                + ", roles="
                + roles
                + ", name="
                + name
                + ", id="
                + id
                + ", validators="
                + validators
                + ", metadata="
                + metadata
                + "]";
    }
}

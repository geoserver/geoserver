/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model.impl;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.geoserver.metadata.data.model.MetadataTemplate;

/** @author Timothy De Bock - timothy.debock.github@gmail.com */
@XStreamAlias("MetadataTemplate")
public class MetadataTemplateImpl implements Serializable, MetadataTemplate {

    private static final long serialVersionUID = -1907518678061997394L;

    private String id;

    private String name;

    private String description;

    private Map<String, Serializable> metadata;

    private Set<String> linkedLayers;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, Serializable> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    @Override
    public Set<String> getLinkedLayers() {
        if (linkedLayers == null) {
            linkedLayers = new HashSet<>();
        }
        return linkedLayers;
    }

    @Override
    public boolean equals(Object other) {
        if (getId() == null) {
            return this == other;
        } else {
            return other instanceof MetadataTemplate
                    && ((MetadataTemplate) other).getId().equals(getId());
        }
    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : id.hashCode();
    }

    @Override
    public MetadataTemplate clone() {
        MetadataTemplateImpl clone = new MetadataTemplateImpl();
        clone.setId(getId());
        clone.setName(getName());
        clone.setDescription(getDescription());
        clone.linkedLayers = new HashSet<>(getLinkedLayers());
        clone.metadata = new HashMap<>();
        for (Entry<String, Serializable> entry : getMetadata().entrySet()) {
            clone.metadata.put(entry.getKey(), ComplexMetadataMapImpl.dimCopy(entry.getValue()));
        }
        return clone;
    }
}

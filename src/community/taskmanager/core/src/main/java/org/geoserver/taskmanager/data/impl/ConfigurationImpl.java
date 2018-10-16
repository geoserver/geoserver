/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "removeStamp"})})
@FilterDefs({
    @FilterDef(name = "activeTaskFilter", defaultCondition = "removeStamp = 0"),
    @FilterDef(name = "activeBatchFilter", defaultCondition = "removeStamp = 0")
})
public class ConfigurationImpl extends BaseImpl implements Configuration {

    private static final long serialVersionUID = 7562166441281067057L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean template = false;

    @Column(nullable = false)
    private Boolean validated = false;

    @Column private String workspace;

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = AttributeImpl.class,
        mappedBy = "configuration",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("id")
    @MapKey(name = "name")
    private Map<String, Attribute> attributes = new LinkedHashMap<String, Attribute>();

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = TaskImpl.class,
        mappedBy = "configuration",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("id")
    @MapKey(name = "name")
    @Filter(name = "activeTaskFilter")
    private Map<String, Task> tasks = new LinkedHashMap<String, Task>();

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = BatchImpl.class,
        mappedBy = "configuration",
        cascade = CascadeType.ALL
    )
    @OrderBy("id")
    @MapKey(name = "name")
    @Filter(name = "activeBatchFilter")
    private Map<String, Batch> batches = new LinkedHashMap<String, Batch>();

    @Column(nullable = false)
    private Long removeStamp = 0L;

    @Column private String description;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean isTemplate() {
        return template;
    }

    @Override
    public void setTemplate(boolean template) {
        this.template = template;
    }

    @Override
    public String getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public Map<String, Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public Map<String, Task> getTasks() {
        return tasks;
    }

    @Override
    public Map<String, Batch> getBatches() {
        return batches;
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
    public void setRemoveStamp(long removeStamp) {
        this.removeStamp = removeStamp;
    }

    @Override
    public long getRemoveStamp() {
        return removeStamp;
    }

    @Override
    public boolean isValidated() {
        return validated;
    }

    @Override
    public void setValidated(boolean initMode) {
        this.validated = initMode;
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

@Entity
@Table(
    uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "configuration", "removeStamp"})}
)
@FilterDef(name = "activeTaskElementFilter", defaultCondition = "removeStamp = 0")
// TODO: need alias support for filters, for now need to filter this out manually
// @FilterDef(name="activeTaskElementFilter", defaultCondition="removeStamp = 0 and
// batch.removeStamp = 0")
public class TaskImpl extends BaseImpl implements Task {

    private static final long serialVersionUID = -4050889394621568829L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column private String name;

    @Column private String type;

    @ManyToOne
    @JoinColumn(name = "configuration")
    private ConfigurationImpl configuration;

    @OneToMany(
        fetch = FetchType.EAGER,
        targetEntity = ParameterImpl.class,
        mappedBy = "task",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @MapKey(name = "name")
    @OrderBy("id")
    private Map<String, Parameter> parameters = new LinkedHashMap<String, Parameter>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = BatchElementImpl.class, mappedBy = "task")
    @OrderBy("index")
    @Filter(name = "activeTaskElementFilter")
    private List<BatchElement> batchElements = new ArrayList<BatchElement>();

    @Column(nullable = false)
    private Long removeStamp = 0L;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    @Override
    public List<BatchElement> getBatchElements() {
        return batchElements;
    }

    public void setBatchElements(List<BatchElement> batchElements) {
        this.batchElements = batchElements;
    }

    @Override
    public ConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = (ConfigurationImpl) configuration;
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
    public void setRemoveStamp(long removeStamp) {
        this.removeStamp = removeStamp;
    }

    @Override
    public long getRemoveStamp() {
        return removeStamp;
    }
}

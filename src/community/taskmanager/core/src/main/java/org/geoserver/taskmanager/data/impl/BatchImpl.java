/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

/** @author Niels Charlier */
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "configuration", "removeStamp"}),
        @UniqueConstraint(columnNames = {"nameNoConfig", "removeStamp"})
    }
)
@FilterDef(name = "activeElementFilter", defaultCondition = "removeStamp = 0")
public class BatchImpl extends BaseImpl implements Batch {

    private static final long serialVersionUID = 3321130631692899821L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = BatchElementImpl.class,
        mappedBy = "batch",
        cascade = CascadeType.ALL
    )
    @OrderBy("index, id")
    @Filter(name = "activeElementFilter")
    private List<BatchElement> elements = new ArrayList<BatchElement>();

    @Column private String workspace;

    @Column(nullable = false)
    private String name;

    // stupid work-around
    // duplicate of name only set if configuration == null, just for unique constraint
    @Column private String nameNoConfig;

    @ManyToOne
    @JoinColumn(name = "configuration", nullable = true)
    private ConfigurationImpl configuration;

    @Column private String description;

    @Column(nullable = true)
    private String frequency;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Long removeStamp = 0L;

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = BatchRunImpl.class,
        mappedBy = "batch",
        cascade = CascadeType.ALL
    )
    @OrderBy("id")
    private List<BatchRun> batchRuns = new ArrayList<BatchRun>();

    @Transient private BatchRun latestBatchRun;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public List<BatchElement> getElements() {
        return elements;
    }

    @Override
    public String getFrequency() {
        return frequency;
    }

    @Override
    public void setFrequency(String frequency) {
        this.frequency = frequency;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        if (configuration == null) {
            this.nameNoConfig = name;
        }
    }

    @Override
    public ConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = (ConfigurationImpl) configuration;
        if (configuration == null) {
            nameNoConfig = name;
        } else {
            nameNoConfig = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
    public List<BatchRun> getBatchRuns() {
        return batchRuns;
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
    public BatchRun getLatestBatchRun() {
        return latestBatchRun;
    }

    public void setLatestBatchRun(BatchRun latestBatchRun) {
        this.latestBatchRun = latestBatchRun;
    }
}

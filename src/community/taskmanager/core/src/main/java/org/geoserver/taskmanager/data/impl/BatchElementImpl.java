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
import javax.persistence.UniqueConstraint;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Task;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"task", "batch"})})
public class BatchElementImpl extends BaseImpl implements BatchElement {

    private static final long serialVersionUID = 7690398584400545752L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch")
    private BatchImpl batch;

    @ManyToOne
    @JoinColumn(name = "task")
    private TaskImpl task;

    @Column private Integer index;

    @OneToMany(
        fetch = FetchType.LAZY,
        targetEntity = RunImpl.class,
        mappedBy = "batchElement",
        cascade = CascadeType.ALL
    )
    @OrderBy("start")
    private List<Run> runs = new ArrayList<Run>();

    @Column(nullable = false)
    private Long removeStamp = 0L;

    @Override
    public BatchImpl getBatch() {
        return batch;
    }

    @Override
    public void setBatch(Batch batch) {
        this.batch = (BatchImpl) batch;
    }

    @Override
    public TaskImpl getTask() {
        return task;
    }

    @Override
    public void setTask(Task task) {
        this.task = (TaskImpl) task;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public List<Run> getRuns() {
        return runs;
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

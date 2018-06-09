/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import java.util.ArrayList;
import java.util.Date;
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
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

@Entity
@Table
public class BatchRunImpl extends BaseImpl implements BatchRun {

    private static final long serialVersionUID = 2468505054020768482L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch")
    private BatchImpl batch;

    @OneToMany(
        fetch = FetchType.EAGER,
        targetEntity = RunImpl.class,
        mappedBy = "batchRun",
        cascade = CascadeType.ALL
    )
    @OrderBy("start")
    @Fetch(FetchMode.SUBSELECT)
    private List<Run> runs = new ArrayList<Run>();

    @Column(nullable = false)
    private Boolean interruptMe = false;

    @Column
    @Index(name = "schedulerReferenceIndex")
    private String schedulerReference;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public BatchImpl getBatch() {
        return batch;
    }

    @Override
    public void setBatch(Batch batch) {
        this.batch = (BatchImpl) batch;
    }

    @Override
    public List<Run> getRuns() {
        return runs;
    }

    @Override
    public Date getStart() {
        return BatchRun.super.getStart();
    }

    @Override
    public Date getEnd() {
        return BatchRun.super.getEnd();
    }

    @Override
    public Run.Status getStatus() {
        return BatchRun.super.getStatus();
    }

    @Override
    public String getMessage() {
        return BatchRun.super.getMessage();
    }

    @Override
    public boolean isInterruptMe() {
        return interruptMe;
    }

    @Override
    public void setInterruptMe(boolean interruptMe) {
        this.interruptMe = interruptMe;
    }

    @Override
    public String getSchedulerReference() {
        return schedulerReference;
    }

    @Override
    public void setSchedulerReference(String qReference) {
        this.schedulerReference = qReference;
    }
}

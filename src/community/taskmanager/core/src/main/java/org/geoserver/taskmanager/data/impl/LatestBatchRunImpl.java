package org.geoserver.taskmanager.data.impl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.geoserver.taskmanager.data.LatestBatchRun;
import org.hibernate.annotations.Subselect;

@Entity
@Subselect("select max(lbr_batchrun.id) as batchRun, lbr_batchrun.batch as batch"
        + " from RunImpl lbr_run"
        + " inner join BatchRunImpl lbr_batchrun on(lbr_run.batchRun = lbr_batchrun.id)"
        + " group by lbr_batchrun.batch")
public class LatestBatchRunImpl implements LatestBatchRun {

    @Id
    @Column(name = "batchRun")
    private Long id;

    @OneToOne
    @JoinColumn(name = "batchRun", nullable = true)
    private BatchRunImpl batchRun;

    @OneToOne
    @JoinColumn(name = "batch", nullable = true)
    private BatchImpl batch;

    @Override
    public BatchRunImpl getBatchRun() {
        return batchRun;
    }

    public void setBatchRun(BatchRunImpl batchRun) {
        this.batchRun = batchRun;
    }

    @Override
    public BatchImpl getBatch() {
        return batch;
    }

    public void setBatch(BatchImpl batch) {
        this.batch = batch;
    }
}

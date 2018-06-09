/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;

@Entity
@Table
public class RunImpl extends BaseImpl implements Run {

    private static final long serialVersionUID = -4539522553695926319L;

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batchElement")
    private BatchElementImpl batchElement;

    @Column(nullable = false)
    private Date start;

    @Column(name = "runEnd")
    private Date end;

    @Column(nullable = false)
    @Enumerated
    private Status status = Status.RUNNING;

    @Column(length = 8192)
    private byte[] message;

    @ManyToOne
    @JoinColumn(name = "batchRun")
    private BatchRunImpl batchRun;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Date getStart() {
        return start;
    }

    @Override
    public void setStart(Date start) {
        this.start = start;
    }

    @Override
    public Date getEnd() {
        return end;
    }

    @Override
    public void setEnd(Date end) {
        this.end = end;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public BatchElementImpl getBatchElement() {
        return batchElement;
    }

    @Override
    public void setBatchElement(BatchElement batchElement) {
        this.batchElement = (BatchElementImpl) batchElement;
    }

    @Override
    public String getMessage() {
        return message == null ? null : new String(message);
    }

    @Override
    public void setMessage(String message) {
        this.message = message == null ? null : message.getBytes();
    }

    @Override
    public BatchRun getBatchRun() {
        return batchRun;
    }

    @Override
    public void setBatchRun(BatchRun br) {
        this.batchRun = (BatchRunImpl) br;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.job;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Task<V> extends FutureTask<V> {

    Long id;
    ProgressMonitor monitor;
    Throwable error;
    AtomicBoolean recieved = new AtomicBoolean(false);
    AtomicBoolean started = new AtomicBoolean(false);
    String toString;

    public Task(Job<V> job) {
        super(job);
        toString = job.toString();
        monitor = new ProgressMonitor();
        job.setMonitor(monitor);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public ProgressMonitor getMonitor() {
        return monitor;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void recieve() {
        recieved.set(true);
    }

    public boolean isRecieved() {
        return recieved.get();
    }

    public void started() {
        started.set(true);
    }

    public boolean isStarted() {
        return started.get();
    }

    @Override
    public String toString() {
        return toString;
    }
}

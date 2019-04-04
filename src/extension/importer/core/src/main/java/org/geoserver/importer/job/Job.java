/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.job;

import java.util.concurrent.Callable;

public abstract class Job<V> implements Callable<V> {

    ProgressMonitor monitor;

    @Override
    public V call() throws Exception {
        return call(monitor);
    }

    public void setMonitor(ProgressMonitor monitor) {
        this.monitor = monitor;
    }

    protected abstract V call(ProgressMonitor monitor) throws Exception;
}

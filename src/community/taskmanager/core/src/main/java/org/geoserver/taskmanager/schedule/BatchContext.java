/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import org.geoserver.taskmanager.data.BatchRun;

/**
 * During run, tasks create temporary objects that are committed to real objects during the commit
 * phase (such as a table name) This maps real objects to temporary objects during a single batch
 * run. Tasks should save and look up temporary objects so that tasks within a batch can work
 * together.
 *
 * @author Niels Charlier
 */
public interface BatchContext {

    public static interface Dependency {
        public void revert() throws TaskException;
    }

    Object get(Object original);

    Object get(Object original, Dependency dependency);

    /** Whatever is put here in the task, must be removed in the commit! */
    void put(Object original, Object temp);

    void delete(Object original) throws TaskException;

    BatchRun getBatchRun();
}

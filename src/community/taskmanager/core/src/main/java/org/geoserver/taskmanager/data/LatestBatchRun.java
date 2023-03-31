package org.geoserver.taskmanager.data;

import org.geoserver.taskmanager.data.impl.BatchImpl;
import org.geoserver.taskmanager.data.impl.BatchRunImpl;

public interface LatestBatchRun {

    BatchRunImpl getBatchRun();

    BatchImpl getBatch();
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.report;

import org.geoserver.taskmanager.data.BatchRun;

/**
 * A report builder generates a report from a batch. One could write a custom one.
 *
 * @author Niels Charlier
 */
public interface ReportBuilder {

    Report buildBatchRunReport(BatchRun batchRun);
}

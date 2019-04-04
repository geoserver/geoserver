/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.report.impl;

import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.report.Report;
import org.geoserver.taskmanager.report.Report.Type;
import org.geoserver.taskmanager.report.ReportBuilder;
import org.springframework.stereotype.Service;

/**
 * A very simple report builder. Contains all information that matters in simple text format.
 *
 * @author Niels Charlier
 */
@Service
public class SimpleReportBuilderImpl implements ReportBuilder {

    @Override
    public Report buildBatchRunReport(BatchRun batchRun) {
        StringBuilder reportContent = new StringBuilder();

        for (Run run : batchRun.getRuns()) {
            BatchElement element = run.getBatchElement();
            Task task = element.getTask();
            reportContent.append(
                    task.getFullName()
                            + ", started "
                            + run.getStart()
                            + ", ended "
                            + run.getEnd()
                            + ", status is "
                            + run.getStatus()
                            + "\n");
            if (run.getMessage() != null) {
                reportContent.append(
                        "\tmessage: " + run.getMessage() + " (check logs for more details) \n");
            }
        }

        StringBuilder reportTitle =
                new StringBuilder("Report: Batch " + batchRun.getBatch().getFullName() + " ");
        Type type;

        switch (batchRun.getStatus()) {
            case FAILED:
            case NOT_COMMITTED:
            case /* shouldn't happen */ READY_TO_COMMIT:
            case /* shouldn't happen */ RUNNING:
            case /* shouldn't happen */ COMMITTING:
            case /* shouldn't happen */ ROLLING_BACK:
                reportTitle.append("has failed");
                type = Type.FAILED;
                break;
            case ROLLED_BACK:
            case NOT_ROLLED_BACK:
                reportTitle.append("was cancelled");
                type = Type.CANCELLED;
                break;
            default:
                reportTitle.append("was successful");
                type = Type.SUCCESS;
        }

        return new Report(reportTitle.toString(), reportContent.toString(), type);
    }
}

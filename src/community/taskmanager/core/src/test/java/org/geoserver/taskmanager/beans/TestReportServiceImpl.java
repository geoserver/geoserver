/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.beans;

import java.util.logging.Logger;
import org.geoserver.taskmanager.report.Report;
import org.geoserver.taskmanager.report.ReportService;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Service;

/**
 * A report service for testing.
 *
 * @author Niels Charlier
 */
@Service
public class TestReportServiceImpl implements ReportService {

    private static final Logger LOGGER = Logging.getLogger(TestReportServiceImpl.class);

    private Filter filter = Filter.ALL;

    private Report lastReport;

    @Override
    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Report getLastReport() {
        return lastReport;
    }

    public void clear() {
        lastReport = null;
    }

    @Override
    public void sendReport(Report report) {
        LOGGER.info("Subject: " + report.getTitle());
        LOGGER.info(report.getContent());
        lastReport = report;
    }
}

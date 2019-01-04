/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.report;

import org.apache.commons.lang3.ArrayUtils;

/**
 * A report service sends a report to a particular destination. One can add an unlimited amount of
 * report services which will all be used.
 *
 * @author Niels Charlier
 */
public interface ReportService {

    /** Enumeration for filter. */
    public enum Filter {
        /** All batch runs are reported * */
        ALL(Report.Type.FAILED, Report.Type.CANCELLED, Report.Type.SUCCESS),
        /** Only failed and cancelled batch runs are reported * */
        FAILED_AND_CANCELLED(Report.Type.FAILED, Report.Type.CANCELLED),
        /** Only failed batch runs are reported * */
        FAILED_ONLY(Report.Type.FAILED);

        Report.Type[] types;

        private Filter(Report.Type... types) {
            this.types = types;
        }

        public boolean matches(Report.Type type) {
            return ArrayUtils.contains(types, type);
        }
    }

    /**
     * Return the filter of the report.
     *
     * @return the filter of the report.
     */
    public Filter getFilter();

    /**
     * Send a report.
     *
     * @param report the report.
     */
    public void sendReport(Report report);
}

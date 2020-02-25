/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.report;

/**
 * A report is the result of a certain batch run, that can be used to email or notify otherwise.
 *
 * @author Niels Charlier
 */
public class Report {

    /** Enumeration for over-all result of the report. */
    public enum Type {
        FAILED,
        CANCELLED,
        SUCCESS
    };

    /** Title of report. */
    private String title;

    /** Content of report. */
    private String content;

    /** Over-all result of the report. */
    private Type type;

    public Report(String title, String content, Type type) {
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Type getType() {
        return type;
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import com.google.common.base.MoreObjects;
import java.io.Serializable;

public class LogEvent implements Serializable {

    private static final long serialVersionUID = -7446826318796072678L;

    public enum Severity {
        DEBUG,
        INFO,
        ERROR
    }

    private long timestamp;

    private Severity severity;

    private long eventId;

    private String repoUrl;

    private String user;

    private String message;

    LogEvent(
            long eventId,
            long timestamp,
            Severity severity,
            String repoUrl,
            String user,
            String message) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.severity = severity;
        this.repoUrl = repoUrl;
        this.user = user;
        this.message = message;
    }

    public long getEventId() {
        return eventId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getRepositoryURL() {
        return repoUrl;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(eventId)
                .addValue(severity)
                .add("time", timestamp)
                .add("user", user)
                .add("repo", repoUrl)
                .add("message", message)
                .toString();
    }
}

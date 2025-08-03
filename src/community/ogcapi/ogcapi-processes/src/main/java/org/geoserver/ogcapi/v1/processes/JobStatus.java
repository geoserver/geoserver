/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;

/** Maps a JSON job status from the OGC API Processes specification to a Java object. */
@JsonPropertyOrder({
    "processID",
    "type",
    "jobID",
    "status",
    "message",
    "created",
    "started",
    "finished",
    "updated",
    "progress",
    "links"
})
public class JobStatus extends AbstractDocument {

    public static final String RESULTS_REL = "http://www.opengis.net/def/rel/ogc/1.0/results";
    public static final String STATUS_REL = "status";

    private String processID;
    private final String type = "process";
    private String jobID;
    private StatusCode status;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date created;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date started;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date finished;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date updated;

    private Integer progress;

    public enum StatusCode {
        @JsonProperty("accepted")
        ACCEPTED,

        @JsonProperty("running")
        RUNNING,

        @JsonProperty("successful")
        SUCCESSFUL,

        @JsonProperty("failed")
        FAILED,

        @JsonProperty("dismissed")
        DISMISSED
    }

    public JobStatus(String processID, String jobID, StatusCode status) {
        this.processID = processID;
        this.jobID = jobID;
        this.status = status;

        Link statusLink = new Link();
        statusLink.setRel(STATUS_REL);
        statusLink.setType(APPLICATION_JSON_VALUE);
        String href = ResponseUtils.buildURL(
                APIRequestInfo.get().getBaseURL(), "ogc/processes/v1/jobs/" + jobID, null, URLMangler.URLType.SERVICE);
        statusLink.setHref(href);
        addLink(statusLink);
    }

    // Getters and setters

    public String getProcessID() {
        return processID;
    }

    public void setProcessID(String processID) {
        this.processID = processID;
    }

    public String getType() {
        return type;
    }

    public String getJobID() {
        return jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public StatusCode getStatus() {
        return status;
    }

    public void setStatus(StatusCode status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getFinished() {
        return finished;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    @Override
    public String toString() {
        return "JobStatus{" + "processID='"
                + processID + '\'' + ", type='"
                + type + '\'' + ", jobID='"
                + jobID + '\'' + ", status="
                + status + ", message='"
                + message + '\'' + ", created="
                + created + ", started="
                + started + ", finished="
                + finished + ", updated="
                + updated + ", progress="
                + progress + '}';
    }
}

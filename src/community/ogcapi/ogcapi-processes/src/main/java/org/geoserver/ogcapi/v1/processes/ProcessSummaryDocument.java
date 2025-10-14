/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi.v1.processes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Objects;
import org.geoserver.ogcapi.AbstractDocument;

@JsonInclude(Include.NON_NULL)
public class ProcessSummaryDocument extends AbstractDocument {

    String version;
    String title;
    String description;
    List<JobControl> jobControlOptions = List.of(JobControl.SYNC, JobControl.ASYNC);
    List<String> keywords;

    public ProcessSummaryDocument(Process process) {
        this.id = process.getName().getURI();
        this.version = process.getVersion();
        this.title = process.getTitle();
        this.description = process.getDescription();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<JobControl> getJobControlOptions() {
        return jobControlOptions;
    }

    public void setJobControlOptions(List<JobControl> jobControlOptions) {
        this.jobControlOptions = jobControlOptions;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ProcessSummaryDocument that = (ProcessSummaryDocument) o;
        return Objects.equals(version, that.version)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(jobControlOptions, that.jobControlOptions)
                && Objects.equals(keywords, that.keywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), version, title, description, jobControlOptions, keywords);
    }
}

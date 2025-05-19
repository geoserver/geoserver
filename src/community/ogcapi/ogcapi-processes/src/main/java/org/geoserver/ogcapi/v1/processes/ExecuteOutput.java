/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The output of the process, specified as a format and a transmission mode */
public class ExecuteOutput {
    /** The way an output is returned to the caller */
    enum TransmissionMode {
        @JsonProperty("value")
        VALUE,
        @JsonProperty("reference")
        REFERENCE,
    }

    /** The format of the output, specified as a media type */
    public static class ExecuteOutputFormat {
        String mediaType;

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }
    }

    ExecuteOutputFormat format;
    TransmissionMode transmissionMode;

    public ExecuteOutputFormat getFormat() {
        return format;
    }

    public TransmissionMode getTransmissionMode() {
        return transmissionMode;
    }

    public void setFormat(ExecuteOutputFormat format) {
        this.format = format;
    }

    public void setTransmissionMode(TransmissionMode transmissionMode) {
        this.transmissionMode = transmissionMode;
    }
}

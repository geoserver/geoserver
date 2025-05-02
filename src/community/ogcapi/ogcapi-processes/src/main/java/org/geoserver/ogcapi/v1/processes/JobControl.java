/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum JobControl {
    @JsonProperty("async-execute")
    ASYNC,
    @JsonProperty("sync-execute")
    SYNC;
}

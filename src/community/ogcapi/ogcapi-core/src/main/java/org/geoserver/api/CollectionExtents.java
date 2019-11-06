/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DateRange;

@JsonPropertyOrder({"spatial", "temporal"})
public class CollectionExtents {

    List<ReferencedEnvelope> spatial;
    DateRange temporal;

    public CollectionExtents(List<ReferencedEnvelope> spatial, DateRange temporal) {
        this.spatial = spatial;
        this.temporal = temporal;
    }

    public CollectionExtents(ReferencedEnvelope spatial, DateRange temporal) {
        this.spatial = Arrays.asList(spatial);
        this.temporal = temporal;
    }

    public CollectionExtents(DateRange temporal) {
        this.temporal = temporal;
    }

    public CollectionExtents(ReferencedEnvelope spatial) {
        this.spatial = Arrays.asList(spatial);
    }

    @JsonIgnore
    public List<ReferencedEnvelope> getSpatial() {
        return spatial;
    }

    public void setSpatial(List<ReferencedEnvelope> spatial) {
        this.spatial = spatial;
    }

    @JsonIgnore
    public DateRange getTemporal() {
        return temporal;
    }

    public void setTemporal(DateRange temporal) {
        this.temporal = temporal;
    }

    @JsonProperty("spatial")
    public List<double[]> getSpatialArray() {
        if (spatial != null) {
            return spatial.stream()
                    .map(
                            re ->
                                    new double[] {
                                        re.getMinX(), re.getMinY(), re.getMaxX(), re.getMaxY()
                                    })
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    @JsonProperty("temporal")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String[] getTemporalArray() {
        if (temporal != null) {
            return new String[] {
                ISO_INSTANT.format(temporal.getMinValue().toInstant()),
                ISO_INSTANT.format(temporal.getMaxValue().toInstant())
            };
        } else {
            return null;
        }
    }
}

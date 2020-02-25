package org.geoserver.wfs3;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DateRange;

@JsonPropertyOrder({"spatial", "temporal"})
public class WFSExtents {

    ReferencedEnvelope spatial;
    DateRange temporal;

    public WFSExtents(ReferencedEnvelope spatial, DateRange temporal) {
        this.spatial = spatial;
        this.temporal = temporal;
    }

    public WFSExtents(DateRange temporal) {
        this.temporal = temporal;
    }

    public WFSExtents(ReferencedEnvelope spatial) {
        this.spatial = spatial;
    }

    @JsonIgnore
    public ReferencedEnvelope getSpatial() {
        return spatial;
    }

    public void setSpatial(ReferencedEnvelope spatial) {
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
    public double[] getSpatialArray() {
        if (spatial != null) {
            return new double[] {
                spatial.getMinX(), spatial.getMinY(), spatial.getMaxX(), spatial.getMaxY()
            };
        } else {
            return null;
        }
    }

    @JsonProperty("temporal")
    @JacksonXmlProperty()
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

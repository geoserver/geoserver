/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;

/** Extent details (including spatial and temporal). */
@JsonPropertyOrder({"spatial", "temporal"})
public class CollectionExtents {

    public static final String WGS84H = "http://www.opengis.net/def/crs/OGC/0/CRS84h";
    public static final String WGS84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    List<ReferencedEnvelope> spatial;
    DateRange temporal;

    /** Spatial extent bboxs, each bbox xmin,ymin,xmax,ymax crs CRS84. */
    public class SpatialExtents {

        public List<double[]> getBbox() {
            if (spatial != null) {
                return spatial.stream().map(re -> getDoubles(re)).collect(Collectors.toList());
            } else {
                return null;
            }
        }

        private double[] getDoubles(ReferencedEnvelope re) {
            if (re instanceof ReferencedEnvelope3D) {
                ReferencedEnvelope3D re3d = (ReferencedEnvelope3D) re;
                return new double[] {
                    re.getMinX(),
                    re.getMinY(),
                    re3d.getMinZ(),
                    re.getMaxX(),
                    re.getMaxY(),
                    re3d.getMaxZ()
                };
            }
            return new double[] {re.getMinX(), re.getMinY(), re.getMaxX(), re.getMaxY()};
        }

        public void setBbox(List<double[]> bbox) {
            if (bbox != null) {
                spatial =
                        bbox.stream()
                                .map(b -> toReferencedEnvelope(b))
                                .collect(Collectors.toList());
            } else {
                spatial = null;
            }
        }

        private ReferencedEnvelope toReferencedEnvelope(double[] b) {
            // handle the 3D case first
            if (b.length == 6)
                return new ReferencedEnvelope3D(
                        b[0], b[3], b[1], b[4], b[2], b[5], DefaultGeographicCRS.WGS84_3D);
            if (b.length != 4)
                throw new IllegalArgumentException("bbox array can contain either 4 or 6 values");
            return new ReferencedEnvelope(b[0], b[2], b[1], b[3], DefaultGeographicCRS.WGS84);
        }

        public String getCrs() {
            if (spatial != null
                    && spatial.stream().anyMatch(re -> re instanceof ReferencedEnvelope3D))
                return WGS84H;
            return WGS84;
        }

        public void setCrs(String crs) {
            // ignore for the moment, no other type of bbox is allowed for collection extents
        }
    }

    /** Temporal extent intervals, each interval between two UTC times. */
    public class TemporalExtents {

        public List<String[]> getInterval() {
            if (temporal != null) {
                java.util.Date minValue = temporal.getMinValue();
                java.util.Date maxValue = temporal.getMaxValue();
                if (minValue instanceof java.sql.Date) {
                    return Collections.singletonList(
                            new String[] {
                                ISO_INSTANT.format(sqlDateToInstant((java.sql.Date) minValue)),
                                ISO_INSTANT.format(sqlDateToInstant((java.sql.Date) maxValue))
                            });
                }
                return Collections.singletonList(
                        new String[] {
                            ISO_INSTANT.format(minValue.toInstant()),
                            ISO_INSTANT.format(maxValue.toInstant())
                        });
            } else {
                return null;
            }
        }

        private Instant sqlDateToInstant(java.sql.Date minValue) {
            return minValue.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        public String getTrs() {
            return "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
        }
    }

    /** For Jackson parsing */
    protected CollectionExtents() {}

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
    public SpatialExtents getSpatialExtents() {
        if (spatial != null) {
            return new SpatialExtents();
        } else {
            return null;
        }
    }

    // Made available for Jackson to use for parsing
    protected void setSpatialExtents(SpatialExtents spatialExtents) {
        // The creation of the object has the side effect of updating CollectionExtents.spatial
    }

    @JsonProperty("temporal")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public TemporalExtents getTemporalExtents() {
        if (temporal != null) {
            return new TemporalExtents();
        } else {
            return null;
        }
    }
}

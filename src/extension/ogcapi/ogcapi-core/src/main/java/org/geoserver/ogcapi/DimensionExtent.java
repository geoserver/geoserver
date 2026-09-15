/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Extent of a dimension beyond space and time, serialized as a sibling of {@code spatial} and {@code temporal} in
 * {@link CollectionExtents} (OGC API - Common, additional dimensions). A dimension carries exactly one of
 * {@link #getTrs()}, {@link #getVrs()} and {@link #getDefinition()}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"interval", "trs", "vrs", "definition", "unit", "grid"})
public class DimensionExtent {

    /** The coordinates of a dimension, either enumerated or described by the step of a regular interval. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"cellsCount", "coordinates", "resolution"})
    public record Grid(Integer cellsCount, List<Object> coordinates, Object resolution) {

        /** A grid listing every coordinate, for a domain known value by value. */
        public static Grid enumerated(List<Object> coordinates) {
            return new Grid(coordinates.size(), coordinates, null);
        }

        /** A grid described by its step, for a domain that is a regular interval. */
        public static Grid regular(Object resolution) {
            return new Grid(null, null, resolution);
        }
    }

    List<List<Object>> interval;
    String trs;
    String vrs;
    String definition;
    String unit;
    Grid grid;

    /** The overall range of the dimension, as a single closed interval. */
    public List<List<Object>> getInterval() {
        return interval;
    }

    public void setInterval(List<List<Object>> interval) {
        this.interval = interval;
    }

    /** Set only for a dimension whose values are dates. */
    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }

    /** Set only for an elevation dimension measured in a CRS. */
    public String getVrs() {
        return vrs;
    }

    public void setVrs(String vrs) {
        this.vrs = vrs;
    }

    /** Set for a dimension that is neither temporal nor vertical. */
    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    /** Unit of measure, as configured, when it is not itself a URI. */
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /** Absent for a continuous domain, whose values are not restricted to a grid. */
    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }
}

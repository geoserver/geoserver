/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The Referencing part of a CoverageJson */
public class Referencing {

    @JsonProperty List<String> coordinates;

    @JsonProperty(required = true)
    System system;

    public Referencing(System system, List<String> coordinates) {
        this.coordinates = coordinates;
        this.system = system;
    }

    static class System extends CoverageJson {

        protected System(String type) {
            super(type);
        }
    }

    private static final String EPSG_HEADER = "http://www.opengis.net/def/crs/EPSG/0/";

    static class TemporalRS extends System {

        @JsonProperty String calendar;

        protected TemporalRS() {
            super("TemporalRS");
            this.calendar = "Gregorian";
        }
    }

    static class VerticalCRS extends System {

        protected VerticalCRS() {
            super("VerticalCRS");
        }
    }

    static class HorizontalRS extends System {

        @JsonProperty String id;

        protected HorizontalRS(String type, String code) {
            super(type);
            id = EPSG_HEADER + code;
        }
    }

    static class GeographicCRS extends HorizontalRS {

        protected GeographicCRS(String code) {
            super("GeographicCRS", code);
        }
    }

    static class ProjectedCRS extends HorizontalRS {

        protected ProjectedCRS(String code) {
            super("ProjectedCRS", code);
        }
    }
}

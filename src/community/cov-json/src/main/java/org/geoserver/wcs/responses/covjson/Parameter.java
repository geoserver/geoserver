/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import org.geotools.coverage.GridSampleDimension;

@JsonPropertyOrder({"type", "description", "unit"})
public class Parameter extends CoverageJson {

    class ObservedProperty {

        @JsonProperty(required = true)
        private Map<String, String> label;

        public ObservedProperty(Map<String, String> i18field) {
            this.label = i18field;
        }
    }

    class Unit {

        @JsonProperty private Map<String, String> label;

        @JsonProperty private String symbol;

        public Unit(javax.measure.Unit<?> dim) {
            symbol = dim.getSymbol();
            label = CoverageJson.asI18nMap(dim.getName());
        }
    }

    static final String TYPE = "Parameter";

    @JsonProperty private Map<String, String> description;

    private Unit unit;

    @JsonProperty(required = true)
    private ObservedProperty observedProperty;

    public Parameter(GridSampleDimension dim) {
        super(TYPE);

        Map<String, String> i18field = CoverageJson.asI18nMap(dim.getDescription().toString());
        description = i18field;
        unit = buildUnit(dim.getUnits());
        observedProperty = new ObservedProperty(i18field);
    }

    private Unit buildUnit(javax.measure.Unit<?> unit) {
        return unit != null ? new Unit(unit) : null;
    }
}

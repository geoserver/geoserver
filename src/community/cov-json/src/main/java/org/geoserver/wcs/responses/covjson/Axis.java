/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonPropertyOrder({"start", "stop", "num", "values"})
public class Axis {

    List<String> coordinates;

    @JsonProperty List<?> values;

    @JsonIgnore String key;

    @JsonProperty Integer num;

    @JsonProperty Double start;

    public Integer getNum() {
        return num;
    }

    @JsonIgnore
    public int getSize() {
        return num != null ? num : values != null ? values.size() : 0;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Double getStart() {
        return start;
    }

    public void setStart(Double start) {
        this.start = start;
    }

    public Double getStop() {
        return stop;
    }

    public void setStop(Double stop) {
        this.stop = stop;
    }

    @JsonProperty Double stop;

    public String getKey() {
        return key;
    }

    public Axis(String key) {
        this.key = key;
    }

    public void setValues(List<Object> listValues) {
        values = listValues;
    }
}

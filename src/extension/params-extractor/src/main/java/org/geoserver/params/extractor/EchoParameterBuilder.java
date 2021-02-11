/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

public class EchoParameterBuilder {

    private String id;
    private String parameter;
    private Boolean activated;

    public EchoParameterBuilder copy(EchoParameter other) {
        this.id = other.getId();
        this.parameter = other.getParameter();
        this.activated = other.getActivated();
        return this;
    }

    public EchoParameterBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public EchoParameterBuilder withParameter(String parameter) {
        this.parameter = parameter;
        return this;
    }

    public EchoParameterBuilder withActivated(Boolean activated) {
        this.activated = activated;
        return this;
    }

    public EchoParameter build() {
        Utils.checkCondition(id != null, "Id cannot be NULL.");
        Utils.checkCondition(parameter != null, "Parameter cannot be NULL.");
        return new EchoParameter(id, parameter, Utils.withDefault(activated, true));
    }
}

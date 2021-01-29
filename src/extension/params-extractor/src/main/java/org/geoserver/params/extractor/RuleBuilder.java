/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.util.regex.Pattern;

public final class RuleBuilder {

    private String id;
    private Boolean activated;
    private Integer position;
    private String match;
    private String parameter;
    private String activation;
    private String transform;
    private Integer remove;
    private String combine;
    private Boolean repeat;

    private Pattern matchPattern;
    private Pattern activationPattern;

    public RuleBuilder copy(Rule other) {
        this.id = other.getId();
        this.activated = other.getActivated();
        this.position = other.getPosition();
        if (position != null) {
            this.matchPattern =
                    Pattern.compile(String.format("^(?:/[^/]*){%d}(/([^/]+)).*$", position));
        } else {
            this.matchPattern = null;
        }
        this.match = other.getMatch();
        this.parameter = other.getParameter();
        this.activation = other.getActivation();
        if (activation != null) {
            this.activationPattern = Pattern.compile(activation);
        }
        this.transform = other.getTransform();
        this.remove = other.getRemove();
        this.combine = other.getCombine();

        return this;
    }

    public RuleBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RuleBuilder withActivated(Boolean activated) {
        this.activated = activated;
        return this;
    }

    public RuleBuilder withPosition(Integer position) {
        if (position != null) {
            this.position = position;
            matchPattern = Pattern.compile(String.format("^(?:/[^/]*){%d}(/([^/]+)).*$", position));
        }
        return this;
    }

    public RuleBuilder withMatch(String match) {
        if (match != null) {
            this.match = match;
            matchPattern = Pattern.compile(match);
        }
        return this;
    }

    public RuleBuilder withParameter(String parameter) {
        this.parameter = parameter;
        return this;
    }

    public RuleBuilder withActivation(String activation) {
        if (activation != null) {
            activationPattern = Pattern.compile(activation);
            this.activation = activation;
        }
        return this;
    }

    public RuleBuilder withRemove(Integer remove) {
        this.remove = remove;
        return this;
    }

    public RuleBuilder withTransform(String transform) {
        this.transform = transform;
        return this;
    }

    public RuleBuilder withCombine(String combine) {
        this.combine = combine;
        return this;
    }

    public RuleBuilder withRepeat(Boolean repeat) {
        if (repeat != null) {
            this.repeat = repeat;
        }
        return this;
    }

    public Rule build() {
        Utils.checkCondition(
                position == null || match == null,
                "Only one of the attributes position and match can be selected.");
        Utils.checkCondition(id != null && !id.isEmpty(), "Rule id cannot be NULL or EMPTY.");
        Utils.checkCondition(
                matchPattern != null, "Both attributes position or match cannot be NULL.");
        Utils.checkCondition(
                parameter != null && !parameter.isEmpty(),
                "Parameter attribute is mandatory it cannot be NULL or EMPTY.");
        Utils.checkCondition(
                transform != null && !transform.isEmpty(),
                "Transform attribute is mandatory it cannot be NULL or EMPTY.");
        return new Rule(
                id,
                Utils.withDefault(activated, true),
                position,
                match,
                activation,
                parameter,
                transform,
                remove,
                combine,
                Utils.withDefault(repeat, false),
                matchPattern,
                activationPattern);
    }
}

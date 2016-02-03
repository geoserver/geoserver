/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import org.geoserver.params.extractor.Rule;
import org.geoserver.params.extractor.RuleBuilder;

import java.io.Serializable;
import java.util.UUID;

public class RuleModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private Integer position;
    private String match;
    private String activation;
    private String parameter;
    private String transform;
    private Integer remove;
    private String combine;

    public RuleModel() {
        id = UUID.randomUUID().toString();
    }

    public RuleModel(Rule rule) {
        id = rule.getId();
        position = rule.getPosition();
        match = rule.getMatch();
        activation = rule.getActivation();
        parameter = rule.getParameter();
        transform = rule.getTransform();
        remove = rule.getRemove();
        combine = rule.getCombine();
        if (position != null && transform != null) {
            transform = transform.replace("$2", "{PARAMETER}");
        }
    }

    public Rule toRule() {
        RuleBuilder ruleBuilder = new RuleBuilder().withId(id)
                .withPosition(position)
                .withMatch(match)
                .withActivation(activation)
                .withParameter(parameter)
                .withRemove(remove)
                .withCombine(combine);
        if (position != null && transform != null) {
            ruleBuilder.withTransform(transform.replace("{PARAMETER}", "$2"));
        } else {
            ruleBuilder.withTransform(transform);
        }
        return ruleBuilder.build();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getActivation() {
        return activation;
    }

    public void setActivation(String activation) {
        this.activation = activation;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public Integer getRemove() {
        return remove;
    }

    public void setRemove(Integer remove) {
        this.remove = remove;
    }

    public String getCombine() {
        return combine;
    }

    public void setCombine(String combine) {
        this.combine = combine;
    }
}

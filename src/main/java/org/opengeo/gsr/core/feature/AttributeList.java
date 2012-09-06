package org.opengeo.gsr.core.feature;

import java.util.List;

public class AttributeList {

    private List<Attribute> attributes;

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public AttributeList(List<Attribute> attributes) {
        super();
        this.attributes = attributes;
    }

    public void add(Attribute attribute) {
        attributes.add(attribute);
    }
}

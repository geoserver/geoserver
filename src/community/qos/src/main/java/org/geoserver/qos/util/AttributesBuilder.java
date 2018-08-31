/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class AttributesBuilder {

    private List<SimpleAttribute> attributes = new ArrayList<>();

    public AttributesBuilder() {}

    public AttributesBuilder add(String name, String value) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
            return this;
        }
        attributes.add(new SimpleAttribute(name, value));
        return this;
    }

    public Attributes getAttributes() {
        AttributesImpl atts = new AttributesImpl();
        attributes.forEach(a -> atts.addAttribute("", "", a.getName(), "string", a.getValue()));
        return atts;
    }

    public static class SimpleAttribute {
        private String name;
        private String value;

        public SimpleAttribute() {}

        public SimpleAttribute(String name, String value) {
            super();
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}

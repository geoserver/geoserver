/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class OwsAbstract implements Serializable {
    private static final long serialVersionUID = 1L;
    private String lang;
    private String value;

    public OwsAbstract() {}

    public OwsAbstract(String value) {
        super();
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

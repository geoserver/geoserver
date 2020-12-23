/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecker;

import java.io.Serializable;

/** @author ImranR */
public class URLEntry implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 7155850671941043630L;

    private String name;
    private String description;
    private String regexExpression;
    private boolean enable = true;

    public URLEntry() {}

    public URLEntry(String name, String description, String regexExpression) {
        super();
        this.name = name;
        this.description = description;
        this.regexExpression = regexExpression;
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @param name the name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the regexExpression */
    public String getRegexExpression() {
        return regexExpression;
    }

    /** @param regexExpression the regexExpression to set */
    public void setRegexExpression(String regexExpression) {
        this.regexExpression = regexExpression;
    }

    /** @return the enable */
    public boolean isEnable() {
        return enable;
    }

    /** @param enable the enable to set */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof URLEntry)) return false;
        URLEntry other = (URLEntry) obj;
        return this.name.equalsIgnoreCase(other.getName());
    }
}

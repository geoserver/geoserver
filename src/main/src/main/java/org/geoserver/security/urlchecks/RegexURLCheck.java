/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import java.io.Serializable;
import java.util.Objects;

/** User supplied regular expression check. */
public class RegexURLCheck extends AbstractURLCheck implements Serializable {

    private String regex;

    public RegexURLCheck() {}

    public RegexURLCheck(String name, String description, String regex) {
        super();
        this.name = name;
        this.description = description;
        this.regex = regex;
    }

    /** @return the regexExpression */
    public String getRegex() {
        return regex;
    }

    /** @param regex the regexExpression to set */
    public void setRegex(String regex) {
        this.regex = regex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegexURLCheck that = (RegexURLCheck) o;
        return enabled == that.enabled
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(regex, that.regex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, regex, enabled);
    }

    @Override
    public String toString() {
        return "URLCheckInfo{"
                + "name='"
                + name
                + '\''
                + ", description='"
                + description
                + '\''
                + ", regex='"
                + regex
                + '\''
                + ", enabled="
                + enabled
                + '}';
    }

    @Override
    public boolean confirm(String location) {
        return location.matches(regex);
    }

    @Override
    public String getConfiguration() {
        return regex;
    }
}

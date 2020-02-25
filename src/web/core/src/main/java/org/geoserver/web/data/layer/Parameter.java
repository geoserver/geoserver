/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.Serializable;
import java.util.logging.Level;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTableParameter;
import org.geotools.jdbc.VirtualTableParameter.Validator;

class Parameter implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -5028680760307467030L;

    String name;

    String defaultValue;

    String regexp;

    public Parameter(String name, String defaultValue, String regexp) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.regexp = regexp;
    }

    public Parameter(VirtualTableParameter param) {
        this.name = param.getName();
        this.defaultValue = param.getDefaultValue();
        Validator validator = param.getValidator();
        if (validator != null) {
            if (validator instanceof RegexpValidator) {
                this.regexp = ((RegexpValidator) validator).getPattern().pattern();
            } else {
                SQLViewParamProvider.LOGGER.log(
                        Level.WARNING, "Skipping unknown validator type " + validator.getClass());
            }
        }
    }

    public VirtualTableParameter toVirtualTableParameter() {
        VirtualTableParameter result = new VirtualTableParameter(name, defaultValue);
        if (regexp != null) {
            result.setValidator(new RegexpValidator(regexp));
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((regexp == null) ? 0 : regexp.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Parameter other = (Parameter) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null) return false;
        } else if (!defaultValue.equals(other.defaultValue)) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (regexp == null) {
            if (other.regexp != null) return false;
        } else if (!regexp.equals(other.regexp)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Parameter [defaultValue="
                + defaultValue
                + ", name="
                + name
                + ", regexp="
                + regexp
                + "]";
    }
}

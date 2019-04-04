/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.Serializable;

class SQLViewAttribute implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -926721043289684925L;

    String name;

    Class<?> type;

    Integer srid;

    boolean pk;

    public SQLViewAttribute(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public Integer getSrid() {
        return srid;
    }

    public void setSrid(Integer srid) {
        this.srid = srid;
    }

    public boolean isPk() {
        return pk;
    }

    public void setPk(boolean pk) {
        this.pk = pk;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (pk ? 1231 : 1237);
        result = prime * result + ((srid == null) ? 0 : srid.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SQLViewAttribute other = (SQLViewAttribute) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (pk != other.pk) return false;
        if (srid == null) {
            if (other.srid != null) return false;
        } else if (!srid.equals(other.srid)) return false;
        if (type == null) {
            if (other.type != null) return false;
        } else if (!type.equals(other.type)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "SQLViewAttribute [name="
                + name
                + ", pk="
                + pk
                + ", srid="
                + srid
                + ", type="
                + type
                + "]";
    }
}

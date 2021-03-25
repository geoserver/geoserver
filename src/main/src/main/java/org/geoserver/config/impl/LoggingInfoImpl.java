/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import org.geoserver.config.LoggingInfo;

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
public class LoggingInfoImpl implements LoggingInfo {

    String id;

    String level;

    String location;

    boolean stdOutLogging;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getLevel() {
        return level;
    }

    @Override
    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public boolean isStdOutLogging() {
        return stdOutLogging;
    }

    @Override
    public void setStdOutLogging(boolean stdOutLogging) {
        this.stdOutLogging = stdOutLogging;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + (stdOutLogging ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof LoggingInfo)) {
            return false;
        }
        LoggingInfo other = (LoggingInfo) obj;
        if (level == null) {
            if (other.getLevel() != null) return false;
        } else if (!level.equals(other.getLevel())) return false;
        if (location == null) {
            if (other.getLocation() != null) return false;
        } else if (!location.equals(other.getLocation())) return false;
        if (stdOutLogging != other.isStdOutLogging()) return false;
        return true;
    }
}

/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.CoverageDimensionInfo;
import org.geotools.util.NumberRange;

public class CoverageDimensionImpl implements CoverageDimensionInfo {

    /**
	 * 
	 */
	private static final long serialVersionUID = 2993765933856195894L;

	String id;

    String name;

    String description;

    NumberRange range;

    List<Double> nullValues = new ArrayList();
    
    public CoverageDimensionImpl() {
    }

    public CoverageDimensionImpl(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NumberRange getRange() {
        return range;
    }

    public void setRange(NumberRange range) {
        this.range = range;
    }

    public List<Double> getNullValues() {
        return nullValues;
    }

    public void setNullValues(List<Double> nullValues) {
        this.nullValues = nullValues;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nullValues == null) ? 0 : nullValues.hashCode());
        result = prime * result + ((range == null) ? 0 : range.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoverageDimensionImpl other = (CoverageDimensionImpl) obj;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nullValues == null) {
            if (other.nullValues != null)
                return false;
        } else if (!nullValues.equals(other.nullValues))
            return false;
        if (range == null) {
            if (other.range != null)
                return false;
        } else if (!range.equals(other.range))
            return false;
        return true;
    }
    
    
}

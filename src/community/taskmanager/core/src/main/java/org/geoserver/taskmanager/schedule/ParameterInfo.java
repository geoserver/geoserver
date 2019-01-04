/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a task type's parameter.
 *
 * @author Niels Charlier
 */
public class ParameterInfo {

    /** The parameter name */
    private String name;

    /** The parameter type. */
    private ParameterType type;

    /** Whether the parameter is required or not. */
    private boolean required;

    /** Dependent parameters */
    private List<ParameterInfo> dependents = new ArrayList<ParameterInfo>();

    /** Depends on parameters */
    private List<ParameterInfo> dependsOn = new ArrayList<ParameterInfo>();

    public ParameterInfo(String name, ParameterType type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public ParameterType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public ParameterInfo dependsOn(ParameterInfo... infos) {
        for (ParameterInfo info : infos) {
            dependsOn.add(info);
            info.dependents.add(this);
        }
        return this;
    }

    public List<ParameterInfo> getDependsOn() {
        return dependsOn;
    }

    public List<ParameterInfo> getDependents() {
        return dependents;
    }
}

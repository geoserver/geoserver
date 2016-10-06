/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import org.apache.wicket.model.IModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

public class ScriptDetachableModel implements IModel<Script> {

    private static final long serialVersionUID = 3279753609508440766L;

    transient Script script;

    Resource file;

    public ScriptDetachableModel(Script script) {
        setObject(script);
    }

    public Script getObject() {
        if (script == null) {
            script = file != null ? new Script(file) : null;
        }
        return script;
    }

    public void setObject(Script object) {
        script = object;
        file = script.getResource();
        if (!Resources.exists(file)) {
            file = null;
        }
    }

    public void detach() {
        this.script = null;
    }
}

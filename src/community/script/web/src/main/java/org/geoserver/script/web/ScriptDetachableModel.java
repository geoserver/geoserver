/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import org.apache.wicket.model.IModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

public class ScriptDetachableModel implements IModel {

    transient Script script;

    Resource file;

    public ScriptDetachableModel(Script script) {
        setObject(script);
    }

    public Object getObject() {
        if (script == null) {
            script = file != null ? new Script(file) : null;
        }
        return script;
    }

    public void setObject(Object object) {
        script = (Script) object;
        file = script.getResource();
        if (!Resources.exists(file)) {
            file = null;
        }
    }

    public void detach() {
        this.script = null;
    }
}
